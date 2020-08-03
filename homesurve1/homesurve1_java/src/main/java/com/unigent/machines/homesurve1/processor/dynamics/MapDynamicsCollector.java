package com.unigent.machines.homesurve1.processor.dynamics;

import com.unigent.agentbase.library.core.state.TensorPayload;
import com.unigent.agentbase.library.core.state.sensor.XYZOrientationReading;
import com.unigent.agentbase.sdk.commons.Config;
import com.unigent.agentbase.sdk.controller.ConsoleHandle;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.state.StateUpdate;
import com.unigent.machines.homesurve1.state.ObjectScenePayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = {
                @ConsumedDataFlow(dataType = ObjectScenePayload.class, localName = "scene"),
                @ConsumedDataFlow(dataType = TensorPayload.class, localName = "linear_motion"),
                @ConsumedDataFlow(dataType = XYZOrientationReading.class, localName = "simulated_orientation")
        }
)
public class MapDynamicsCollector extends ProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    static List<MapStateTransition> dynamicBuffer = Collections.synchronizedList(new ArrayList<>());

    private static final long TIME_RANGE_MILLIS = 50;
    private static final URI TOPIC_XYZ = URI.create("simulated_orientation");
    private static final URI TOPIC_LINEAR_MOTION = URI.create("linear_motion");

    private MapState previousState = null;

    public MapDynamicsCollector(String name) {
        super(name);
    }

    @Override
    public void configure(Map<String, URI> outputBinding, Config config, ConsoleHandle console, NodeServices nodeServices) {
        super.configure(outputBinding, config, console, nodeServices);
        nodeServices.console.registerCommandHandler(new MapDynamicsConsoleHandler());
    }

    @Override
    public void onStateUpdate(@Nonnull StateUpdate stateUpdate, @Nonnull URI sourceTopic, @Nullable String localBinding) {

        if(!"scene".equals(localBinding)) {
            return;
        }

        if(dynamicBuffer.size() > 1000) {
            log.error("Dynamics buffer overflow. The experiment is over!");
            return;
        }

        ObjectScenePayload scene = (ObjectScenePayload) stateUpdate.getPayload();
        long sourceImageTimestamp = scene.getSourceImageTimestamp();

        XYZOrientationReading relatedOrientationReading = nodeServices.stateBus.ensureTopic(TOPIC_XYZ)
                .getData(sourceImageTimestamp - TIME_RANGE_MILLIS, sourceImageTimestamp + TIME_RANGE_MILLIS)
                .stream()
                .map(update->(XYZOrientationReading) update.getPayload())
                .findFirst()
                .orElseThrow(()->new RuntimeException("No related XYZ orientation reading"));

        TensorPayload relatedLinearMotionReading = nodeServices.stateBus.ensureTopic(TOPIC_LINEAR_MOTION)
                .getData(sourceImageTimestamp - TIME_RANGE_MILLIS, sourceImageTimestamp + TIME_RANGE_MILLIS)
                .stream()
                .map(update->(TensorPayload) update.getPayload())
                .findFirst()
                .orElseThrow(()->new RuntimeException("No related Linear Motion reading"));

        MapState nextState = new MapState(
                scene.getObjects().stream()
                        .map(detectedObject->new MapState.MapObject(detectedObject.getClassId(), detectedObject.getAzimuthDegrees(), detectedObject.getDistanceMeters()))
                        .sorted()
                        .collect(Collectors.toList())
        );

        log.info("dynamics# Got state {}", nextState);

        if(previousState != null) {
            MapAction action = new MapAction(relatedOrientationReading.getAngularVelocityGamma(), relatedLinearMotionReading.toVector()[0]);
            dynamicBuffer.add(new MapStateTransition(previousState, action, nextState));
            log.info("dynamics# Buffer size: {}", dynamicBuffer.size());
        }

        previousState = nextState;
    }
}

package com.unigent.machines.homesurve1.processor.map;

import com.unigent.agentbase.library.core.state.TensorPayload;
import com.unigent.agentbase.library.core.state.sensor.XYZOrientationReading;
import com.unigent.agentbase.sdk.commons.Config;
import com.unigent.agentbase.sdk.commons.util.Dates;
import com.unigent.agentbase.sdk.commons.util.geometry.Geometry;
import com.unigent.agentbase.sdk.controller.ConsoleHandle;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.state.StateUpdate;
import com.unigent.machines.homesurve1.InitializerProcessor;
import com.unigent.machines.homesurve1.processor.BodyMotionAwareProcessorBase;
import com.unigent.machines.homesurve1.state.RecognizedObjectScenePayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;

/**
 *
 * The topics "linear_motion" and "sensor/orientation/bno055" are queried manually
 *
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = {
                @ConsumedDataFlow(
                        dataType = RecognizedObjectScenePayload.class,
                        localName = "recognized_scene"
                ),
                @ConsumedDataFlow(
                        dataType = TensorPayload.class,
                        localName = "linear_motion",
                        description = "[path along X,velocity along X, delta time]",
                        receiveUpdates = false
                ),
                @ConsumedDataFlow(
                        dataType = XYZOrientationReading.class,
                        localName = "sensor/orientation/bno055",
                        receiveUpdates = false
                )
        }
)
public class MapDynamicsCollector extends BodyMotionAwareProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    public static final int DETECTABLE_AZIMUTH_CHANGE_DEGREES = 2;
    public static final int DETECTABLE_MOTION_CHANGE_CM = 5;


    private MapState previousState = null;
    private Double previousAzimuth;

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

        checkArgument("recognized_scene".equals(localBinding), localBinding);

        RecognizedObjectScenePayload scene = (RecognizedObjectScenePayload) stateUpdate.getPayload();
        long sourceImageTimestamp = scene.getSourceImageTimestamp();

        log.info("dynamics# Got scene with {} objects at {}", scene.getObjects().size(), Dates.toLocalTime(sourceImageTimestamp));

        double newAzimuth = getAzimuth(sourceImageTimestamp);

        if(previousAzimuth == null) {
            previousAzimuth = newAzimuth;
            return;
        }

        MapState nextState = sceneToMapState(scene);
        log.info("dynamics# Got state {}", nextState);

        if(!nextState.getObjects().isEmpty() && previousState != null && !previousState.getObjects().isEmpty()) {
            int deltaX = getLinearMotionXcm(sourceImageTimestamp);
            if(abs(deltaX) < DETECTABLE_MOTION_CHANGE_CM) {
                deltaX = 0;
            }

            int deltaAzimuth = azimuthToIntDegrees(Geometry.diffAzimuthDegrees(newAzimuth, previousAzimuth));
            if(abs(deltaAzimuth) < DETECTABLE_AZIMUTH_CHANGE_DEGREES) {
                deltaAzimuth = 0;
            }

            if(deltaX != 0 || deltaAzimuth != 0) {
                // Store the transition!
                MapAction action = new MapAction(deltaAzimuth, deltaX);
                MapStateTransition stateTransition = new MapStateTransition(previousState, action, nextState);
                InitializerProcessor.getInstance().getMapDynamicsMemory().store(stateTransition);
            }
        }

        previousState = nextState;
        previousAzimuth = newAzimuth;
    }


    public static MapState sceneToMapState(RecognizedObjectScenePayload scene) {
        return new MapState(
                scene.getObjects().stream()
                        .map(detectedObject->new MapState.MapObject(
                                detectedObject.getObjectId(),
                                azimuthToIntDegrees(detectedObject.getAzimuthDegrees()),
                                distanceMetersToCm(detectedObject.getDistanceMM())
                        ))
                        .sorted()
                        .collect(Collectors.toList())
        );
    }

    public static int azimuthToIntDegrees(double azimuth) {
        return (int) Math.round(azimuth);
    }

    public static int distanceMetersToCm(double distanceMeters) {
        return (int) Math.round(distanceMeters * 100);
    }
}

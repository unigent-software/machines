package com.unigent.machines.homesurve1.processor.dynamics;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.unigent.agentbase.library.core.state.TensorPayload;
import com.unigent.agentbase.library.core.state.sensor.XYZOrientationReading;
import com.unigent.agentbase.sdk.commons.Config;
import com.unigent.agentbase.sdk.commons.util.Dates;
import com.unigent.agentbase.sdk.commons.util.JSON;
import com.unigent.agentbase.sdk.controller.ConsoleHandle;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.state.StateUpdate;
import com.unigent.machines.homesurve1.state.ObjectScenePayload;
import com.unigent.machines.homesurve1.state.RecognizedObjectScenePayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
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
public class MapDynamicsCollector extends ProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final double DETECTABLE_AZIMUTH_CHANGE_DEGREES = 2.0;
    private static final double DETECTABLE_MOTION_CHANGE_METERS = 0.05;

    private static final long TIME_RANGE_MILLIS = 80;
    private static final URI TOPIC_XYZ = URI.create("sensor/orientation/bno055");
    private static final URI TOPIC_LINEAR_MOTION = URI.create("linear_motion");

    private MapState previousState = null;

    private Double previousAzimuth;

    private Nitrite mapDynamicsDb;
    private ObjectRepository<MapStateTransition> mapStateTransitionRepo;

    static MapDynamicsCollector instance;

    public MapDynamicsCollector(String name) {
        super(name);
        instance = this;
    }

    @Override
    public void configure(Map<String, URI> outputBinding, Config config, ConsoleHandle console, NodeServices nodeServices) {
        super.configure(outputBinding, config, console, nodeServices);
        nodeServices.console.registerCommandHandler(new MapDynamicsConsoleHandler());
    }

    @Override
    public void initiate() {
        super.initiate();
        this.mapDynamicsDb = nodeServices.nitriteManager.getNitrite("map_dynamics");
        this.mapStateTransitionRepo = this.mapDynamicsDb.getRepository(MapStateTransition.class);
        nodeServices.stateBus.ensureTopic(TOPIC_XYZ);
    }

    @Override
    public void shutdown() {
        this.mapDynamicsDb.close();
        super.shutdown();
    }

    @Override
    public void onStateUpdate(@Nonnull StateUpdate stateUpdate, @Nonnull URI sourceTopic, @Nullable String localBinding) {

        checkArgument("recognized_scene".equals(localBinding), localBinding);

        RecognizedObjectScenePayload scene = (RecognizedObjectScenePayload) stateUpdate.getPayload();
        long sourceImageTimestamp = scene.getSourceImageTimestamp();

        log.info("dynamics# Got scene with {} objects at {}", scene.getObjects().size(), Dates.toLocalTime(sourceImageTimestamp));

        long fromMillis = sourceImageTimestamp - TIME_RANGE_MILLIS;
        long toMillis = sourceImageTimestamp + TIME_RANGE_MILLIS;

        XYZOrientationReading relatedOrientationReading = nodeServices.stateBus.ensureTopic(TOPIC_XYZ)
                .getData(fromMillis, toMillis)
                .stream()
                .map(update->(XYZOrientationReading) update.getPayload())
                .findFirst()
                .orElseThrow(()-> new RuntimeException("No related XYZ orientation reading between " + Dates.toLocalTime(fromMillis) + " and " + Dates.toLocalTime(toMillis)));

        if(previousAzimuth == null) {
            previousAzimuth = relatedOrientationReading.getAzimuth();
            return;
        }

        TensorPayload relatedLinearMotionReading = nodeServices.stateBus.ensureTopic(TOPIC_LINEAR_MOTION)
                .getData(sourceImageTimestamp - TIME_RANGE_MILLIS, sourceImageTimestamp + TIME_RANGE_MILLIS)
                .stream()
                .map(update->(TensorPayload) update.getPayload())
                .findFirst()
                .orElse(null);

        MapState nextState = new MapState(
                scene.getObjects().stream()
                        .map(detectedObject->new MapState.MapObject(detectedObject.getObjectId(), detectedObject.getAzimuthDegrees(), detectedObject.getDistanceMeters()))
                        .sorted()
                        .collect(Collectors.toList())
        );

        log.info("dynamics# Got state {}", nextState);

        if(previousState != null) {
            double deltaX = relatedLinearMotionReading == null ? 0.0 : relatedLinearMotionReading.toVector()[0];
            if(abs(deltaX) < DETECTABLE_MOTION_CHANGE_METERS) {
                deltaX = 0.0;
            }

            double deltaAzimuth = relatedOrientationReading.getAzimuth() - previousAzimuth;
            if(abs(deltaAzimuth) < DETECTABLE_AZIMUTH_CHANGE_DEGREES) {
                deltaAzimuth = 0.0;
            }

            MapAction action = new MapAction(deltaAzimuth, deltaX);
            MapStateTransition stateTransition = new MapStateTransition(previousState, action, nextState);

            this.mapStateTransitionRepo.insert(stateTransition);
            this.mapDynamicsDb.commit();
        }

        previousState = nextState;
    }

    long getBufferSize() {
        return this.mapStateTransitionRepo.size();
    }

    void clearBuffer() {
        this.mapStateTransitionRepo.drop();
        this.mapStateTransitionRepo = this.mapDynamicsDb.getRepository(MapStateTransition.class);
    }

    int dump(PrintWriter output) {
        int cnt = 0;
        try {
            ObjectWriter objectWriter = JSON.mapper.writerWithDefaultPrettyPrinter();
            for(MapStateTransition stateTransition : this.mapStateTransitionRepo.find()) {
                output.println(objectWriter.writeValueAsString(stateTransition));
                cnt ++;
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to dump: " + e.getMessage(), e);
        }
        log.info("Dumped {} items", cnt);
        return cnt;
    }
}

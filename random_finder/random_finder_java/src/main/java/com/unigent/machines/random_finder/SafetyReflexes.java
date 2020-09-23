package com.unigent.machines.random_finder;

import com.unigent.agentbase.library.core.state.sensor.BinaryValueReading;
import com.unigent.agentbase.library.core.state.sensor.DistanceReading;
import com.unigent.agentbase.sdk.action.TaskRequest;
import com.unigent.agentbase.sdk.controller.ActuatorCommand;
import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.rl.DiscreteActionImpl;
import com.unigent.agentbase.sdk.state.StateUpdate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Random Finder Demo Robot
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/

@AgentBaseProcessor(
        description = "Provides basic safety reflexes when proximity sensors trigger",
        consumedData = {
                @ConsumedDataFlow(localName = "prox_front_left", dataType = BinaryValueReading.class),
                @ConsumedDataFlow(localName = "prox_front_right", dataType = BinaryValueReading.class),
                @ConsumedDataFlow(localName = "prox_back", dataType = BinaryValueReading.class),
                @ConsumedDataFlow(localName = "sonar_front", dataType = DistanceReading.class),
                @ConsumedDataFlow(localName = "sonar_back", dataType = DistanceReading.class),
                @ConsumedDataFlow(localName = "sonar_left", dataType = DistanceReading.class),
                @ConsumedDataFlow(localName = "sonar_right", dataType = DistanceReading.class)
        }
)
public class SafetyReflexes extends ProcessorBase implements Constants {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONTROLLER_URI = "mega_2560";
    private static final List<ActuatorCommand> STOP_MOTORS = Arrays.asList(
            new ActuatorCommand("motor_left", new double [] {0.0}),
            new ActuatorCommand("motor_right", new double [] {0.0})
    );

    public SafetyReflexes(String name) {
        super(name);
    }

    @Override
    public void onStateUpdate(@Nonnull StateUpdate stateUpdate, @Nonnull URI sourceTopic, @Nullable String localBinding) {

        Objects.requireNonNull(localBinding);

        if(localBinding.startsWith("prox_")) {
            boolean touch = ((BinaryValueReading)stateUpdate.getPayload()).isValue();
            if(touch) {
                stopMotors();
                nodeServices.console.inform("Prox touch at " + localBinding);
            }
        }
        else if(localBinding.startsWith("sonar_")) {
            double distance = ((DistanceReading) stateUpdate.getPayload()).getDistanceMeters();
            if(distance < 0.15) {
                stopMotors();
                nodeServices.console.inform("Sonar " + localBinding + " too close: " + distance + "m");
            }
        }
        else {
            log.error("Unrecognized payload at " + localBinding);
        }
    }

    private void stopMotors() {
        log.info("Stopping motors!");

        // Stop right away!!
        nodeServices.localHardwareControllersByUri.get(CONTROLLER_URI).actuateUrgently(STOP_MOTORS);

        // Move back (is there is nothing behind)
        DistanceReading backSonarReading = nodeServices.stateBus.ensureTopic("sensor/distance/sonar_back").getMostRecentDataPayload();
        BinaryValueReading backProxReading = nodeServices.stateBus.ensureTopic("sensor/value/binary/prox_back").getMostRecentDataPayload();

        boolean spaceInBack = backSonarReading == null || !backSonarReading.isValid() || backSonarReading.getDistanceMeters() > 0.5;
        boolean touchInBack = backProxReading != null && backProxReading.isValue();
        if(spaceInBack && !touchInBack) {
            log.info("Stepping backward");
            nodeServices.taskManager.execute(AS_MOTOR, new DiscreteActionImpl(MotorActions.ACTION_STEP_BACKWARD), TaskRequest.newTask(TaskRequest.MAX_URGENCY, 3000));
        }
    }

    @Override
    public void initiate() {
        Objects.requireNonNull(nodeServices.localHardwareControllersByUri.get(CONTROLLER_URI), "Motor controller " + CONTROLLER_URI + " not found");
    }
}

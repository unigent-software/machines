package com.unigent.machines.homesurve1.processor;

import com.unigent.agentbase.library.core.state.TensorPayload;
import com.unigent.agentbase.sdk.commons.util.Mean;
import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.processing.metadata.ProducedDataFlow;
import com.unigent.agentbase.sdk.state.StateUpdate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.net.URI;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
    consumedData = @ConsumedDataFlow(dataType = TensorPayload.class, localName = "depth_image", description = "640x480 depth image"),
    producedData = @ProducedDataFlow(dataType = TensorPayload.class, localName = "linear_motion", description = "[[path along X][velocity along X]]"),
    description = "Produces linear motion (travel and velocity) value (+/-) in meters"
)
public class LinearMotionDetector extends ProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    private static final double DEPTH_SENSITIVITY_METERS = 0.05;

    private Long previousTimestamp = null;
    private Double previousDepth = null;

    public LinearMotionDetector(String name) {
        super(name);
    }

    @Override
    public void initiate() {
        super.initiate();
        previousTimestamp = null;
    }

    @Override
    public void onStateUpdate(@Nonnull StateUpdate stateUpdate, @Nonnull URI sourceTopic, @Nullable String localBinding) {
        checkArgument(sourceTopic.toString().equals("depth_image"), "Unexpected local binding");
        long timestamp = System.currentTimeMillis();
        TensorPayload depthTensor = (TensorPayload) stateUpdate.getPayload();
        double depth = getMeanDepth(depthTensor);
        log.info("linear_velocity# depth: {} m", depth);

        if(previousTimestamp == null) {
            // Bootstrap
            previousTimestamp = timestamp;
            previousDepth = depth;
            return;
        }

        double deltaDepth = previousDepth - depth; // If depth reduces -> forward movement

        if(abs(deltaDepth) < DEPTH_SENSITIVITY_METERS) {
            return;
        }

        double deltaTime = (timestamp - previousTimestamp) / 1000.0; // Seconds
        double velocityMetersSec = deltaDepth / deltaTime;

        produceStateUpdate(
                new TensorPayload(
                        new double[][]{
                                new double[]{deltaDepth},
                                new double[]{velocityMetersSec},
                        }
                ),
                "linear_motion"
        );

        previousTimestamp = timestamp;
        previousDepth = depth;
    }

    private static double getMeanDepth(TensorPayload depthTensor) {
        double [][] depthData = depthTensor.toMatrix();
        Mean depthMean = new Mean();
        // Looking at the center box 320x240
        for(int r=120; r<360; r+= 2) {
            for(int c=160; c<480; c+= 2) {
                depthMean.increment(depthData[r][c]);
            }
        }
        return depthMean.getValue();
    }
}

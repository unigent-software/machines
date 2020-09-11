package com.unigent.machines.homesurve1.processor;

import com.unigent.agentbase.library.core.state.TensorPayload;
import com.unigent.agentbase.library.core.state.sensor.XYZOrientationReading;
import com.unigent.agentbase.sdk.commons.util.Dates;
import com.unigent.agentbase.sdk.processing.ProcessorBase;

import java.net.URI;

import static com.unigent.machines.homesurve1.processor.map.MapDynamicsCollector.distanceMetersToCm;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class BodyMotionAwareProcessorBase extends ProcessorBase {

    private static final long TIME_RANGE_MILLIS = 80;
    private static final URI TOPIC_XYZ = URI.create("sensor/orientation/bno055");
    private static final URI TOPIC_LINEAR_MOTION = URI.create("linear_motion");

    public BodyMotionAwareProcessorBase(String name) {
        super(name);
    }

    protected Integer getLinearMotionXcm(long timestamp) {
        TensorPayload relatedLinearMotionReading = nodeServices.stateBus.ensureTopic(TOPIC_LINEAR_MOTION)
                .getData(timestamp - TIME_RANGE_MILLIS, timestamp + TIME_RANGE_MILLIS)
                .stream()
                .map(update->(TensorPayload) update.getPayload())
                .findFirst()
                .orElse(null);
        return distanceMetersToCm(relatedLinearMotionReading == null ? 0.0 : relatedLinearMotionReading.toVector()[0]);
    }

    protected double getAzimuth(long timeStampMillis) {
        long fromMillis = timeStampMillis - TIME_RANGE_MILLIS;
        long toMillis = timeStampMillis + TIME_RANGE_MILLIS;
        return nodeServices.stateBus.ensureTopic(TOPIC_XYZ)
                .getData(fromMillis, toMillis)
                .stream()
                .map(update->(XYZOrientationReading) update.getPayload())
                .findFirst()
                .orElseThrow(()-> new RuntimeException("No related XYZ orientation reading between " + Dates.toLocalTime(fromMillis) + " and " + Dates.toLocalTime(toMillis)))
                .getX();
    }

    @Override
    public void initiate() {
        super.initiate();
        nodeServices.stateBus.ensureTopic(TOPIC_XYZ);
    }

}

package com.unigent.machines.homesurve1.state;

import com.unigent.agentbase.sdk.serialization.JsonSerializerBase;
import com.unigent.agentbase.sdk.state.StatePayload;
import com.unigent.agentbase.sdk.state.metadata.AgentBaseState;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseState(serializerType = RobotLocationPayload.RobotLocationSerializer.class)
public class RobotLocationPayload implements StatePayload {

    private String mapFragmentId;
    private double x;
    private double y;
    private double confidence;

    public String getMapFragmentId() {
        return mapFragmentId;
    }

    public void setMapFragmentId(String mapFragmentId) {
        this.mapFragmentId = mapFragmentId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public static class RobotLocationSerializer extends JsonSerializerBase<RobotLocationPayload> {
        public Class<RobotLocationPayload> getTargetType() {
            return RobotLocationPayload.class;
        }
    }
}

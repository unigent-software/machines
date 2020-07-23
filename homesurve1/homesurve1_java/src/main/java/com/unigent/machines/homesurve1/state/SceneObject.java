package com.unigent.machines.homesurve1.state;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class SceneObject {

    private String classId;
    private String label;
    private double distanceMeters;
    private double azimuthDegrees;

    public SceneObject() {

    }

    public SceneObject(String classId, String label, double distanceMeters, double azimuthDegrees) {
        this.classId = classId;
        this.label = label;
        this.distanceMeters = distanceMeters;
        this.azimuthDegrees = azimuthDegrees;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public double getAzimuthDegrees() {
        return azimuthDegrees;
    }

    public void setAzimuthDegrees(double azimuthDegrees) {
        this.azimuthDegrees = azimuthDegrees;
    }

    @Override
    public String toString() {
        return "SceneObject{" +
                "classId='" + classId + '\'' +
                ", label='" + label + '\'' +
                ", distanceMeters=" + distanceMeters +
                ", azimuthDegrees=" + azimuthDegrees +
                '}';
    }
}

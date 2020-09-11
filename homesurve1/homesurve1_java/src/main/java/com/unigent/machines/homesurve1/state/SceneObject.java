package com.unigent.machines.homesurve1.state;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class SceneObject {

    private int classId;
    private String label;
    private int distanceMM;
    private double azimuthDegrees; // 0-360
    private double widthDegrees;

    public SceneObject() {
    }

    public SceneObject(int classId, String label, int distanceMM, double azimuthDegrees, double widthDegrees) {
        this.classId = classId;
        this.label = label;
        this.distanceMM = distanceMM;
        this.azimuthDegrees = azimuthDegrees;
        this.widthDegrees = widthDegrees;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getDistanceMM() {
        return distanceMM;
    }

    public void setDistanceMM(int distanceMM) {
        this.distanceMM = distanceMM;
    }

    public double getAzimuthDegrees() {
        return azimuthDegrees;
    }

    public void setAzimuthDegrees(double azimuthDegrees) {
        this.azimuthDegrees = azimuthDegrees;
    }

    public double getWidthDegrees() {
        return widthDegrees;
    }

    public void setWidthDegrees(double widthDegrees) {
        this.widthDegrees = widthDegrees;
    }

    @Override
    public String toString() {
        return "SceneObject{" +
                "classId='" + classId + '\'' +
                ", label='" + label + '\'' +
                ", distanceMM=" + distanceMM +
                ", azimuthDegrees=" + azimuthDegrees +
                ", widthDegrees=" + widthDegrees +
                '}';
    }
}

package com.unigent.machines.random_finder;

/**
 * Random Finder Demo Robot
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/

public class SceneObject {

    private int classId;
    private String label;
    private double azimuthDegrees; // 0-360
    private double widthDegrees;

    public SceneObject() {
    }

    public SceneObject(int classId, String label, double azimuthDegrees, double widthDegrees) {
        this.classId = classId;
        this.label = label;
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
                ", azimuthDegrees=" + azimuthDegrees +
                ", widthDegrees=" + widthDegrees +
                '}';
    }
}

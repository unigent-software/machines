package com.unigent.machines.homesurve1.processor.dynamics;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapAction {

    private double deltaGamma;
    private double deltaX;

    public MapAction() {
    }

    public MapAction(double deltaGamma, double deltaX) {
        this.deltaGamma = deltaGamma;
        this.deltaX = deltaX;
    }

    public double getDeltaGamma() {
        return deltaGamma;
    }

    public double getDeltaX() {
        return deltaX;
    }

    @Override
    public String toString() {
        return "MapAction{" +
                "deltaGamma=" + deltaGamma +
                ", deltaX=" + deltaX +
                '}';
    }
}

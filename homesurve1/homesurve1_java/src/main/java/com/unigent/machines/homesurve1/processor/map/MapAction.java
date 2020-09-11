package com.unigent.machines.homesurve1.processor.map;

import java.util.Objects;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapAction {

    private int deltaGamma;
    private int deltaX;

    public MapAction() {
    }

    public MapAction(int deltaGamma, int deltaX) {
        this.deltaGamma = deltaGamma;
        this.deltaX = deltaX;
    }

    public int getDeltaGamma() {
        return deltaGamma;
    }

    public int getDeltaX() {
        return deltaX;
    }

    @Override
    public String toString() {
        return "{" + "dG=" + deltaGamma + ", dX=" + deltaX + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapAction mapAction = (MapAction) o;
        return mapAction.deltaGamma == deltaGamma && mapAction.deltaX == deltaX;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deltaGamma, deltaX);
    }
}

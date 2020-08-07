package com.unigent.machines.homesurve1.processor.dynamics;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapStateTransition {

    private MapState fromState;
    private MapAction action;
    private MapState toState;

    public MapStateTransition(MapState fromState, MapAction action, MapState toState) {
        this.fromState = fromState;
        this.action = action;
        this.toState = toState;
    }

    public void setFromState(MapState fromState) {
        this.fromState = fromState;
    }

    public void setAction(MapAction action) {
        this.action = action;
    }

    public void setToState(MapState toState) {
        this.toState = toState;
    }

    public MapState getFromState() {
        return fromState;
    }

    public MapAction getAction() {
        return action;
    }

    public MapState getToState() {
        return toState;
    }

    @Override
    public String toString() {
        return "MapStateTransition{" +
                "fromState=" + fromState +
                ", action=" + action +
                ", toState=" + toState +
                '}';
    }
}

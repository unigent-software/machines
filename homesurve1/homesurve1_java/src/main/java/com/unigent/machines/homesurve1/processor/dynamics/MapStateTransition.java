package com.unigent.machines.homesurve1.processor.dynamics;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapStateTransition {

    private final MapState fromState;
    private final MapAction action;
    private final MapState toState;

    public MapStateTransition(MapState fromState, MapAction action, MapState toState) {
        this.fromState = fromState;
        this.action = action;
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

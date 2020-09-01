package com.unigent.machines.homesurve1.processor.dynamics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unigent.agentbase.sdk.commons.util.Ids;
import org.dizitart.no2.objects.Id;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/

public class MapStateTransition {

    @Id
    private String id;
    private MapState fromState;
    private MapAction action;
    private MapState toState;

    public MapStateTransition() {
    }

    public MapStateTransition(MapState fromState, MapAction action, MapState toState) {
        this.id = Ids.randomId();
        this.fromState = fromState;
        this.action = action;
        this.toState = toState;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @JsonIgnore
    public String toSimpleString() {
        String fromS = "[" + fromState.getObjects().stream().map(MapState.MapObject::getObjectId).collect(Collectors.joining(",")) + "]";
        String toS = "[" + toState.getObjects().stream().map(MapState.MapObject::getObjectId).collect(Collectors.joining(",")) + "]";
        String actionS = "(" + BigDecimal.valueOf(action.getDeltaGamma()).toPlainString() + ", " + BigDecimal.valueOf(action.getDeltaX()).toPlainString() + ")";
        return actionS + " @ " + fromS + " -> " + toS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapStateTransition that = (MapStateTransition) o;
        if(id == null || that.id == null) {
            throw new IllegalStateException("Unable to check equality without an id: " + id + ", " + that.id);
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Objects.requireNonNull(id, "ID must not be null"));
    }
}

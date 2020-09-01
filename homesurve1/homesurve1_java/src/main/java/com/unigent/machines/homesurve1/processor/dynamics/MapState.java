package com.unigent.machines.homesurve1.processor.dynamics;

import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.abs;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapState {

    private List<MapObject> objects;

    public MapState() {
    }

    public MapState(MapObject ... objects) {
        this(Arrays.asList(objects));
    }

    public MapState(List<MapObject> objects) {
        this.objects = objects;
    }

    public List<MapObject> getObjects() {
        return objects;
    }

    public void setObjects(List<MapObject> objects) {
        this.objects = objects;
    }

    public boolean matches(MapState other) {
        for(MapObject thisObj : this.objects) {
            if(other.objects.stream().anyMatch(otherObj->otherObj.isEquivalent(thisObj))) {
                return true;
            }
        }
        return false;
    }

    public float match(MapState other) {
        int total = this.objects.size() + other.objects.size();
        int matching = 0;
        for(MapObject thisObj : this.objects) {
            if(other.objects.stream().anyMatch(otherObj->otherObj.isEquivalent(thisObj))) {
                matching += 2;
            }
        }
        return matching / (float) total;
    }

    @Override
    public String toString() {
        return "MapState{" + objects + '}';
    }

    @Indices(value = {
            @Index(value = "objectId", type = IndexType.NonUnique),
    })
    public static class MapObject implements Comparable<MapObject> {

        private String objectId;
        private int azimuth;
        private int distance;

        public MapObject() {
        }

        public MapObject(String objectId, int azimuth, int distance) {
            this.objectId = objectId;
            this.azimuth = azimuth;
            this.distance = distance;
        }

        public void setObjectId(String objectId) {
            this.objectId = objectId;
        }

        public void setAzimuth(int azimuth) {
            this.azimuth = azimuth;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }

        public String getObjectId() {
            return objectId;
        }

        public int getAzimuth() {
            return azimuth;
        }

        public int getDistance() {
            return distance;
        }

        @Override
        public int compareTo(MapObject o) {
            int r = Double.compare(this.azimuth, o.azimuth);
            if(r == 0) {
                r = this.objectId.compareTo(o.objectId);
            }
            if(r == 0) {
                r = Double.compare(this.distance, o.distance);
            }
            return r;
        }

        @Override
        public String toString() {
            return "{" + objectId + " @ " + azimuth + ", " + distance + '}';
        }

        public boolean isEquivalent(MapObject other) {
            return
                    this.objectId.equals(other.objectId) &&
                    abs(this.azimuth - other.azimuth) < MapDynamicsCollector.DETECTABLE_AZIMUTH_CHANGE_DEGREES &&
                    abs(this.distance - other.distance) < MapDynamicsCollector.DETECTABLE_MOTION_CHANGE_CM
            ;
        }
    }

}

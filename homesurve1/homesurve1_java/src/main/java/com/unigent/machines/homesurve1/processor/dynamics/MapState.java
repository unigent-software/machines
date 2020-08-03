package com.unigent.machines.homesurve1.processor.dynamics;

import com.unigent.agentbase.library.core.state.DoublePayload;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapState {

    private final List<MapObject> objects;

    public MapState(List<MapObject> objects) {
        this.objects = objects;
    }

    @Override
    public String toString() {
        return "MapState{" +
                "objects=" + objects +
                '}';
    }

    public static class MapObject implements Comparable<MapObject> {

        private int classId;
        private double azimuth;
        private double distance;

        public MapObject(int classId, double azimuth, double distance) {
            this.classId = classId;
            this.azimuth = azimuth;
            this.distance = distance;
        }

        @Override
        public int compareTo(MapObject o) {
            int r = Double.compare(this.azimuth, o.azimuth);
            if(r == 0) {
                r = Integer.compare(this.classId, o.classId);
            }
            if(r == 0) {
                r = Double.compare(this.distance, o.distance);
            }
            return r;
        }

        @Override
        public String toString() {
            return "MapObject{" +
                    "classId=" + classId +
                    ", azimuth=" + azimuth +
                    ", distance=" + distance +
                    '}';
        }
    }

}

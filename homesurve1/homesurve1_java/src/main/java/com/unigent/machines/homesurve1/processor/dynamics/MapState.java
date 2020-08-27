package com.unigent.machines.homesurve1.processor.dynamics;

import java.util.List;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapState {

    private List<MapObject> objects;

    public MapState() {
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

        public MapObject() {
        }

        public MapObject(int classId, double azimuth, double distance) {
            this.classId = classId;
            this.azimuth = azimuth;
            this.distance = distance;
        }

        public void setClassId(int classId) {
            this.classId = classId;
        }

        public void setAzimuth(double azimuth) {
            this.azimuth = azimuth;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public int getClassId() {
            return classId;
        }

        public double getAzimuth() {
            return azimuth;
        }

        public double getDistance() {
            return distance;
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

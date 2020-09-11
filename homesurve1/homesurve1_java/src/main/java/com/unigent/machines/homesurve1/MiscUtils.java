package com.unigent.machines.homesurve1;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MiscUtils {

    public static int distanceMM(double distanceMeters) {
        return (int) Math.round(distanceMeters * 1000);
    }

}

package com.unigent.machines.homesurve1.state;

import java.util.List;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class DetectedObjects {

    private List<String> classIds;
    private List<String> labels;
    private List<Double> confidences;
    private List<List<Integer>> boxes;

    public List<String> getClassIds() {
        return classIds;
    }

    public void setClassIds(List<String> classIds) {
        this.classIds = classIds;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<Double> getConfidences() {
        return confidences;
    }

    public void setConfidences(List<Double> confidences) {
        this.confidences = confidences;
    }

    public List<List<Integer>> getBoxes() {
        return boxes;
    }

    public void setBoxes(List<List<Integer>> boxes) {
        this.boxes = boxes;
    }
}

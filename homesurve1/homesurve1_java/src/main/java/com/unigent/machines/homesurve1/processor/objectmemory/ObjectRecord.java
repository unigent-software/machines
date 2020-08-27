package com.unigent.machines.homesurve1.processor.objectmemory;

import org.dizitart.no2.objects.Id;

public class ObjectRecord {

    @Id
    private String subjectObjectId; // <classId>-<#>
    private int subjectClassId;
    private String subjectLabel;
    private int contextClassId;
    private int distance; // millimeters
    private int orientation; // degrees

    public ObjectRecord() {
    }

    public ObjectRecord(String subjectObjectId, int subjectClassId, String subjectLabel, int contextClassId, int distance, int orientation) {
        this.subjectObjectId = subjectObjectId;
        this.subjectClassId = subjectClassId;
        this.subjectLabel = subjectLabel;
        this.contextClassId = contextClassId;
        this.distance = distance;
        this.orientation = orientation;
    }

    public String getSubjectObjectId() {
        return subjectObjectId;
    }

    public void setSubjectObjectId(String subjectObjectId) {
        this.subjectObjectId = subjectObjectId;
    }

    public int getSubjectClassId() {
        return subjectClassId;
    }

    public void setSubjectClassId(int subjectClassId) {
        this.subjectClassId = subjectClassId;
    }

    public int getContextClassId() {
        return contextClassId;
    }

    public void setContextClassId(int contextClassId) {
        this.contextClassId = contextClassId;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public String getSubjectLabel() {
        return subjectLabel;
    }

    public void setSubjectLabel(String subjectLabel) {
        this.subjectLabel = subjectLabel;
    }
}

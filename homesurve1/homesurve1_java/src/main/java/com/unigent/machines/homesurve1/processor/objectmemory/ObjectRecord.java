package com.unigent.machines.homesurve1.processor.objectmemory;

import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;

import java.util.StringJoiner;

@Indices(value = {
        @Index(value = "subjectClassId", type = IndexType.NonUnique),
        @Index(value = "contextClassId", type = IndexType.NonUnique),
        @Index(value = "subjectLabel", type = IndexType.NonUnique)
})
public class ObjectRecord {

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

    @Override
    public String toString() {
        return new StringJoiner(", ", ObjectRecord.class.getSimpleName() + "[", "]")
                .add("id='" + subjectObjectId + "'")
                .add("subject=" + subjectClassId)
                .add("label='" + subjectLabel + "'")
                .add("context=" + contextClassId)
                .add("distance=" + distance)
                .add("orientation=" + orientation)
                .toString();
    }
}

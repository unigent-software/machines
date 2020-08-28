package com.unigent.machines.homesurve1.state;

import java.util.StringJoiner;

public class RecognizedSceneObject extends SceneObject {

    private String objectId;

    public RecognizedSceneObject() {
    }

    public RecognizedSceneObject(SceneObject sceneObject, String objectId) {
        this(sceneObject.getClassId(), sceneObject.getLabel(), sceneObject.getDistanceMeters(), sceneObject.getAzimuthDegrees(), objectId);
    }

    public RecognizedSceneObject(int classId, String label, double distanceMeters, double azimuthDegrees, String objectId) {
        super(classId, label, distanceMeters, azimuthDegrees);
        this.objectId = objectId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RecognizedSceneObject.class.getSimpleName() + "[", "]")
                .add("scene='" + super.toString() + "'")
                .add("objectId='" + objectId + "'")
                .toString();
    }
}
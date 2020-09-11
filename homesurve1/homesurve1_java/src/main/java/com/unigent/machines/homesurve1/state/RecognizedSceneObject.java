package com.unigent.machines.homesurve1.state;

import java.util.StringJoiner;

public class RecognizedSceneObject extends SceneObject {

    private String objectId;

    public RecognizedSceneObject() {
    }

    public RecognizedSceneObject(SceneObject sceneObject, String objectId) {
        this(sceneObject.getClassId(), sceneObject.getLabel(), sceneObject.getDistanceMM(), sceneObject.getAzimuthDegrees(), sceneObject.getWidthDegrees(), objectId);
    }

    public RecognizedSceneObject(int classId, String label, int distanceMM, double azimuthDegrees, double widthDegrees, String objectId) {
        super(classId, label, distanceMM, azimuthDegrees, widthDegrees);
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

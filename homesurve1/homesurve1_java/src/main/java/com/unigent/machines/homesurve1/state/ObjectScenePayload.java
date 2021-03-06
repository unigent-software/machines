package com.unigent.machines.homesurve1.state;

import com.unigent.agentbase.sdk.commons.RepresentableAsText;
import com.unigent.agentbase.sdk.serialization.JsonSerializerBase;
import com.unigent.agentbase.sdk.state.StatePayload;
import com.unigent.agentbase.sdk.state.metadata.AgentBaseState;

import java.util.ArrayList;
import java.util.List;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseState(serializerType = ObjectScenePayload.ObjectScenePayloadSerializer.class)
public class ObjectScenePayload implements StatePayload, RepresentableAsText {

    private long sourceImageTimestamp;
    private List<SceneObject> objects;

    public ObjectScenePayload(List<SceneObject> objects, long sourceImageTimestamp) {
        this.objects = objects;
        this.sourceImageTimestamp = sourceImageTimestamp;
    }

    public List<SceneObject> getObjects() {
        return objects;
    }

    public long getSourceImageTimestamp() {
        return sourceImageTimestamp;
    }

    public void setObjects(List<SceneObject> objects) {
        this.objects = objects;
    }

    public static class ObjectScenePayloadSerializer extends JsonSerializerBase<ObjectScenePayload> {
        public Class<ObjectScenePayload> getTargetType() {
            return ObjectScenePayload.class;
        }
    }

    @Override
    public List<String> getTextRepresentation() {
        List<String> result = new ArrayList<>();
        result.add("Timestamp: " + sourceImageTimestamp);
        objects.forEach(o->result.add(o.toString()));
        return result;
    }
}

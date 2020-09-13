package com.unigent.machines.random_finder;

import com.unigent.agentbase.sdk.commons.RepresentableAsText;
import com.unigent.agentbase.sdk.serialization.JsonSerializerBase;
import com.unigent.agentbase.sdk.state.StatePayload;
import com.unigent.agentbase.sdk.state.metadata.AgentBaseState;

import java.util.ArrayList;
import java.util.List;

/**
 * Random Finder Demo Robot
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseState(serializerType = ScenePayload.ObjectScenePayloadSerializer.class)
public class ScenePayload implements StatePayload, RepresentableAsText {

    private long sourceImageTimestamp;
    private List<SceneObject> objects;

    public ScenePayload(List<SceneObject> objects, long sourceImageTimestamp) {
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

    public static class ObjectScenePayloadSerializer extends JsonSerializerBase<ScenePayload> {
        public Class<ScenePayload> getTargetType() {
            return ScenePayload.class;
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

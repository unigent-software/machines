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
@AgentBaseState(serializerType = RecognizedObjectScenePayload.RecognizedObjectScenePayloadSerializer.class)
public class RecognizedObjectScenePayload implements StatePayload, RepresentableAsText {

    private long sourceImageTimestamp;
    private List<RecognizedSceneObject> objects;

    public RecognizedObjectScenePayload(List<RecognizedSceneObject> objects, long sourceImageTimestamp) {
        this.objects = objects;
        this.sourceImageTimestamp = sourceImageTimestamp;
    }

    public List<RecognizedSceneObject> getObjects() {
        return objects;
    }

    public long getSourceImageTimestamp() {
        return sourceImageTimestamp;
    }

    public void setObjects(List<RecognizedSceneObject> objects) {
        this.objects = objects;
    }

    public static class RecognizedObjectScenePayloadSerializer extends JsonSerializerBase<RecognizedObjectScenePayload> {
        public Class<RecognizedObjectScenePayload> getTargetType() {
            return RecognizedObjectScenePayload.class;
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

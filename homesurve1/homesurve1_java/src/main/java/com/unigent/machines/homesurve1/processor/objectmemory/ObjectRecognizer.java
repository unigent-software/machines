package com.unigent.machines.homesurve1.processor.objectmemory;

import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.processing.metadata.ProducedDataFlow;
import com.unigent.agentbase.sdk.state.StateUpdate;
import com.unigent.machines.homesurve1.InitializerProcessor;
import com.unigent.machines.homesurve1.state.ObjectScenePayload;
import com.unigent.machines.homesurve1.state.RecognizedObjectScenePayload;
import com.unigent.machines.homesurve1.state.RecognizedSceneObject;
import com.unigent.machines.homesurve1.state.SceneObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

@AgentBaseProcessor(
        consumedData = @ConsumedDataFlow(localName = "scene", dataType = ObjectScenePayload.class),
        producedData = @ProducedDataFlow(localName = "recognized_scene", dataType = RecognizedObjectScenePayload.class)
)
public class ObjectRecognizer extends ProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    public ObjectRecognizer(String name) {
        super(name);
    }

    @Override
    public void onStateUpdate(@Nonnull StateUpdate stateUpdate, @Nonnull URI sourceTopic, @Nullable String localBinding) {
        checkArgument("scene".equals(localBinding));
        ObjectScenePayload scenePayload = (ObjectScenePayload) stateUpdate.getPayload();

        int objectCount = scenePayload.getObjects().size();
        if(objectCount == 0) {
            log.debug("object_recognizer# Empty scene");
            return;
        }

        List<SceneObject> context = new ArrayList<>(objectCount - 1);
        List<RecognizedSceneObject> recognizedSceneObjects = new ArrayList<>(objectCount);
        ObjectMemory objectMemory = InitializerProcessor.getInstance().getObjectMemory();
        for(int subjectIndex=0; subjectIndex<objectCount; subjectIndex++) {
            context.clear();
            SceneObject subject = null;
            for(int j=0; j<objectCount; j++) {
                if(subjectIndex == j) {
                    subject = scenePayload.getObjects().get(j);
                }
                else {
                    context.add(scenePayload.getObjects().get(j));
                }
            }
            if(context.isEmpty()) {
                continue;
            }
            RecognizedSceneObject object = objectMemory.recognize(Objects.requireNonNull(subject, "Subject is null"), context);
            if(object != null) {
                recognizedSceneObjects.add(object);
            }
        }

        produceStateUpdate(new RecognizedObjectScenePayload(recognizedSceneObjects, scenePayload.getSourceImageTimestamp()), "recognized_scene");
    }
}

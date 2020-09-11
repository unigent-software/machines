package com.unigent.machines.homesurve1.processor.objectmemory;

import com.unigent.agentbase.library.core.state.TensorPayload;
import com.unigent.agentbase.library.core.state.sensor.XYZOrientationReading;
import com.unigent.agentbase.sdk.commons.util.geometry.Geometry;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.processing.metadata.ProducedDataFlow;
import com.unigent.agentbase.sdk.state.StateUpdate;
import com.unigent.machines.homesurve1.InitializerProcessor;
import com.unigent.machines.homesurve1.processor.BodyMotionAwareProcessorBase;
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
import static com.unigent.machines.homesurve1.processor.map.MapDynamicsCollector.azimuthToIntDegrees;

@AgentBaseProcessor(
        consumedData = {
                @ConsumedDataFlow(
                        localName = "scene",
                        dataType = ObjectScenePayload.class
                ),
                @ConsumedDataFlow(
                        dataType = TensorPayload.class,
                        localName = "linear_motion",
                        description = "[path along X,velocity along X, delta time]",
                        receiveUpdates = false
                ),
                @ConsumedDataFlow(
                        dataType = XYZOrientationReading.class,
                        localName = "sensor/orientation/bno055",
                        receiveUpdates = false
                )
        },
        producedData = @ProducedDataFlow(localName = "recognized_scene", dataType = RecognizedObjectScenePayload.class)
)
public class ObjectRecognizer extends BodyMotionAwareProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    public ObjectRecognizer(String name) {
        super(name);
    }

    private RecognizedObjectScenePayload previousScene;
    private Double previousAzimuth;

    @Override
    public void onStateUpdate(@Nonnull StateUpdate stateUpdate, @Nonnull URI sourceTopic, @Nullable String localBinding) {
        checkArgument("scene".equals(localBinding));
        ObjectScenePayload scenePayload = (ObjectScenePayload) stateUpdate.getPayload();

        int objectCount = scenePayload.getObjects().size();
        if(objectCount == 0) {
            log.info("object_recognizer# Empty scene");
            previousScene = null;
            return;
        }

        // Detect motion
        double newAzimuth = getAzimuth(scenePayload.getSourceImageTimestamp());
        int deltaAzimuth = 0;
        if(previousAzimuth != null) {
            deltaAzimuth = azimuthToIntDegrees(Geometry.diffAzimuthDegrees(newAzimuth, previousAzimuth));
            log.info("object_recognizer# deltaAzimuth=" + deltaAzimuth);
        }
        int linearMotionCm = getLinearMotionXcm(scenePayload.getSourceImageTimestamp());


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

        RecognizedObjectScenePayload newScene = new RecognizedObjectScenePayload(recognizedSceneObjects, scenePayload.getSourceImageTimestamp());
        produceStateUpdate(newScene, "recognized_scene");

        previousScene = newScene;
        previousAzimuth = newAzimuth;
    }

    /**
     * Predicts where the object should be according to the motion and prediction matches, returns
     * recognized object from the previous scene
     */
    @Nullable
    private RecognizedSceneObject recognizeByExpectation(SceneObject subject, int deltaAzimuthDeg, int deltaXcm) {
        if(previousScene == null) {
            return null;
        }

        if(deltaXcm > 10) {
            log.info("object_recognizer# expectation: Too much linear movement {}", deltaXcm);
            return null;
        }

        // Where this subject should have been in previous scene
        int predictedAzimuth = (int) Math.round(Geometry.addAzimuthDegrees(subject.getAzimuthDegrees(), -deltaAzimuthDeg));
        return previousScene.findObjectAtAzimuthWithClassId(predictedAzimuth, subject.getClassId());
    }
}

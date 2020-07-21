package com.unigent.machines.homesurve1.processor;

import com.unigent.agentbase.library.core.state.JsonPayload;
import com.unigent.agentbase.library.core.state.TensorPayload;
import com.unigent.agentbase.sdk.processing.AllInputsAvailableProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.processing.metadata.ProducedDataFlow;
import com.unigent.machines.homesurve1.state.DetectedObjects;
import com.unigent.machines.homesurve1.state.ObjectScenePayload;
import com.unigent.machines.homesurve1.state.SceneObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = {
                @ConsumedDataFlow(localName = "detected_objects_json", dataType = JsonPayload.class),
                @ConsumedDataFlow(localName = "depth_image", dataType = TensorPayload.class)
        },
        producedData = {
                @ProducedDataFlow(localName = "scene", dataType = ObjectScenePayload.class)
        }
)
public class ObjectSceneBuilder extends AllInputsAvailableProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Object detector uses 416x416 images. Depth image comes as 640x480;
     */
    private static final double ratioX = 640.0 / 416.0;
    private static final double ratioY = 480.0 / 416.0;

    public ObjectSceneBuilder(String name) {
        super(name, FreshPayloadPolicy.Error);
    }

    @Override
    protected void payloadsReady() {
        JsonPayload detectedObjectsJson = getPayload("detected_objects_json");
        TensorPayload depthImageTensor = getPayload("depth_image");

        DetectedObjects detectedObjects = detectedObjectsJson.toObject(DetectedObjects.class);
        List<SceneObject> sceneObjects = new ArrayList<>(detectedObjects.getBoxes().size());
        for(int i=0; i<detectedObjects.getBoxes().size(); i++) {
            String classId = detectedObjects.getClassIds().get(i);
            String label = detectedObjects.getLabels().get(i);
            int [] box = adjustedBox(detectedObjects.getBoxes().get(i));

            >>>
            sceneObjects.add(new SceneObject());

        }

        ObjectScenePayload scene = new ObjectScenePayload(sceneObjects);
        produceStateUpdate(scene, "scene");
    }

    /**
     *
     * @param originalBox: [x, y, w, h]
     */
    static int [] adjustedBox(List<Integer> originalBox) {
        return new int [] {
                Double.valueOf(originalBox.get(0) * ratioX).intValue(),
                Double.valueOf(originalBox.get(1) * ratioY).intValue(),
                Double.valueOf(originalBox.get(2) * ratioX).intValue(),
                Double.valueOf(originalBox.get(3) * ratioY).intValue()
        };
    }
}

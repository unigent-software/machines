package com.unigent.machines.random_finder;

import com.unigent.agentbase.library.core.state.JsonPayload;
import com.unigent.agentbase.library.core.state.sensor.XYZOrientationReading;
import com.unigent.agentbase.sdk.commons.Config;
import com.unigent.agentbase.sdk.controller.ConsoleHandle;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.processing.InputCollectingProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.processing.metadata.ProcessorConfigProperty;
import com.unigent.agentbase.sdk.processing.metadata.ProducedDataFlow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.unigent.agentbase.sdk.commons.util.geometry.Geometry.addAzimuthDegrees;
import static java.lang.Math.max;

/**
 * Random Finder Demo Robot
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = {
                @ConsumedDataFlow(localName = "detected_objects_json", dataType = JsonPayload.class),
                @ConsumedDataFlow(localName = "orientation", dataType = XYZOrientationReading.class)
        },
        producedData = {
                @ProducedDataFlow(localName = "scene", dataType = ScenePayload.class)
        },
        config = {
                @ProcessorConfigProperty(name = "camera.fov.horizontal", required = true)
        }
)
public class SceneBuilder extends InputCollectingProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Object detector uses 416x416 images
     */

    public static final int size = 416;


    private double horizontalDegreesPerPixel;

    public SceneBuilder(String name) {
        super(name, FreshPayloadPolicy.Replace);
    }

    @Override
    public void configure(Map<String, URI> outputBinding, Config config, ConsoleHandle console, NodeServices nodeServices) {
        super.configure(outputBinding, config, console, nodeServices);
        double cameraHorizontalFieldOfView = config.getInt("camera.fov.horizontal");

        horizontalDegreesPerPixel = cameraHorizontalFieldOfView / size;
        log.info("Configured with cameraHorizontalFieldOfView={}", cameraHorizontalFieldOfView);
    }

    @Override
    protected void payloadsReady() {
        JsonPayload detectedObjectsJson = getPayload("detected_objects_json");
        XYZOrientationReading orientation = getPayload("orientation");

        DetectedObjects detectedObjects = detectedObjectsJson.toObject(DetectedObjects.class);
        List<SceneObject> sceneObjects = new ArrayList<>(detectedObjects.getBoxes().size());

        double centerAzimuth = orientation.getX();
        double leftEdgeAzimuth = addAzimuthDegrees(centerAzimuth, -(size / 2.0) * this.horizontalDegreesPerPixel);

        for(int i=0; i<detectedObjects.getBoxes().size(); i++) {
            int classId = detectedObjects.getClassIds().get(i);
            String label = detectedObjects.getLabels().get(i);
            List<Integer> box = detectedObjects.getBoxes().get(i); // x,y,w,h

            int boxX = max(0, box.get(0));
//            int boxY = max(0, box.get(1));
            int boxWidth = box.get(2);
//            int boxHeight = box.get(3);

            // Azimuth
            int centerX = boxX + boxWidth / 2;
            double objectAzimuth = addAzimuthDegrees(leftEdgeAzimuth, centerX * this.horizontalDegreesPerPixel);

            // Angular width
            double objectAngularWidth = boxWidth * this.horizontalDegreesPerPixel;

            sceneObjects.add(new SceneObject(classId, label, objectAzimuth, objectAngularWidth));
        }

        produceStateUpdate(new ScenePayload(sceneObjects, detectedObjects.getSourceImageTimestamp()), "scene");
    }
}

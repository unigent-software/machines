package com.unigent.machines.homesurve1.processor;

import com.unigent.agentbase.library.core.state.JsonPayload;
import com.unigent.agentbase.library.core.state.TensorPayload;
import com.unigent.agentbase.library.core.state.sensor.XYZOrientationReading;
import com.unigent.agentbase.sdk.commons.Config;
import com.unigent.agentbase.sdk.commons.util.Mean;
import com.unigent.agentbase.sdk.commons.util.geometry.Geometry;
import com.unigent.agentbase.sdk.controller.ConsoleHandle;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.processing.InputCollectingProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.processing.metadata.ProcessorConfigProperty;
import com.unigent.agentbase.sdk.processing.metadata.ProducedDataFlow;
import com.unigent.machines.homesurve1.state.DetectedObjects;
import com.unigent.machines.homesurve1.state.ObjectSceneDebugPayload;
import com.unigent.machines.homesurve1.state.ObjectScenePayload;
import com.unigent.machines.homesurve1.state.SceneObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.unigent.agentbase.sdk.commons.util.geometry.Geometry.addAzimuthDegrees;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * RealSense cameras FOV: https://www.intel.com/content/www/us/en/support/articles/000030385/emerging-technologies/intel-realsense-technology.html
 *
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = {
                @ConsumedDataFlow(localName = "detected_objects_json", dataType = JsonPayload.class),
                @ConsumedDataFlow(localName = "depth_image", dataType = TensorPayload.class),
                @ConsumedDataFlow(localName = "orientation", dataType = XYZOrientationReading.class)
        },
        producedData = {
                @ProducedDataFlow(localName = "scene", dataType = ObjectScenePayload.class),
                @ProducedDataFlow(localName = "scene_debug", dataType = ObjectSceneDebugPayload.class)
        },
        config = {
                @ProcessorConfigProperty(name = "debug", required = true),
                @ProcessorConfigProperty(name = "camera.fov.horizontal", required = true)
        }
)
public class ObjectSceneBuilder extends InputCollectingProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Object detector uses 416x416 images. Depth image comes as 640x480;
     */

    public static final int fullWidth = 640;
    public static final int fullHeight = 480;

    private static final double ratioX = fullWidth / 416.0;
    private static final double ratioY = fullHeight / 416.0;

    private double horizontalDegreesPerPixel;
    private boolean debugMode;

    public ObjectSceneBuilder(String name) {
        super(name, FreshPayloadPolicy.Replace);
    }

    @Override
    public void configure(Map<String, URI> outputBinding, Config config, ConsoleHandle console, NodeServices nodeServices) {
        super.configure(outputBinding, config, console, nodeServices);
        double cameraHorizontalFieldOfView = config.getInt("camera.fov.horizontal");

        horizontalDegreesPerPixel = cameraHorizontalFieldOfView / fullWidth;
        debugMode = config.getBoolean("debug", false);
        log.info("Configured with cameraHorizontalFieldOfView={}, Debug:{}", cameraHorizontalFieldOfView, debugMode);
    }

    @Override
    protected void payloadsReady() {
        JsonPayload detectedObjectsJson = getPayload("detected_objects_json");
        TensorPayload depthImageTensor = getPayload("depth_image");
        XYZOrientationReading orientation = getPayload("orientation");

        DetectedObjects detectedObjects = detectedObjectsJson.toObject(DetectedObjects.class);
        List<SceneObject> sceneObjects = new ArrayList<>(detectedObjects.getBoxes().size());

        double centerAzimuth = orientation.getAzimuth();
        double leftEdgeAzimuth = addAzimuthDegrees(centerAzimuth, -(fullWidth / 2.0) * this.horizontalDegreesPerPixel);
        double [][] depthData  = depthImageTensor.toMatrix();

        for(int i=0; i<detectedObjects.getBoxes().size(); i++) {
            String classId = detectedObjects.getClassIds().get(i);
            String label = detectedObjects.getLabels().get(i);
            int [] box = adjustedBox(detectedObjects.getBoxes().get(i)); // x,y,w,h

            int boxX = max(0, box[0]);
            int boxY = max(0, box[1]);
            int boxWidth = box[2];
            int boxHeight = box[3];

            // Azimuth
            int centerX = boxX + boxWidth / 2;
            double objectAzimuth = addAzimuthDegrees(leftEdgeAzimuth, centerX * this.horizontalDegreesPerPixel);

            // Distance
            Mean depthMean = new Mean();
            for(int r=boxY; r<min(fullHeight - 1, boxY + boxHeight); r++) {
                for(int c=boxX; c<min(fullWidth - 1, boxX + boxWidth); c++) {
                    depthMean.increment(depthData[r][c]);
                }
            }
            double depth = depthMean.getValue();
            sceneObjects.add(new SceneObject(classId, label, depth, objectAzimuth));
        }

        produceStateUpdate(new ObjectScenePayload(sceneObjects), "scene");

        if(debugMode) {
            produceStateUpdate(new ObjectSceneDebugPayload(depthData, sceneObjects, detectedObjects.getBoxes()), "scene_debug");
        }
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

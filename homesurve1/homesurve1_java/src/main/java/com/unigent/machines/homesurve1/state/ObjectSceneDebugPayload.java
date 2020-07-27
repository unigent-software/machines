package com.unigent.machines.homesurve1.state;

import com.unigent.agentbase.sdk.commons.RepresentableAsImage;
import com.unigent.agentbase.sdk.commons.util.Images;
import com.unigent.agentbase.sdk.serialization.JsonSerializerBase;
import com.unigent.agentbase.sdk.state.StatePayload;
import com.unigent.agentbase.sdk.state.metadata.AgentBaseState;
import com.unigent.machines.homesurve1.processor.ObjectSceneBuilder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseState(serializerType = ObjectSceneDebugPayload.ObjectSceneDebugPayloadSerializer.class)
public class ObjectSceneDebugPayload implements StatePayload, RepresentableAsImage {

    private final double [][] depthData;
    private final List<SceneObject> sceneObjects;
    private final List<List<Integer>> boxes;

    public ObjectSceneDebugPayload(double[][] depthData, List<SceneObject> sceneObjects, List<List<Integer>> boxes) {
        this.depthData = depthData;
        this.sceneObjects = sceneObjects;
        this.boxes = boxes;
    }

    @Override
    public List<BufferedImage> getImageRepresentation() {
        // Depth (cropped to 480 x 480)
        int depthImageSize = ObjectSceneBuilder.fullHeight;
        BufferedImage depthImage = new BufferedImage(depthImageSize, depthImageSize, BufferedImage.TYPE_INT_RGB);
        for(int r = 0; r < depthImageSize; r++) {
            for(int c = 0; c < depthImageSize; c++) {
                depthImage.setRGB(c, r, Color.getHSBColor((float) (depthData[r][c + ObjectSceneBuilder.halfWidth] / 5.0), 0.8f, 0.8f).getRGB());
            }
        }

        // Scale image to 416 x 416
        BufferedImage depthImage416 = Images.resize(depthImage, ObjectSceneBuilder.size, ObjectSceneBuilder.size);

        // Boxes
        Graphics2D g = (Graphics2D) depthImage416.getGraphics();
        g.setColor(Color.WHITE);
        for(int i=0; i<sceneObjects.size(); i++) {

            SceneObject so = sceneObjects.get(i);
            int x = boxes.get(i).get(0);
            int y = boxes.get(i).get(1);
            int w = boxes.get(i).get(2);
            int h = boxes.get(i).get(3);

            g.drawRect(x, y, w, h);
            g.drawString(so.getLabel(), x + w/2, y + h/2);
        }
        g.dispose();

        return Collections.singletonList(depthImage416);
    }

    public class ObjectSceneDebugPayloadSerializer extends JsonSerializerBase<ObjectSceneDebugPayload> {
        @Override
        public Class<ObjectSceneDebugPayload> getTargetType() {
            return ObjectSceneDebugPayload.class;
        }
    }
}

package com.unigent.machines.homesurve1.processor;

import com.unigent.agentbase.sdk.commons.util.Maths;
import com.unigent.agentbase.sdk.commons.util.Mean;
import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.processing.metadata.ProducedDataFlow;
import com.unigent.agentbase.sdk.state.StateUpdate;
import com.unigent.machines.homesurve1.state.ObjectScenePayload;
import com.unigent.machines.homesurve1.state.SceneObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = @ConsumedDataFlow(dataType = ObjectScenePayload.class, localName = "scene_input"),
        producedData = @ProducedDataFlow(dataType = ObjectScenePayload.class, localName = "scene_output")
)
public class ObjectSceneFilter extends ProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TIME_BUCKET_MILLIS = 500;
    private static final int OBJECT_DECAY_MILLIS = 2000;
    private static final double DISTANCE_TOLERANCE_METERS = 0.3;
    private static final double AZIMUTH_TOLERANCE_DEGREES = 2.0;

    private final Set<TrackedObject> trackedObjects = new HashSet<>();

    public ObjectSceneFilter(String name) {
        super(name);
    }

    @Override
    public void onStateUpdate(@Nonnull StateUpdate stateUpdate, @Nonnull URI sourceTopic, @Nullable String localBinding) {
        checkState("scene_input".equals(localBinding));
        ObjectScenePayload incomingScene = (ObjectScenePayload) stateUpdate.getPayload();

        long now = System.currentTimeMillis();

        // Clear expired objects
        clearExpiredObjects(now);

        // Inspect new scene
        boolean change = false;
        for(SceneObject sceneObject : incomingScene.getObjects()) {
            TrackedObject match = findTrackedObject(sceneObject);
            if(match == null) {
                match = new TrackedObject(now, sceneObject);
                this.trackedObjects.add(match);
            }

            change |= match.addData(now, sceneObject.getDistanceMeters(), sceneObject.getAzimuthDegrees());
        }

        if(change) {
            produceStateUpdate(createScene(), "scene_output");
        }

    }

    private ObjectScenePayload createScene() {
        return new ObjectScenePayload(this.trackedObjects.stream()
                .filter(TrackedObject::isMature)
                .map(TrackedObject::toSceneObject)
                .collect(Collectors.toList())
        );
    }

    private static class TrackedObject {

        long lastSawAtMillis;
        final String classId;
        final String label;

        boolean mature;
        RangeAndValue distance;
        RangeAndValue azimuth;

        LinkedList<Measurement> history = new LinkedList<>();

        public TrackedObject(long lastSawAtMillis, SceneObject sceneObject) {
            this.lastSawAtMillis = lastSawAtMillis;
            this.classId = sceneObject.getClassId();
            this.label = sceneObject.getLabel();
        }

        public boolean addData(long now, double distance, double azimuth) {
            this.lastSawAtMillis = now;
            this.history.add(new Measurement(now, distance, azimuth));

            // Still maturing?
            if(!mature) {
                if(this.history.getFirst().timestamp < now - TIME_BUCKET_MILLIS) {
                    mature = true;
                    recalculateDistance();
                    recalculateAzimuth();
                    return true;
                }
                else {
                    return false;
                }
            }

            // Mature!

            // Cleanup
            Set<Measurement> toRemove = new HashSet<>();
            boolean sufficientTimeBucket = false;
            for(Measurement m : this.history) {
                if(now - m.timestamp > OBJECT_DECAY_MILLIS) {
                    toRemove.add(m);
                }
            }
            this.history.removeAll(toRemove);

            // Fits range?
            boolean change = false;
            if(!this.distance.fits(distance)) {
                recalculateDistance();
                change = true;
            }
            if(!this.azimuth.fits(azimuth)) {
                recalculateAzimuth();
                change = true;
            }

            return change;
        }

        public boolean isMature() {
            return mature;
        }

        private void recalculateDistance() {
            this.distance = recalculateValue(m->m.distance);
        }

        private void recalculateAzimuth() {
            this.azimuth = recalculateValue(m->m.azimuth);
        }

        private RangeAndValue recalculateValue(Function<Measurement, Double> valueSource) {
            Mean mean = new Mean();
            double min = Double.MAX_VALUE;
            double max = 0;
            for(Measurement m : this.history) {
                double value = valueSource.apply(m);
                mean.increment(value);
                min = min(min, value);
                max = max(max, value);
            }
            return new RangeAndValue(mean.getValue(), min, max);
        }

        public SceneObject toSceneObject() {
            return new SceneObject(classId, label, distance.value, azimuth.value);
        }

    }

    private static class Measurement {
        final long timestamp;
        final double distance;
        final double azimuth;

        public Measurement(long timestamp, double distance, double azimuth) {
            this.timestamp = timestamp;
            this.distance = distance;
            this.azimuth = azimuth;
        }
    }

    private static class RangeAndValue {

        final double value;
        final double valueMin;
        final double valueMax;

        public RangeAndValue(double value, double valueMin, double valueMax) {
            this.value = value;
            this.valueMin = valueMin;
            this.valueMax = valueMax;
        }

        public boolean fits(double v) {
            return v >= valueMin && v <= valueMax;
        }
    }

    private synchronized void clearExpiredObjects(long now) {
        long expiredBefore = now - OBJECT_DECAY_MILLIS;
        Set<TrackedObject> toRemove = new HashSet<>();
        this.trackedObjects.stream().filter(to -> to.lastSawAtMillis < expiredBefore).forEach(toRemove::add);

        if(!toRemove.isEmpty()) {
            log.debug("Removing {} expired objects", toRemove.size());
            this.trackedObjects.removeAll(toRemove);
        }
    }

    @Nullable
    private TrackedObject findTrackedObject(SceneObject object) {
        return this.trackedObjects.stream()
                .filter(to->
                        to.classId.equals(object.getClassId()) &&
                        Maths.shortestDeltaDegrees(to.azimuth.value, object.getAzimuthDegrees()) < AZIMUTH_TOLERANCE_DEGREES &&
                        Math.abs(to.distance.value - object.getDistanceMeters()) < DISTANCE_TOLERANCE_METERS
                )
                .findFirst()
                .orElse(null);
    }
}

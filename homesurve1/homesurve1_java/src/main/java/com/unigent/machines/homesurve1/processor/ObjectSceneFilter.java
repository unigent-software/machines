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
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.*;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = @ConsumedDataFlow(dataType = ObjectScenePayload.class, localName = "scene_input"),
        producedData = @ProducedDataFlow(dataType = ObjectScenePayload.class, localName = "scene_output"),
        description = "Eliminates pulsating, shivering and double objects from the scene"
)
public class ObjectSceneFilter extends ProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TIME_BUCKET_MILLIS = 800;
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
        boolean change = clearExpiredObjects(now);

        // Inspect new scene
        int matchCount = 0;
        for(SceneObject sceneObject : incomingScene.getObjects()) {
            TrackedObject match = findTrackedObject(sceneObject);
            if(match == null) {
                match = new TrackedObject(now, sceneObject);
                this.trackedObjects.add(match);
                log.info("scenefilter# Added object '{}' at {} deg, {} m", match.label, sceneObject.getAzimuthDegrees(), sceneObject.getDistanceMeters());
            }
            else {
                matchCount ++;
            }

            change |= match.addData(now, sceneObject.getDistanceMeters(), sceneObject.getAzimuthDegrees());
        }

        log.info("scenefilter# Matched {} of {}. Change: {}. Tracking {} objects", matchCount, incomingScene.getObjects().size(), change, this.trackedObjects.size());

        if(change) {
            ObjectScenePayload resultScene = new ObjectScenePayload(
                    this.trackedObjects.stream()
                            .filter(TrackedObject::isMature)
                            .map(TrackedObject::toSceneObject)
                            .collect(Collectors.toList()),
                    incomingScene.getSourceImageTimestamp()
            );
            log.info("scenefilter# Created scene with {} objects", resultScene.getObjects().size());
            produceStateUpdate(resultScene, "scene_output");
        }
    }

    private static class TrackedObject {

        long lastSawAtMillis;
        final int classId;
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

            boolean change = false;

            // Cleanup
            Set<Measurement> toRemove = new HashSet<>();
            for(Measurement m : this.history) {
                if(now - m.timestamp > OBJECT_DECAY_MILLIS) {
                    toRemove.add(m);
                }
            }

            this.history.removeAll(toRemove);

            recalculateDistance();
            recalculateAzimuth();

            return change;
        }

        public double getCurrentOrLastDistance() {
            return mature ? distance.value : history.getLast().distance;
        }

        public double getCurrentOrLastAzimuth() {
            return mature ? azimuth.value : history.getLast().azimuth;
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


        @Override
        public String toString() {
            return new StringJoiner(", ", TrackedObject.class.getSimpleName() + "[", "]")
                    .add("label='" + label + "'")
                    .add("distance=" + distance)
                    .add("azimuth=" + azimuth)
                    .toString();
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

        @Override
        public String toString() {
            return new StringJoiner(", ", RangeAndValue.class.getSimpleName() + "[", "]")
                    .add("value=" + value)
                    .add("valueMin=" + valueMin)
                    .add("valueMax=" + valueMax)
                    .toString();
        }
    }

    private synchronized boolean clearExpiredObjects(long now) {
        long expiredBefore = now - OBJECT_DECAY_MILLIS;
        Set<TrackedObject> toRemove = new HashSet<>();
        this.trackedObjects.stream().filter(to -> to.lastSawAtMillis < expiredBefore).forEach(toRemove::add);

        if(!toRemove.isEmpty()) {
            log.debug("Removing {} expired objects", toRemove.size());
            this.trackedObjects.removeAll(toRemove);
            return true;
        }

        return false;
    }

    @Nullable
    private TrackedObject findTrackedObject(SceneObject object) {
        return this.trackedObjects.stream()
                .filter(to->
                        to.classId == object.getClassId() &&
                        abs(Maths.shortestDeltaDegrees(to.getCurrentOrLastAzimuth(), object.getAzimuthDegrees())) < AZIMUTH_TOLERANCE_DEGREES &&
                        abs(to.getCurrentOrLastDistance() - object.getDistanceMeters()) < DISTANCE_TOLERANCE_METERS
                )
                .findFirst()
                .orElse(null);
    }
}

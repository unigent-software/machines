package com.unigent.machines.homesurve1.processor.objectmemory;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Iterables;
import com.unigent.agentbase.sdk.commons.Initiatable;
import com.unigent.agentbase.sdk.commons.util.JSON;
import com.unigent.agentbase.sdk.persistence.NitriteManager;
import com.unigent.machines.homesurve1.processor.dynamics.MapStateTransition;
import com.unigent.machines.homesurve1.state.RecognizedSceneObject;
import com.unigent.machines.homesurve1.state.SceneObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dizitart.no2.IndexOptions;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static org.dizitart.no2.IndexOptions.indexOptions;
import static org.dizitart.no2.objects.filters.ObjectFilters.*;

public class ObjectMemory implements Initiatable {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private Nitrite objectMemoryDb;
    private ObjectRepository<ObjectRecord> objectRecordRepo;
    private ObjectRepository<ObjectIdCounter> objectIdCounterRepo;

    private final NitriteManager nitriteManager;

    public ObjectMemory(NitriteManager nitriteManager) {
        this.nitriteManager = nitriteManager;
    }

    @Override
    public void initiate() {
        this.objectMemoryDb = nitriteManager.getNitrite("object_memory");
        this.objectRecordRepo = this.objectMemoryDb.getRepository(ObjectRecord.class);
        this.objectIdCounterRepo = this.objectMemoryDb.getRepository(ObjectIdCounter.class);

        if(!this.objectRecordRepo.hasIndex("subjectClassId")) {
            this.objectRecordRepo.createIndex("subjectClassId", indexOptions(IndexType.NonUnique));
        }
        if(!this.objectRecordRepo.hasIndex("subjectLabel")) {
            this.objectRecordRepo.createIndex("subjectLabel", indexOptions(IndexType.NonUnique));
        }
        if(!this.objectRecordRepo.hasIndex("contextClassId")) {
            this.objectRecordRepo.createIndex("contextClassId", indexOptions(IndexType.NonUnique));
        }
    }

    @Override
    public void shutdown() {
        this.objectMemoryDb.close();
    }

    private static final int DISTANCE_PRECISION_MILLIMETERS = 100;
    private static final int ORIENTATION_PRECISION_DEGREES = 2;

    @Nullable
    public RecognizedSceneObject recognize(SceneObject subject, Collection<SceneObject> contextObjects) {

        log.info("objmemory# Recognize {} in {}", subject.getLabel(), contextObjects.stream().map(SceneObject::getLabel).collect(Collectors.toList()));

        // Special case when there is no context
        if(contextObjects.isEmpty()) {
            return null;
        }

        Map<String, Integer> idMatches = new HashMap<>();
        for(SceneObject ctx : contextObjects) {
            int gammaDegrees = gammaDegrees(ctx, subject);
            int distanceMM = distanceMM(subject, ctx, gammaDegrees);
            ObjectRecord match = findMatchingRecord(subject.getClassId(), ctx.getClassId(), distanceMM, gammaDegrees);
            if(match != null) {
                idMatches.compute(match.getSubjectObjectId(), (k,v) -> v == null ? 1 : v + 1);
            }
        }

        String recognizedObjectId;
        if(idMatches.isEmpty()) {
            // No matches -> save as a new object
            recognizedObjectId = saveNewSubject(subject, contextObjects);
        }
        else {
            // Vote for best match
            recognizedObjectId = idMatches.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
        }

        RecognizedSceneObject result = new RecognizedSceneObject(subject, recognizedObjectId);
        log.info("objmemory# Result: {}", result.getObjectId());
        return result;
    }

    public long size() {
        return this.objectRecordRepo.size();
    }

    public int dump(PrintWriter output) {
        int cnt = 0;
        try {
            ObjectWriter objectWriter = JSON.mapper.writerWithDefaultPrettyPrinter();
            for(ObjectRecord objectRecord : this.objectRecordRepo.find()) {
                output.println(objectWriter.writeValueAsString(objectRecord));
                cnt ++;
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to dump: " + e.getMessage(), e);
        }
        log.info("Dumped {} items", cnt);
        return cnt;
    }

    public List<String> findObjectsByLabel(String label) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        this.objectRecordRepo.find(eq("subjectLabel", label)).forEach(objectRecord -> result.add(objectRecord.getSubjectObjectId()));
        return new ArrayList<>(result);
    }

    public List<String> findObjects() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        this.objectRecordRepo.find().forEach(objectRecord -> result.add(objectRecord.getSubjectObjectId()));
        return new ArrayList<>(result);
    }

    private String saveNewSubject(SceneObject subject, Collection<SceneObject> contextObjects) {
        String newObjectId = subject.getLabel() + "-" + nextCounter(subject.getClassId());
        for(SceneObject ctx : contextObjects) {
            int gammaDegrees = gammaDegrees(ctx, subject);
            ObjectRecord newRecord = new ObjectRecord(
                    newObjectId,
                    subject.getClassId(),
                    subject.getLabel(),
                    ctx.getClassId(),
                    distanceMM(subject, ctx, gammaDegrees),
                    gammaDegrees
            );
            this.objectRecordRepo.insert(newRecord);
        }
        this.objectMemoryDb.commit();
        return newObjectId;
    }

    private static int gammaDegrees(SceneObject a, SceneObject b) {
        return (int) round(abs(a.getAzimuthDegrees() - b.getAzimuthDegrees()));
    }

    // a^2 = b^2 + c^2 - 2*b*c*cos(A)
    private static int distanceMM(SceneObject a, SceneObject b, int gammaDegrees) {
        return (int) round(
                sqrt(
                        a.getDistanceMeters() * a.getDistanceMeters() +
                        b.getDistanceMeters() * b.getDistanceMeters() -
                        2 * a.getDistanceMeters() * b.getDistanceMeters() * cos(toRadians(gammaDegrees))
                )
                * 1000 // meters -> mm
        );
    }


    @Nullable
    private synchronized ObjectRecord findMatchingRecord(int subjectClassId, int contextClassId, int distance, int orientation) {
        return this.objectRecordRepo
                .find(
                    and(
                            eq("subjectClassId", subjectClassId),
                            eq("contextClassId", contextClassId),
                            gte("distance", distance - DISTANCE_PRECISION_MILLIMETERS),
                            lte("distance", distance + DISTANCE_PRECISION_MILLIMETERS),
                            gte("orientation", orientation - ORIENTATION_PRECISION_DEGREES),
                            lte("orientation", orientation + ORIENTATION_PRECISION_DEGREES)
                    )
                )
                .firstOrDefault();
    }

    private synchronized int nextCounter(int classId) {
        ObjectIdCounter counter = Iterables.getFirst(this.objectIdCounterRepo.find(eq("type", classId)), null);
        if(counter == null) {
            counter = new ObjectIdCounter(classId, 0);
            this.objectIdCounterRepo.insert(counter);
        }
        else {
            counter.setCounter(counter.getCounter() + 1);
            this.objectIdCounterRepo.update(counter);
        }
        this.objectMemoryDb.commit();
        return counter.getCounter();
    }
}

package com.unigent.machines.homesurve1.processor.dynamics;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Streams;
import com.unigent.agentbase.sdk.commons.Initiatable;
import com.unigent.agentbase.sdk.commons.util.JSON;
import com.unigent.agentbase.sdk.commons.util.geometry.Geometry;
import com.unigent.agentbase.sdk.persistence.NitriteManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.unigent.machines.homesurve1.processor.dynamics.MapDynamicsCollector.DETECTABLE_AZIMUTH_CHANGE_DEGREES;
import static com.unigent.machines.homesurve1.processor.dynamics.MapDynamicsCollector.DETECTABLE_MOTION_CHANGE_CM;
import static org.dizitart.no2.objects.filters.ObjectFilters.*;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapDynamicsMemory implements Initiatable {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private final NitriteManager nitriteManager;

    private Nitrite mapDynamicsDb;
    private ObjectRepository<MapStateTransition> mapStateTransitionRepo;

    public MapDynamicsMemory(NitriteManager nitriteManager) {
        this.nitriteManager = nitriteManager;
    }

    @Override
    public void initiate() {
        this.mapDynamicsDb = nitriteManager.getNitrite("map_dynamics");
        this.mapStateTransitionRepo = this.mapDynamicsDb.getRepository(MapStateTransition.class);
    }

    @Override
    public void shutdown() {
        this.mapDynamicsDb.close();
    }

    public void store(MapStateTransition stateTransition) {
        this.mapStateTransitionRepo.insert(stateTransition);
        this.mapDynamicsDb.commit();
    }

    public List<MapStateTransition> findTransitionsByTargetObjectId(String objectId) {
        return this.mapStateTransitionRepo
                .find(elemMatch("toState", elemMatch("objects", eq("objectId", objectId))))
                .toList();
    }

    public Collection<MapStateTransition> findByTransitionsFromState(MapState fromState) {
        Set<MapStateTransition> transitions = new HashSet<>();
        for(MapState.MapObject stateObj : fromState.getObjects()) {
            StreamSupport
                    .stream(
                            this.mapStateTransitionRepo
                                .find(
                                        elemMatch(
                                                "fromState",
                                                elemMatch("objects",
                                                        and(
                                                                eq("objectId", stateObj.getObjectId()),
                                                                lte("distance", stateObj.getDistance() + DETECTABLE_MOTION_CHANGE_CM),
                                                                gte("distance", stateObj.getDistance() - DETECTABLE_MOTION_CHANGE_CM)
                                                        )

                                                )
                                        )
                                )
                                .spliterator(),
                            false
                    )
                    .filter(stateTransition ->
                            stateTransition.getFromState().getObjects()
                                    .stream()
                                    .anyMatch(mapObj->
                                            mapObj.getObjectId().equals(stateObj.getObjectId()) &&
                                            mapObj.getDistance() <= stateObj.getDistance() + DETECTABLE_MOTION_CHANGE_CM &&
                                            mapObj.getDistance() >= stateObj.getDistance() - DETECTABLE_MOTION_CHANGE_CM &&
                                            Geometry.diffAzimuthDegrees(mapObj.getAzimuth(), stateObj.getAzimuth()) <= DETECTABLE_AZIMUTH_CHANGE_DEGREES
                                    )
                    )
                    .forEach(transitions::add);
        }

        return transitions;
    }

    long size() {
        return this.mapStateTransitionRepo.size();
    }

    void clear() {
        this.mapStateTransitionRepo.drop();
        this.mapStateTransitionRepo = this.mapDynamicsDb.getRepository(MapStateTransition.class);
    }

    Iterable<MapStateTransition> findAll() {
        return this.mapStateTransitionRepo.find();
    }

    int dump(PrintWriter output) {
        int cnt = 0;
        try {
            ObjectWriter objectWriter = JSON.mapper.writerWithDefaultPrettyPrinter();
            for(MapStateTransition stateTransition : this.mapStateTransitionRepo.find()) {
                output.println(objectWriter.writeValueAsString(stateTransition));
                cnt ++;
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to dump: " + e.getMessage(), e);
        }
        log.info("Dumped {} items", cnt);
        return cnt;
    }
}

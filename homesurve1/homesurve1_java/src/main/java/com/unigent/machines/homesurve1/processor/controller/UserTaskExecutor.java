package com.unigent.machines.homesurve1.processor.controller;

import com.unigent.agentbase.library.core.state.StringPayload;
import com.unigent.agentbase.sdk.commons.Config;
import com.unigent.agentbase.sdk.controller.ConsoleHandle;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.processing.metadata.DiscreteActionInfo;
import com.unigent.agentbase.sdk.rl.DiscreteAction;
import com.unigent.agentbase.sdk.state.StateUpdate;
import com.unigent.machines.homesurve1.InitializerProcessor;
import com.unigent.machines.homesurve1.processor.dynamics.MapAction;
import com.unigent.machines.homesurve1.processor.dynamics.MapDynamicsMemory;
import com.unigent.machines.homesurve1.processor.dynamics.MapState;
import com.unigent.machines.homesurve1.processor.dynamics.MapStateTransition;
import com.unigent.machines.homesurve1.processor.objectmemory.ObjectMemory;
import com.unigent.machines.homesurve1.state.ObjectScenePayload;
import com.unigent.machines.homesurve1.state.RecognizedObjectScenePayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static com.unigent.machines.homesurve1.processor.dynamics.MapDynamicsCollector.sceneToMapState;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = {
                @ConsumedDataFlow(localName = "target_label", dataType = StringPayload.class, receiveUpdates = false),
                @ConsumedDataFlow(localName = "recognized_scene", dataType = ObjectScenePayload.class, receiveUpdates = false)
        },
        discreteActions = @DiscreteActionInfo(uri = "go", description = "Goes to an object labeled by payload in target label topic")
)
public class UserTaskExecutor extends ProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    static final URI TOPIC_CURRENT_SCENE = URI.create("recognized_scene");
    static final URI TARGET_LABEL = URI.create("target_label");
    static final String ACTION_SPACE = "task";
    static final String ACTION_GO = "go";

    public UserTaskExecutor(String name) {
        super(name);
    }

    @Override
    public void configure(Map<String, URI> outputBinding, Config config, ConsoleHandle console, NodeServices nodeServices) {
        super.configure(outputBinding, config, console, nodeServices);
        nodeServices.console.registerCommandHandler(new UserTaskConsoleHandler());
    }

    @Override
    public boolean executeAction(@Nonnull String actionSpaceUri, @Nonnull DiscreteAction action, @Nonnull String taskId) {
        switch (action.getUri()) {
            case ACTION_GO:
                StateUpdate targetLabelState = nodeServices.stateBus.ensureTopic(TARGET_LABEL).getMostRecentData();
                if (targetLabelState == null) {
                    console.inform("Nothing in " + TARGET_LABEL);
                    return false;
                }

                String label = ((StringPayload) targetLabelState.getPayload()).getValue();
                ok("will start looking for " + label);
                execGo(label);
                break;

            default:
                sorry("I don't know how to " + action.getUri());
        }
        return true;
    }

    private void ok(String text) {
        console.inform("OK, " + text);
    }

    private void sorry(String text) {
        console.inform("Sorry, " + text);
    }

    private void execGo(String label) {
        ObjectMemory objectMemory = InitializerProcessor.getInstance().getObjectMemory();
        List<String> objectIds = objectMemory.findObjectsByLabel(label);
        if (objectIds.isEmpty()) {
            sorry("I've never seen a " + label);
            return;
        }
        ok("I've seen " + objectIds.size() + " " + label + "s");

        MapState currentState = getCurrentState();
        if (currentState == null) {
            sorry("I don't know where I am");
            return;
        }

        List<MapAction> plan = null;
        for (String objectId : objectIds) {
            ok("Looking for path from " + currentState + " to " + objectId + "...");
            plan = findPath(currentState, objectId);
            if (plan != null) {
                break;
            }
        }

        if (plan == null) {
            sorry("I don't know how to get to " + label);
            return;
        }

        ok("Found a path: " + plan);
    }

    private MapState getCurrentState() {
        RecognizedObjectScenePayload currentScene = nodeServices.stateBus.ensureTopic(TOPIC_CURRENT_SCENE).getMostRecentDataPayload();
        if (currentScene == null) {
            return null;
        }
        return sceneToMapState(currentScene);
    }

    @Nullable
    private List<MapAction> findPath(MapState fromState, final String toObjectId) {
        MapDynamicsMemory mapDynamicsMemory = InitializerProcessor.getInstance().getMapDynamicsMemory();
        List<MapState> goalStates = mapDynamicsMemory.findTransitionsByTargetObjectId(toObjectId).stream().map(MapStateTransition::getToState).collect(Collectors.toList());
        log.info("task# Found {} goal states", goalStates.size());
        if (goalStates.isEmpty()) {
            return null;
        }

        List<MapAction> path = null;
        for(MapState goalState : goalStates) {
            path = findPath(fromState, goalState, mapDynamicsMemory);
            if(path != null) {
                break;
            }
        }

        return path;
    }

    static List<MapAction> findPath(MapState fromState, MapState goalState, MapDynamicsMemory mapDynamicsMemory) {
        if(fromState.matches(goalState)) {
            return Collections.emptyList();
        }
        List<MapStateTransition> path = new ArrayList<>();
        boolean found = findPathImpl(fromState, goalState, path, mapDynamicsMemory);
        if(!found) {
            return null;
        }
        Collections.reverse(path);
        return path.stream().map(MapStateTransition::getAction).collect(Collectors.toList());
    }

    private static boolean findPathImpl(MapState currState, MapState goalState, List<MapStateTransition> path, MapDynamicsMemory mapDynamicsMemory) {
        if(currState.matches(goalState)) {
            return true;
        }
        for(MapStateTransition transitionFromCurrentState : mapDynamicsMemory.findByTransitionsFromState(currState)) {
            // Depth First (we go deeper before going to the next element)
            if(transitionFromCurrentState.getToState().matches(goalState) || findPathImpl(transitionFromCurrentState.getToState(), goalState, path, mapDynamicsMemory)) {
                path.add(transitionFromCurrentState);
                return true;
            }
        }

        return false;
    }
}
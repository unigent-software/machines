package com.unigent.machines.random_finder;

import com.unigent.agentbase.library.core.state.StringPayload;
import com.unigent.agentbase.library.core.state.sensor.DistanceReading;
import com.unigent.agentbase.library.core.state.sensor.XYZOrientationReading;
import com.unigent.agentbase.sdk.action.ActionOfferState;
import com.unigent.agentbase.sdk.action.TaskRequest;
import com.unigent.agentbase.sdk.commons.Config;
import com.unigent.agentbase.sdk.commons.util.geometry.Geometry;
import com.unigent.agentbase.sdk.controller.ConsoleHandle;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.ConsumedDataFlow;
import com.unigent.agentbase.sdk.processing.metadata.DiscreteActionInfo;
import com.unigent.agentbase.sdk.rl.DiscreteAction;
import com.unigent.agentbase.sdk.rl.DiscreteActionImpl;
import com.unigent.agentbase.sdk.state.StateUpdate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import static com.google.common.base.Preconditions.checkState;
import static com.unigent.agentbase.sdk.commons.util.Threads.sleep;
import static java.lang.StrictMath.abs;

/**
 * Random Finder Demo Robot
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = {
                @ConsumedDataFlow(localName = "target_label", dataType = StringPayload.class),
                @ConsumedDataFlow(localName = "scene", dataType = ScenePayload.class),
                @ConsumedDataFlow(localName = "orientation", dataType = XYZOrientationReading.class),
                @ConsumedDataFlow(localName = "sonar", dataType = DistanceReading.class),
        },
        discreteActions = @DiscreteActionInfo(uri = "show_object", description = "Goes to an object labeled by payload in target label topic")
)
public class UserTaskExecutor extends ProcessorBase {

    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    static final URI TARGET_LABEL = URI.create("target_label");
    static final String ACTION_SPACE = "task";
    static final String ACTION_SHOW_OBJECT = "show_object";

    public UserTaskExecutor(String name) {
        super(name);
    }

    private String targetLabel = null;
    private int currentAzimuth = -1;
    private ScenePayload currentScene = null;
    private int currentSonarCm = -1;

    @Override
    public void configure(Map<String, URI> outputBinding, Config config, ConsoleHandle console, NodeServices nodeServices) {
        super.configure(outputBinding, config, console, nodeServices);
        nodeServices.console.registerCommandHandler(new UserTaskConsoleHandler());
    }

    @Override
    public void onStateUpdate(@Nonnull StateUpdate stateUpdate, @Nonnull URI sourceTopic, @Nullable String localBinding) {
        switch(Objects.requireNonNull(localBinding)) {
            case "orientation":
                this.currentAzimuth = (int) Math.round(((XYZOrientationReading) stateUpdate.getPayload()).getX());
                break;

            case "scene":
                this.currentScene = (ScenePayload) stateUpdate.getPayload();
                break;

            case "target_label":
                this.targetLabel = ((StringPayload) stateUpdate.getPayload()).getValue();
                break;

            case "sonar":
                this.currentSonarCm = (int) Math.round(((DistanceReading) stateUpdate.getPayload()).getDistanceMeters());
                break;

            default:
                log.warn("Unknown payload: {}", localBinding);
        }
    }

    @Override
    public boolean executeAction(@Nonnull String actionSpaceUri, @Nonnull DiscreteAction action, @Nonnull String taskId) {
        switch (action.getUri()) {
            case ACTION_SHOW_OBJECT:
                StateUpdate targetLabelState = nodeServices.stateBus.ensureTopic(TARGET_LABEL).getMostRecentData();
                if (targetLabelState == null) {
                    console.inform("Nothing in " + TARGET_LABEL);
                    return false;
                }

                String label = ((StringPayload) targetLabelState.getPayload()).getValue();
                ok("will start looking for " + label);
                return new ShowObjectActionExecutor(this.targetLabel).showObject();

            default:
                sorry("I don't know how to " + action.getUri());
        }
        return false;
    }

    private void ok(String text) {
        console.inform("OK, " + text);
    }

    private void sorry(String text) {
        console.inform("Sorry, " + text);
    }

    private class ShowObjectActionExecutor {

        private static final long TIMEOUT_MILLIS = 10 * 60 * 1000; // 10 min

        private final TaskRequest taskRequest = TaskRequest.multiShotTask(TaskRequest.MEDIUM_URGENCY);
        private final String objectLabel;

        public ShowObjectActionExecutor(String objectLabel) {
            this.objectLabel = objectLabel;
        }

        public boolean showObject() {
            long searchStartedAt = System.currentTimeMillis();
            boolean done = false;
            while(!done) {

                if(System.currentTimeMillis() - searchStartedAt > TIMEOUT_MILLIS) {
                    return false;
                }

                if(!find()) { // Blocks until found
                    sorry("Can't find");
                    return false;
                }
                done = go();
            }
            ok("Done!");
            return true;
        }

        private boolean find() {

            while(!isFound()) {

                // A) Turn around
                checkState(currentAzimuth >=0, "Azimuth is not available");
                int azimuth = currentAzimuth;
                int turnedDegrees = 0;
                while(turnedDegrees < 360) {
                    if(isFound()) {
                        break;
                    }
                    if(!turnRight()) {
                        sorry("Can't turn!");
                        sleep(1000);
                    }
                    sleep(400);
                    turnedDegrees += Geometry.diffAzimuthDegrees(azimuth, currentAzimuth);
                    azimuth = currentAzimuth;
                }

                // B) Wander
                ok("Wandering...");
                if(!wander()) {
                    sorry("Can't wander");
                    return false;
                }
                ok("Wandered!");
            }

            return true;
        }

        private boolean isFound() {
            return currentScene.getObjects().stream().anyMatch(sceneObject -> sceneObject.getLabel().equalsIgnoreCase(this.objectLabel));
        }

        private boolean turnRight() {
            return nodeServices.taskManager.execute("motor", new DiscreteActionImpl("turn_right"), taskRequest) == ActionOfferState.Executed;
        }

        private boolean stepForward() {
            return nodeServices.taskManager.execute("motor", new DiscreteActionImpl("step_forward"), taskRequest) == ActionOfferState.Executed;
        }

        private boolean wander() {
            int azimuth = new Random().nextInt(360);
            if(!turnToAzimuth(azimuth)) {
                return false;
            }

            for(int i=0; i<10; i++) {
                if(!stepForward()) {
                    return false;
                }
            }

            return true;
        }

        private boolean go() {
            while(currentSonarCm > 50) {
                // Make sure we still see the object
                Optional<SceneObject> targetObject = currentScene.getObjects()
                        .stream()
                        .filter(sceneObject -> sceneObject.getLabel().equalsIgnoreCase(this.objectLabel))
                        .findFirst();
                if(targetObject.isEmpty()) {
                    sorry("Lost the object");
                    return false;
                }

                // Adjust our heading
                int objectAzimuth = (int) Math.round(targetObject.get().getAzimuthDegrees());
                if(!turnToAzimuth(objectAzimuth)) {
                    sorry("Can't turn to " + objectAzimuth);
                    return false;
                }

                // Move closer
                ok("Moving closer. Sonar: " + currentSonarCm);
                if(!stepForward()) {
                    sorry("Can't step forward");
                    return false;
                }

                // Give camera a chance to focus to get the new scene
                sleep(400);
            }

            return true;
        }

        private boolean turnToAzimuth(int targetAzimuth) {
            while(true) {
                double diff = Geometry.diffAzimuthDegrees(currentAzimuth, targetAzimuth);
                if(abs(diff) <= 10.0) {
                    return true;
                }

                ok("Azimuth diff: " + Math.round(diff));

                String nudgeAction = diff < 0 ? MotorActions.ACTION_NUDGE_LEFT : MotorActions.ACTION_NUDGE_RIGHT;
                if(nodeServices.taskManager.execute("motor", new DiscreteActionImpl(nudgeAction), taskRequest) != ActionOfferState.Executed) {
                    sorry("Unable to " + nudgeAction);
                    return false;
                }
                sleep(200);
            }
        }
    }

}
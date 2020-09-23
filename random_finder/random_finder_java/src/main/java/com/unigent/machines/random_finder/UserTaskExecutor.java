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
                return new ShowObjectActionExecutor(this.targetLabel, taskId).showObject();

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

        private final TaskRequest taskRequest;
        private final String objectLabel;
        private boolean ran = false;

        public ShowObjectActionExecutor(String objectLabel, String taskId) {
            this.objectLabel = objectLabel;
            this.taskRequest = TaskRequest.continueTask(taskId);
        }

        public synchronized boolean showObject() {

            checkState(!ran, "Already ran");
            ran = true;

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

                ok("Found a " + targetLabel + "! Moving closer...");
                done = go();
            }
            ok("Here it is!");

            // "celebrate"
            for(int i=0; i<3; i++) {
                stepBackward();
                sleep(400);
                stepForward();
            }
            ok("DONE");

            return true;
        }

        private boolean find() {

            while(!isFound()) {

                // A) Turn around
                turnAround();

                if(isFound()) {
                    break;
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

        private void turnAround() {
            checkState(currentAzimuth >=0, "Azimuth is not available");
            int azimuth = currentAzimuth;
            int turnedDegrees = 0;
            while(turnedDegrees < 360) {
                if(isFound()) {
                    return;
                }
                if(!turnRight()) {
                    sorry("Can't turn!");
                    sleep(3000);
                }
                sleep(1200);
                turnedDegrees += Geometry.diffAzimuthDegrees(azimuth, currentAzimuth);
                azimuth = currentAzimuth;
            }
        }

        private boolean isFound() {
            return currentScene != null && currentScene.getObjects().stream().anyMatch(sceneObject -> sceneObject.getLabel().equalsIgnoreCase(this.objectLabel));
        }

        private boolean turnRight() {
            return nodeServices.taskManager.execute("motor", new DiscreteActionImpl("turn_right"), taskRequest) == ActionOfferState.Executed;
        }

        private boolean stepForward() {
            return nodeServices.taskManager.execute("motor", new DiscreteActionImpl("step_forward"), taskRequest) == ActionOfferState.Executed;
        }

        private boolean stepBackward() {
            return nodeServices.taskManager.execute("motor", new DiscreteActionImpl("step_backward"), taskRequest) == ActionOfferState.Executed;
        }

        private boolean wander() {
            for(int attempt = 0; attempt<10; attempt++) {
                int azimuth = new Random().nextInt(360);
                ok("Chose direction " + azimuth + " on attempt " + attempt + " to wander to");
                if(!turnToAzimuth(azimuth)) {
                    continue;
                }

                if(leap()) {
                    return true;
                }

                // re-plan
            }
            return false;
        }

        private boolean leap() {
            for(int i=0; i<6; i++) {
                if(!stepForward()) {
                    ok("Oops!");
                    stepBackward();
                    return false;
                }
            }
            return true;
        }

        private boolean go() {
            while(currentSonarCm > 40) {
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

                String nudgeAction = diff < 0
                        ? (abs(diff) > 20 ? MotorActions.ACTION_TURN_LEFT : MotorActions.ACTION_NUDGE_LEFT)
                        : (abs(diff) > 20 ? MotorActions.ACTION_TURN_RIGHT : MotorActions.ACTION_NUDGE_RIGHT)
                ;
                if(nodeServices.taskManager.execute("motor", new DiscreteActionImpl(nudgeAction), taskRequest) != ActionOfferState.Executed) {
                    sorry("Unable to " + nudgeAction);
                    return false;
                }
                sleep(200);
            }
        }
    }

}
package com.unigent.machines.random_finder;

import com.unigent.agentbase.sdk.action.TaskRequest;
import com.unigent.agentbase.sdk.commons.Config;
import com.unigent.agentbase.sdk.commons.util.Threads;
import com.unigent.agentbase.sdk.controller.ConsoleHandle;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.agentbase.sdk.processing.metadata.DiscreteActionInfo;
import com.unigent.agentbase.sdk.rl.ContinuousAction;
import com.unigent.agentbase.sdk.rl.ContinuousActionImpl;
import com.unigent.agentbase.sdk.rl.DiscreteAction;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Map;

/**
 * Random Finder Demo Robot
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/

@AgentBaseProcessor(
        discreteActions = {
            @DiscreteActionInfo(uri = "stop", description = "Stop"),
            @DiscreteActionInfo(uri = "step_forward", description = "Makes a step forward"),
            @DiscreteActionInfo(uri = "step_backward", description = "Makes a step backward"),
            @DiscreteActionInfo(uri = "turn_left", description = "Turns left"),
            @DiscreteActionInfo(uri = "turn_right", description = "Turns right"),
            @DiscreteActionInfo(uri = "nudge_left", description = "Turns left just a bit"),
            @DiscreteActionInfo(uri = "nudge_right", description = "Turns right just a bit")
        },
        description = "Provides a basic set of motor actions"
)
public class MotorActions extends ProcessorBase implements Constants {

    public static final String ACTION_STOP = "stop";
    public static final String ACTION_STEP_FORWARD = "step_forward";
    public static final String ACTION_STEP_BACKWARD = "step_backward";
    public static final String ACTION_TURN_LEFT = "turn_left";
    public static final String ACTION_TURN_RIGHT = "turn_right";
    public static final String ACTION_NUDGE_LEFT = "nudge_left";
    public static final String ACTION_NUDGE_RIGHT = "nudge_right";

    private static final ContinuousAction STOP = new ContinuousActionImpl(0.0, 0.0);
    private static final long STEP_TIME = 200;
    private static final long NUDGE_TIME = 150;

    public MotorActions(String name) {
        super(name);
    }

    @Override
    public void configure(Map<String, URI> outputBinding, Config config, ConsoleHandle console, NodeServices nodeServices) {
        super.configure(outputBinding, config, console, nodeServices);
        nodeServices.console.registerCommandHandler(new MotorActionsConsoleHandler());
    }

    @Override
    public boolean executeAction(@Nonnull String actionSpaceUri, @Nonnull DiscreteAction discreteAction, @Nonnull String taskId) {
        TaskRequest task = TaskRequest.continueTask(taskId);
        switch(discreteAction.getUri()) {
            case ACTION_STOP:
                this.nodeServices.taskManager.execute(AS_ACTUATOR, STOP, task);
                break;

            case ACTION_STEP_FORWARD:
                this.nodeServices.taskManager.execute(AS_ACTUATOR, new ContinuousActionImpl(1.0, 1.0), task);
                Threads.sleep(STEP_TIME);
                this.nodeServices.taskManager.execute(AS_ACTUATOR, STOP, task);
                break;

            case ACTION_STEP_BACKWARD:
                this.nodeServices.taskManager.execute(AS_ACTUATOR, new ContinuousActionImpl(-1.0, -1.0), task);
                Threads.sleep(STEP_TIME);
                this.nodeServices.taskManager.execute(AS_ACTUATOR, STOP, task);
                break;

            case ACTION_TURN_LEFT:
                this.nodeServices.taskManager.execute(AS_ACTUATOR, new ContinuousActionImpl(0, 1.0), task);
                Threads.sleep(STEP_TIME);
                this.nodeServices.taskManager.execute(AS_ACTUATOR, STOP, task);
                break;

            case ACTION_TURN_RIGHT:
                this.nodeServices.taskManager.execute(AS_ACTUATOR, new ContinuousActionImpl(1.0, 0), task);
                Threads.sleep(STEP_TIME);
                this.nodeServices.taskManager.execute(AS_ACTUATOR, STOP, task);
                break;

            case ACTION_NUDGE_LEFT:
                this.nodeServices.taskManager.execute(AS_ACTUATOR, new ContinuousActionImpl(0, 0.7), task);
                Threads.sleep(NUDGE_TIME);
                this.nodeServices.taskManager.execute(AS_ACTUATOR, STOP, task);
                break;

            case ACTION_NUDGE_RIGHT:
                this.nodeServices.taskManager.execute(AS_ACTUATOR, new ContinuousActionImpl(0.7, 0), task);
                Threads.sleep(NUDGE_TIME);
                this.nodeServices.taskManager.execute(AS_ACTUATOR, STOP, task);
                break;

        }

        return true;
    }

}

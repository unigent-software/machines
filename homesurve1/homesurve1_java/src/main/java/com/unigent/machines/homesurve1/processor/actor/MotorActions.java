package com.unigent.machines.homesurve1.processor.actor;

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

@AgentBaseProcessor(
        discreteActions = {
            @DiscreteActionInfo(uri = "stop", description = "Stop"),
            @DiscreteActionInfo(uri = "step_forward", description = "Makes a step forward"),
            @DiscreteActionInfo(uri = "step_backward", description = "Makes a step backward"),
            @DiscreteActionInfo(uri = "turn_left", description = "Turns left"),
            @DiscreteActionInfo(uri = "turn_right", description = "Turns right")
        },
        description = "Provides a basic set of motor actions"
)
public class MotorActions extends ProcessorBase {

    private static final String ACTUATOR = "actuator";
    private static final ContinuousAction STOP = new ContinuousActionImpl(0.0, 0.0);
    private static final long STEP_TIME = 500;

    public MotorActions(String name) {
        super(name);
    }

    @Override
    public void configure(Map<String, URI> outputBinding, Config config, ConsoleHandle console, NodeServices nodeServices) {
        super.configure(outputBinding, config, console, nodeServices);
        nodeServices.console.registerCommandHandler(new ManualControlConsoleHandler());
    }

    @Override
    public boolean executeAction(@Nonnull String actionSpaceUri, @Nonnull DiscreteAction discreteAction, @Nonnull String taskId) {
        TaskRequest task = new TaskRequest(taskId, TaskRequest.MEDIUM_URGENCY, false);
        switch(discreteAction.getUri()) {
            case "stop":
                task.endOfTask();
                this.nodeServices.taskManager.execute(ACTUATOR, STOP, task);
                break;

            case "step_forward":
                this.nodeServices.taskManager.execute(ACTUATOR, new ContinuousActionImpl(1.0, 1.0), task);
                Threads.sleep(STEP_TIME, true);
                task.endOfTask();
                this.nodeServices.taskManager.execute(ACTUATOR, STOP, task);
                break;

            case "step_backward":
                this.nodeServices.taskManager.execute(ACTUATOR, new ContinuousActionImpl(-1.0, -1.0), task);
                Threads.sleep(STEP_TIME, true);
                task.endOfTask();
                this.nodeServices.taskManager.execute(ACTUATOR, STOP, task);
                break;

            case "turn_left":
                this.nodeServices.taskManager.execute(ACTUATOR, new ContinuousActionImpl(-0.5, 1.0), task);
                Threads.sleep(STEP_TIME, true);
                task.endOfTask();
                this.nodeServices.taskManager.execute(ACTUATOR, STOP, task);
                break;

            case "turn_right":
                this.nodeServices.taskManager.execute(ACTUATOR, new ContinuousActionImpl(1.0, -0.5), task);
                Threads.sleep(STEP_TIME, true);
                task.endOfTask();
                this.nodeServices.taskManager.execute(ACTUATOR, STOP, task);
                break;
        }

        return true;
    }
}

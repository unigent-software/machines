package com.unigent.machines.random_finder;

import com.unigent.agentbase.sdk.action.TaskManager;
import com.unigent.agentbase.sdk.action.TaskRequest;
import com.unigent.agentbase.sdk.node.ConsoleCommandHandlerBase;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.rl.DiscreteActionImpl;

import java.util.Collections;
import java.util.List;

/**
 * Provides console commands for manual robot control
 *
 * Random Finder Demo Robot
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/

public class MotorActionsConsoleHandler extends ConsoleCommandHandlerBase {

    private static final String ACTIONS_SPACE_MOTOR = "motor";

    private static final long TASK_TIMEOUT_MILLIS = 3000;

    private static final String CMD_STEP_FORWARD = "step_forward";
    private static final String CMD_STEP_BACKWARD = "step_backward";
    private static final String CMD_TURN_LEFT = "turn_left";
    private static final String CMD_TURN_RIGHT = "turn_right";

    private static final List<String> ALL_COMMANDS = List.of(CMD_STEP_FORWARD, CMD_STEP_BACKWARD, CMD_TURN_LEFT, CMD_TURN_RIGHT);

    @Override
    public String help() {
        return "Manual control for robot movements";
    }

    @Override
    public List<String> getFirstTokens() {
        return Collections.singletonList("motor");
    }

    @Override
    public boolean handle(List<String> tokens) {

        if(tokens.size() < 2) {
            console.inform("Specify one of " + ALL_COMMANDS);
            return true;
        }

        TaskManager taskManager = console.getNodeServices().taskManager;
        TaskRequest taskRequest = TaskRequest.newTask(TaskRequest.MEDIUM_URGENCY, TASK_TIMEOUT_MILLIS);
        switch (tokens.get(1)) {
            case CMD_STEP_FORWARD:
            case "sf":
                taskManager.execute(ACTIONS_SPACE_MOTOR, new DiscreteActionImpl(CMD_STEP_FORWARD), taskRequest);
                return true;
            case CMD_STEP_BACKWARD:
            case "sb":
                taskManager.execute(ACTIONS_SPACE_MOTOR, new DiscreteActionImpl(CMD_STEP_BACKWARD), taskRequest);
                return true;
            case CMD_TURN_LEFT:
            case "tl":
                taskManager.execute(ACTIONS_SPACE_MOTOR, new DiscreteActionImpl(CMD_TURN_LEFT), taskRequest);
                return true;
            case CMD_TURN_RIGHT:
            case "tr":
                taskManager.execute(ACTIONS_SPACE_MOTOR, new DiscreteActionImpl(CMD_TURN_RIGHT), taskRequest);
                return true;
        }

        return false;
    }
}

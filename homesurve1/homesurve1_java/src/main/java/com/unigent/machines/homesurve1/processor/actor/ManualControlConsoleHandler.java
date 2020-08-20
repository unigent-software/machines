package com.unigent.machines.homesurve1.processor.actor;

import com.unigent.agentbase.sdk.action.TaskRequest;
import com.unigent.agentbase.sdk.node.ConsoleCommandHandler;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.rl.ContinuousActionImpl;
import com.unigent.agentbase.sdk.rl.DiscreteActionImpl;

import java.util.List;

/**
 * Provides console commands for manual robot control
 */
public class ManualControlConsoleHandler implements ConsoleCommandHandler {

    private static final String ACTIONS_SPACE_MOTOR = "motor";

    private static final String CMD_STEP_FORWARD = "step_forward";
    private static final String CMD_STEP_BACKWARD = "step_backward";
    private static final String CMD_TURN_LEFT = "turn_left";
    private static final String CMD_TURN_RIGHT = "turn_right";

    private static final List<String> ALL_COMMANDS = List.of(CMD_STEP_FORWARD, CMD_STEP_BACKWARD, CMD_TURN_LEFT, CMD_TURN_RIGHT);

    private NodeServices nodeServices;

    @Override
    public void configure(NodeServices nodeServices) {
        this.nodeServices = nodeServices;
    }

    @Override
    public String help() {
        return "Manual control for robot movements";
    }

    @Override
    public List<String> getFirstTokens() {
        return ALL_COMMANDS;
    }

    @Override
    public boolean handle(List<String> tokens) {
        switch (tokens.get(0)) {
            case CMD_STEP_FORWARD:
                nodeServices.taskManager.execute(ACTIONS_SPACE_MOTOR, new DiscreteActionImpl(CMD_STEP_FORWARD), TaskRequest.singleShotTask());
                return true;
            case CMD_STEP_BACKWARD:
                nodeServices.taskManager.execute(ACTIONS_SPACE_MOTOR, new DiscreteActionImpl(CMD_STEP_BACKWARD), TaskRequest.singleShotTask());
                return true;
            case CMD_TURN_LEFT:
                nodeServices.taskManager.execute(ACTIONS_SPACE_MOTOR, new DiscreteActionImpl(CMD_TURN_LEFT), TaskRequest.singleShotTask());
                return true;
            case CMD_TURN_RIGHT:
                nodeServices.taskManager.execute(ACTIONS_SPACE_MOTOR, new DiscreteActionImpl(CMD_TURN_RIGHT), TaskRequest.singleShotTask());
                return true;
        }

        return false;
    }
}

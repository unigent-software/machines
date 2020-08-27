package com.unigent.machines.homesurve1.processor.controller;

import com.unigent.agentbase.library.core.state.StringPayload;
import com.unigent.agentbase.sdk.action.ActionOfferState;
import com.unigent.agentbase.sdk.action.TaskRequest;
import com.unigent.agentbase.sdk.node.ConsoleCommandHandlerBase;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.rl.DiscreteActionImpl;
import com.unigent.agentbase.sdk.state.StateUpdate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
**/
public class UserTaskConsoleHandler extends ConsoleCommandHandlerBase {

    private final ExecutorService taskRunner = Executors.newCachedThreadPool();

    @Override
    public String help() {
        return "Listens for user commands";
    }

    @Override
    public List<String> getFirstTokens() {
        return Collections.singletonList("go");
    }

    @Override
    public boolean handle(List<String> tokens) throws Exception {
        if(tokens.size() < 2) {
            console.inform("Need more arguments: go <label>");
            return true;
        }

        String label = tokens.get(1);
        NodeServices nodeServices = console.getNodeServices();

        // 1. Put the label onto the target label topic
        nodeServices.stateBus.publish(UserTaskExecutor.TARGET_LABEL, new StateUpdate(new StringPayload(label), StateUpdate.Origin.Manager));

        // 2. Start the "go" action in a separate thread to free the console
        this.taskRunner.submit(()->{
            ActionOfferState offerState = nodeServices.taskManager.execute(
                    UserTaskExecutor.ACTION_SPACE,
                    new DiscreteActionImpl(UserTaskExecutor.ACTION_GO),
                    TaskRequest.singleShotTask()
            );
            console.inform("Completed. Status: " + offerState);
        });

        return true;
    }
}

package com.unigent.machines.random_finder;

import com.google.common.base.Joiner;
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
 * Random Finder Demo Robot
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class UserTaskConsoleHandler extends ConsoleCommandHandlerBase {

    private static final String DO = "do";
    private static final String SHOW = "show";
    private static final String CANCEL = "cancel";

    private ExecutorService taskRunner;

    private String currentTaskId;

    @Override
    public String help() {
        return "Listens for user commands";
    }

    @Override
    public List<String> getFirstTokens() {
        return Collections.singletonList(DO);
    }

    @Override
    public boolean handle(List<String> tokens) throws Exception {

        NodeServices nodeServices = console.getNodeServices();
        if(tokens.size() < 2) {
            console.inform("Need more arguments: " + DO + " " + SHOW + "/" + CANCEL);
            return true;
        }

        switch(tokens.get(1)) {
            case SHOW:

                if(currentTaskId != null) {
                    console.inform("Sorry, already busy running task " + currentTaskId);
                    return true;
                }

                if(tokens.size() < 3) {
                    console.inform("Need more arguments: " + DO + " " + SHOW + " <label>");
                    return true;
                }

                String label = Joiner.on(' ').join(tokens.subList(2, tokens.size()));

                // 1. Put the label onto the target label topic
                nodeServices.stateBus.publish(UserTaskExecutor.TARGET_LABEL, new StateUpdate(new StringPayload(label), StateUpdate.Origin.Manager));

                // 2. Start the "go" action in a separate thread to free the console
                this.taskRunner.submit(()->{
                    TaskRequest taskRequest = TaskRequest.newTask();
                    this.currentTaskId = taskRequest.getTaskId();
                    console.inform("Starting task " + currentTaskId);
                    ActionOfferState offerState = nodeServices.taskManager.execute(
                            UserTaskExecutor.ACTION_SPACE,
                            new DiscreteActionImpl(UserTaskExecutor.ACTION_SHOW_OBJECT),
                            taskRequest
                    );
                    console.inform(SHOW + " completed. Status: " + offerState);
                });
                break;

            case CANCEL:
                if(currentTaskId == null) {
                    console.inform("I'm not running any tasks");
                    return true;
                }

                console.inform("Cancelling task " + currentTaskId);
                boolean cancelled = nodeServices.taskManager.cancelTask(currentTaskId);
                console.inform("Cancel success: " + cancelled);
                this.currentTaskId = null;
                break;

            default:
                return false;
        }
        return true;

    }

    @Override
    public void initiate() {
        super.initiate();
        this.taskRunner = Executors.newCachedThreadPool();
    }

    @Override
    public void shutdown() {
        this.taskRunner.shutdownNow();
        this.taskRunner = null;
        super.shutdown();
    }
}

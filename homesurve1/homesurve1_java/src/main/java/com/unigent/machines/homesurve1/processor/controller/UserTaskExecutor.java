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
import com.unigent.machines.homesurve1.processor.objectmemory.ObjectMemory;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor(
        consumedData = @ConsumedDataFlow(localName = "target_label", dataType = StringPayload.class, receiveUpdates = false),
        discreteActions = @DiscreteActionInfo(uri = "go", description = "Goes to an object labeled by payload in target label topic")
)
public class UserTaskExecutor extends ProcessorBase {

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
        StateUpdate targetLabelState = nodeServices.stateBus.ensureTopic(TARGET_LABEL).getMostRecentData();
        if(targetLabelState == null) {
            console.inform("Nothing in " + TARGET_LABEL);
            return false;
        }

        String label = ((StringPayload)targetLabelState.getPayload()).getValue();
        console.inform("Will start looking for " + label);

        return runAction(label);
    }

    private boolean runAction(String label) {

        ObjectMemory objectMemory = InitializerProcessor.getInstance().getObjectMemory();
        List<String> objectIds = objectMemory.findObjectsByLabel(label);
        if(objectIds.isEmpty()) {
            console.inform("I've never seen a " + label);
            return true;
        }

        console.inform("I've seen " + objectIds.size() + " " + label + "s");
        return true;
    }
}

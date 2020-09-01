package com.unigent.machines.homesurve1;

import com.unigent.agentbase.sdk.commons.Config;
import com.unigent.agentbase.sdk.controller.ConsoleHandle;
import com.unigent.agentbase.sdk.node.NodeServices;
import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import com.unigent.machines.homesurve1.processor.dynamics.MapDynamicsCollector;
import com.unigent.machines.homesurve1.processor.dynamics.MapDynamicsMemory;
import com.unigent.machines.homesurve1.processor.objectmemory.ObjectMemory;
import com.unigent.machines.homesurve1.processor.objectmemory.ObjectMemoryConsoleHandler;

import java.net.URI;
import java.util.Map;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor
public class InitializerProcessor extends ProcessorBase {

    private static InitializerProcessor instance;

    private MapDynamicsMemory mapDynamicsMemory;
    private ObjectMemory objectMemory;

    public InitializerProcessor(String name) {
        super(name);
        instance = this;
    }

    public static synchronized InitializerProcessor getInstance() {
        return instance;
    }

    @Override
    public void initiate() {
        super.initiate();
        this.objectMemory.initiate();
        this.mapDynamicsMemory.initiate();
    }

    @Override
    public void shutdown() {
        this.mapDynamicsMemory.shutdown();
        this.objectMemory.shutdown();
        super.shutdown();
    }

    public ObjectMemory getObjectMemory() {
        return objectMemory;
    }

    public MapDynamicsMemory getMapDynamicsMemory() {
        return mapDynamicsMemory;
    }

    @Override
    public void configure(Map<String, URI> outputBinding, Config config, ConsoleHandle console, NodeServices nodeServices) {
        super.configure(outputBinding, config, console, nodeServices);
        this.objectMemory = new ObjectMemory(nodeServices.nitriteManager);
        this.mapDynamicsMemory = new MapDynamicsMemory(nodeServices.nitriteManager);
        nodeServices.console.registerCommandHandler(new ObjectMemoryConsoleHandler(this.objectMemory));
    }

}

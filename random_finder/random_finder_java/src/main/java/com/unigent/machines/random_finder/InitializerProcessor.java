package com.unigent.machines.random_finder;

import com.unigent.agentbase.sdk.processing.ProcessorBase;
import com.unigent.agentbase.sdk.processing.metadata.AgentBaseProcessor;
import org.opencv.core.Core;

/**
 * Random Finder Demo Robot
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
@AgentBaseProcessor
public class InitializerProcessor extends ProcessorBase {

    private static InitializerProcessor instance;

    public InitializerProcessor(String name) {
        super(name);
        instance = this;
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static synchronized InitializerProcessor getInstance() {
        return instance;
    }

}

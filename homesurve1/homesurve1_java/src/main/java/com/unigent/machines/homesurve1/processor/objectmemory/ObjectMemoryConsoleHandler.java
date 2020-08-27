package com.unigent.machines.homesurve1.processor.objectmemory;

import com.google.common.base.Joiner;
import com.unigent.agentbase.sdk.node.ConsoleCommandHandlerBase;
import com.unigent.machines.homesurve1.processor.dynamics.MapDynamicsCollector;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class ObjectMemoryConsoleHandler extends ConsoleCommandHandlerBase {

    final static String DUMP_NAME = "object_memory.json";

    private final ObjectMemory objectMemory;

    public ObjectMemoryConsoleHandler(ObjectMemory objectMemory) {
        this.objectMemory = objectMemory;
    }

    @Override
    public String help() {
        return "Dumps collected dynamics to " + DUMP_NAME + " under node data directory";
    }

    @Override
    public List<String> getFirstTokens() {
        return List.of("objects");
    }

    @Override
    public boolean handle(List<String> tokens) throws Exception {
        if(tokens.size() < 2) {
            console.inform("Need more arguments (dump, size, list)");
            return true;
        }

        String subCommand = tokens.get(1);
        switch(subCommand) {
            case "dump":
                File dump = new File(console.getNodeServices().nodeDataDir, DUMP_NAME);
                try (PrintWriter writer = new PrintWriter(new FileWriter(dump))) {
                    int cnt = objectMemory.dump(writer);
                    console.inform("Dumped " + cnt + " items to " + dump.getCanonicalPath());
                }
                break;

            case "size":
                console.inform("So far " + objectMemory.size());
                break;

            case "list":
                console.inform("All objects:\n" + Joiner.on('\n').join(objectMemory.findObjects()));
                break;

            default:
                console.inform("Unknown sub command " + subCommand);
        }
        return true;
    }
}

package com.unigent.machines.homesurve1.processor.dynamics;

import com.unigent.agentbase.sdk.node.ConsoleCommandHandlerBase;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapDynamicsConsoleHandler extends ConsoleCommandHandlerBase {

    final static String DUMP_NAME = "map_dynamics.json";

    @Override
    public String help() {
        return "Dumps collected dynamics to " + DUMP_NAME + " under node data directory";
    }

    @Override
    public List<String> getFirstTokens() {
        return List.of("dynamics");
    }

    @Override
    public boolean handle(List<String> tokens) throws Exception {
        if(tokens.size() < 2) {
            console.inform("Need more arguments (dump, clear, size)");
            return true;
        }

        String subCommand = tokens.get(1);
        switch(subCommand) {
            case "dump":
                File dump = new File(console.getNodeServices().nodeDataDir, DUMP_NAME);
                try (PrintWriter writer = new PrintWriter(new FileWriter(dump))) {
                    int cnt = MapDynamicsCollector.instance.dump(writer);
                    console.inform("Dumped " + cnt + " items to " + dump.getCanonicalPath());
                }
                break;

            case "clear":
                MapDynamicsCollector.instance.clearBuffer();
                console.inform("Cleared!");
                break;

            case "size":
                console.inform("So far " + MapDynamicsCollector.instance.getBufferSize());
                break;

            default:
                console.inform("Unknown sub command " + subCommand);
        }
        return true;
    }
}

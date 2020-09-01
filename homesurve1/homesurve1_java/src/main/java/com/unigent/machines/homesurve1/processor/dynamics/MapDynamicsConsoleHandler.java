package com.unigent.machines.homesurve1.processor.dynamics;

import com.unigent.agentbase.sdk.node.ConsoleCommandHandlerBase;
import com.unigent.machines.homesurve1.InitializerProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Home Surveillance Robot, POC 1
 * Unigent Robotics, 2020
 * <a href="http://unigent.com">Unigent</a>
 **/
public class MapDynamicsConsoleHandler extends ConsoleCommandHandlerBase {

    final static String DUMP_FULL_NAME = "map_dynamics_full.json";
    final static String DUMP_SIMPLE_NAME = "map_dynamics_simple.txt";

    @Override
    public String help() {
        return "Dumps collected dynamics to under node data directory";
    }

    @Override
    public List<String> getFirstTokens() {
        return List.of("dynamics");
    }

    @Override
    public boolean handle(List<String> tokens) throws Exception {
        if(tokens.size() < 2) {
            console.inform("Need more arguments (dump_json, dump_simple, clear, size)");
            return true;
        }

        MapDynamicsMemory mapDynamicsMemory = InitializerProcessor.getInstance().getMapDynamicsMemory();
        String subCommand = tokens.get(1);
        switch(subCommand) {

            case "dump_simple":
                File simple_dump = new File(console.getNodeServices().nodeDataDir, DUMP_SIMPLE_NAME);
                try (PrintWriter writer = new PrintWriter(new FileWriter(simple_dump))) {
                    mapDynamicsMemory.findAll().forEach(stateTransition -> writer.println(stateTransition.toSimpleString()));
                    console.inform("Dumped " + simple_dump.getCanonicalPath());
                }
                break;

            case "dump_full":
                File full_dump = new File(console.getNodeServices().nodeDataDir, DUMP_FULL_NAME);
                try (PrintWriter writer = new PrintWriter(new FileWriter(full_dump))) {
                    int cnt = mapDynamicsMemory.dump(writer);
                    console.inform("Dumped " + cnt + " items to " + full_dump.getCanonicalPath());
                }
                break;

            case "clear":
                mapDynamicsMemory.clear();
                console.inform("Cleared!");
                break;

            case "size":
                console.inform("So far " + mapDynamicsMemory.size());
                break;

            default:
                console.inform("Unknown sub command " + subCommand);
        }
        return true;
    }
}

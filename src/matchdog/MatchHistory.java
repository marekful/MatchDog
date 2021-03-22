package matchdog;

import jcons.src.com.meyling.console.UnixConsole;
import matchdog.console.printer.DefaultPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MatchHistory {

    private static String dataDir;

    private final Match match;
    private final Map<Integer, ArrayList<String>> commands;

    DefaultPrinter printer;

    MatchHistory(Match match) {

        if (match.getGameno() != 1) {
            throw new IllegalArgumentException("Game number must be one");
        }

        dataDir = match.server.getDataDir();
        commands = new HashMap<>();

        printer = new DefaultPrinter(
            match.server, "MatchHistory", UnixConsole.LIGHT_RED, UnixConsole.BACKGROUND_WHITE
        );

        match.server.addPrinter(printer);

        this.match = match;
        initialCommands();
    }

    private void initialCommands() {
        addCommand("set default " + match.getPlayer0() + " " + match.getPlayer1());
        addCommand("set player 0 human");
        addCommand("set player 1 human");
        addCommand("set automatic game off");
        addCommand("set automatic roll off");
        addCommand("set automatic move off");
        addCommand("new match " + match.getMl());
    }

    public void addCommand(String command) {

        if (match.wasResumed()) {
            return; // not yet handled for dropped/resumed matches
        }

        commands.computeIfAbsent(match.getGameno(), k -> new ArrayList<>());
        commands.get(match.getGameno()).add(command);

        printer.printLine("Command added: " + command);
    }

    public void setTurn() {
        addCommand("set turn " + match.getPlayerOnTurn());
    }

    public void writeToFile() throws IOException {

        FileWriter writer = new FileWriter(dataDir + "matchlogs/" + match.id + ".txt");
        for (int gameNo : commands.keySet()) {
            for(String str : commands.get(gameNo)) {
                writer.append(str.replace("-", "/")
                        .replace("/bar", "/off")).append(System.lineSeparator());
            }
            writer.append(System.lineSeparator());
            writer.append(System.lineSeparator());
        }
        writer.append(System.lineSeparator())
                .append("analyse match")
                .append(System.lineSeparator())
                .append("relational add match")
                .append(System.lineSeparator())
                .append("save match \"").append(dataDir).append("matchlogs/").append(match.id).append(".sgf\"");

        writer.close();
    }
}

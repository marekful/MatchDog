package matchdog;

import jcons.src.com.meyling.console.UnixConsole;
import matchdog.console.printer.DefaultPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MatchHistory {

    public final static int WRITE_FILE_AT_MATCH_END = 0;
    public final static int WRITE_FILE_PER_LINE = 1;

    private static String configDir;
    private static String dataDir;

    private final int writeStrategy;
    private final Match match;
    private final Map<Integer, ArrayList<String>> commands;

    private FileWriter writer;

    DefaultPrinter printer;

    MatchHistory(Match match, int writeStrategy) {

        if (match.getGameno() != 1) {
            throw new IllegalArgumentException("Game number must be one");
        }

        dataDir = match.server.getDataDir();
        configDir = match.server.getConfigDir();
        this.writeStrategy = writeStrategy;

        commands = new HashMap<>();

        printer = new DefaultPrinter(
            match.server, "MatchHistory", UnixConsole.LIGHT_RED, UnixConsole.BACKGROUND_WHITE
        );

        match.server.addPrinter(printer);

        this.match = match;
        initialCommands();
    }

    private FileWriter getWriter() {
        if (writer == null) {
            try {
                writer = new FileWriter(getFileName() + ".txt");
            } catch (IOException e) {
                printer.printLine("Cannot open " + getFileName() + " for writing");
                return null;
            }
        }
        return writer;
    }

    private void initialCommands() {
        addCommand("set default " + match.getPlayer0() + " " + match.getPlayer1());
        addCommand("set player 0 human");
        addCommand("set player 1 human");
        addCommand("set automatic game off");
        addCommand("set automatic roll off");
        addCommand("set automatic move off\n");
        addCommand("new match " + match.getMl() + "\n");
    }

    private void writeOneLineToFile(String line) {
        if ((writer = getWriter()) == null) {
            return;
        }

        try {
            writer.append(line).append(System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int getWriteStrategy() {
        return writeStrategy;
    }

    public void addCommand(String command) {

        if (match.wasResumed()) {
            return; // not yet handled for dropped/resumed matches
        }

        commands.computeIfAbsent(match.getGameno(), k -> new ArrayList<>());
        commands.get(match.getGameno()).add(command);

        if (writeStrategy == WRITE_FILE_PER_LINE) {
            writeOneLineToFile(command);
        }

        printer.printLine("Command: " + command);
    }

    public void setTurn() {
        addCommand("set turn " + match.getPlayerOnTurn());
    }

    public void writeToFile() throws IOException {

        if ((writer = getWriter()) == null) {
            printer.printLine("Not writing file");
            return;
        }

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
                .append("save match \"").append(getFileName()).append(".sgf\"");
    }

    public void exportMatchToSgf() {

        (new Thread(() -> {
            String[] fixedArgs = {"-t", "-q", "-s", configDir + "gnubg", "-D", dataDir + "gnubg"};
            BGRunner analRunner = new BGRunner(match.server, match.server.programPrefs.getGnubgCmdArr(), fixedArgs);

            // export-from-file
            if (!analRunner.startGnubg("-c \"" + getFileName() + ".txt\"", true)) {
                printer.printLine("ERROR: Could not export sgf");
                return;
            }
            printer.printLine("Exported .sgf file");
        })).start();
    }

    public String getFileName() {
        return  dataDir + "matchlogs/" + match.id;
    }

    public void onMatchEnd() {

        if (!match.wasResumed && (writer = getWriter()) != null) {
            try {
                if (writeStrategy == WRITE_FILE_PER_LINE) {
                    writer.append(System.lineSeparator())
                            .append("analyse match")
                            .append(System.lineSeparator())
                            .append("relational add match")
                            .append(System.lineSeparator())
                            .append("save match \"").append(getFileName()).append(".sgf\"");
                } else if (writeStrategy == WRITE_FILE_AT_MATCH_END) {
                    writeToFile();
                }
                writer.flush();
                writer.close();

                exportMatchToSgf();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        match.server.removePrinter(printer);
    }
}

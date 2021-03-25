package matchdog;

import jcons.src.com.meyling.console.UnixConsole;
import matchdog.console.printer.DefaultPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MatchHistory {

    public final static int WRITE_FILE_AT_MATCH_END = 0;
    public final static int WRITE_FILE_PER_LINE = 1;

    private static String configDir;
    private static String dataDir;

    private final int writeStrategy;
    private final Match match;
    private final Map<Integer, ArrayList<String>> commands;

    private final ArrayList<String> commandsSinceLastId;

    private FileWriter writer;
    private FileWriter tmpWriter;
    private FileWriter posWriter;

    String rand, posRand;

    DefaultPrinter printer;

    MatchHistory(Match match, int writeStrategy) {

        if (match.getGameno() != 1) {
            throw new IllegalArgumentException("Game number must be one");
        }

        dataDir = match.server.getDataDir();
        configDir = match.server.getConfigDir();
        this.writeStrategy = writeStrategy;

        commands = new HashMap<>();
        commandsSinceLastId = new ArrayList<>();

        printer = new DefaultPrinter(
            match.server, "MatchHistory", UnixConsole.LIGHT_RED, UnixConsole.BACKGROUND_WHITE
        );

        match.server.addPrinter(printer);

        this.match = match;
        addInitialCommands();
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

    private void deleteTempFile() {
        File o = new File(getTempFileName() + ".tmp");
        o.delete();
    }

    private void deletePosFile() {
        File o = new File(getPosFileName() + ".pos.tmp");
        o.delete();
    }

    private FileWriter getTmpWriter() {
        if (tmpWriter == null) {
            try {
                deleteTempFile();
                rand = String.format("%.0f", Double.parseDouble(String.valueOf(Math.random())) * 10000);

                File f = new File(getTempFileName() + ".tmp");
                tmpWriter = new FileWriter(f);
            } catch (IOException e) {
                printer.printLine("Cannot open " + getTempFileName() + ".tmp for writing");
                return null;
            }
        }
        return tmpWriter;
    }

    private FileWriter getPositionWriter() {
        if (posWriter == null) {
            try {
                deletePosFile();
                posRand = String.format("%.0f", Double.parseDouble(String.valueOf(Math.random())) * 10000);

                File f = new File(getPosFileName() + ".pos.tmp");
                posWriter = new FileWriter(f);
            } catch (IOException e) {
                printer.printLine("Cannot open " + getPosFileName() + ".pos.tmp for writing");
                return null;
            }
        }
        return posWriter;
    }

    private ArrayList<String> getInitialCommands() {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("set default " + match.getPlayer0() + " " + match.getPlayer1());
        commands.add("set player 0 human");
        commands.add("set player 1 human");
        commands.add("set automatic game off");
        commands.add("set automatic roll off");
        commands.add("set automatic move off\n");
        commands.add("new match " + match.getMl() + "\n");
        return commands;
    }

    private void addInitialCommands() {

        for (String command : getInitialCommands()) {
            addCommand(command);
        }
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

    public void clearCommandsSinceLastId() {
        commandsSinceLastId.clear();
    }

    public void addCommandSinceLastId(String command) {
        commandsSinceLastId.add(command);
    }

    public int getWriteStrategy() {
        return writeStrategy;
    }

    public void addCommand(String command) {

        addCommandSinceLastId(command);

        if (match.wasResumed()) {
            return; // not yet handled for dropped/resumed matches
        }

        commands.computeIfAbsent(match.getGameno(), k -> new ArrayList<>());
        commands.get(match.getGameno()).add(command);

        if (writeStrategy == WRITE_FILE_PER_LINE) {
            writeOneLineToFile(command);
        }

        printer.printLine("Command: " + command);

        //match.sendCommand(command);
    }

    public void setTurn() {
        addCommand("set turn " + match.getPlayerOnTurn());
    }

    public void writeToFile(FileWriter writer, boolean addFinalCommands, boolean addHint) throws IOException {


        for (int gameNo : commands.keySet()) {
            for(String str : commands.get(gameNo)) {
                writer.append(str.replace("-", "/")
                        .replace("/bar", "/off")).append(System.lineSeparator());
            }
            writer.append(System.lineSeparator());
            writer.append(System.lineSeparator());
        }

        if (addFinalCommands) {
            writer.append(System.lineSeparator())
                    .append("analyse match")
                    .append(System.lineSeparator())
                    .append("relational add match")
                    .append(System.lineSeparator())
                    .append("save match \"").append(getFileName()).append(".sgf\"");
        }

        if (addHint) {
            writer.append("show board").append(System.lineSeparator()).append(System.lineSeparator());
            writer.append("hint").append(System.lineSeparator()).append(System.lineSeparator());
        }
    }

    public void writeToTempFile() throws IOException {
        writeToFile(getTmpWriter(), false, true);
        Objects.requireNonNull(getTmpWriter()).flush();
        tmpWriter = null;
    }

    public void writePositionToFile() throws IOException {
        FileWriter writer;
        if ((writer = getPositionWriter()) == null) {
            return;
        }

        for (String command : getInitialCommands()) {
            writer.append(command).append(System.lineSeparator());
        }

        if (((MatchEx)match).getGnubgMatchId() != null) {
            writer.append(System.lineSeparator())
                    .append("set gnubgid ").append(((MatchEx) match).getGnubgId())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }

        for (String s : commandsSinceLastId) {
            writer.append(s).append(System.lineSeparator());
        }

        writer.append(System.lineSeparator())
                .append("show board").append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("hint").append(System.lineSeparator())
                .append(System.lineSeparator());

        writer.flush();
        posWriter = null;
    }

    public void exportMatchToSgf() {

        (new Thread(() -> {
            printer.printLine("Starting .sgf export");
            String[] fixedArgs = {"-t", "-q", "-s", configDir + "gnubg", "-D", dataDir + "gnubg"};
            Gnubg analRunner = new GnubgAnalysis(match.server, match.server.programPrefs.getGnubgCmdArr(), fixedArgs, true);

            // export-from-file
            if (!analRunner.start("-c \"" + getFileName() + ".txt\"", true)) {
                printer.printLine("ERROR: Could not export sgf");
                return;
            }
            printer.printLine("Exported " + getFileName() + ".sgf");
        })).start();
    }

    public String getFileName() {
        return  dataDir + "matchlogs/" + match.id;
    }

    public String getTempFileName() {
        return dataDir + "matchlogs/" + match.id + "-" + rand;
    }

    public String getPosFileName() {
        return dataDir + "matchlogs/" + match.id + "-" + posRand;
    }

    public void onMatchEnd() {

        deleteTempFile();
        deletePosFile();

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
                    writeToFile(getWriter(), true, false);
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

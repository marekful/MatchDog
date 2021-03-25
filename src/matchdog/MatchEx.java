package matchdog;

import jcons.src.com.meyling.console.UnixConsole;
import matchdog.console.printer.BufferedConsolePrinter;
import matchdog.console.printer.DefaultPrinter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchEx extends Match {

    private final static Pattern multiMove = Pattern.compile("(\\s|^)([^\\s]+)\\((\\d+)\\)");
    private final static Pattern tripletMove = Pattern.compile("(\\d+|bar)\\*?/(\\d+)\\*?/(\\d+|off)\\*?");

    private final static Pattern positionId = Pattern.compile("Position ID\\s*: ([^\\s]+)[\\s\\n]");
    private final static Pattern matchId = Pattern.compile("Match ID\\s*: ([^\\s]+)[\\s\\n]");

    private final static Pattern onMove = Pattern.compile("^\\s+1\\.\\s+Cubeful\\s+\\d-ply\\s+(.*?)\\s+Eq.*$", Pattern.MULTILINE);
    private final static Pattern onRollRoll = Pattern.compile("Proper cube action: (No|Too good to) (re)?double,", Pattern.MULTILINE);
    private final static Pattern onRollDouble = Pattern.compile("Proper cube action: (Optional )?(Red|D)ouble,", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private final static Pattern onDoubleTake = Pattern.compile("Proper cube action: [^,]+, take", Pattern.MULTILINE);
    private final static Pattern onDoubleDrop = Pattern.compile("Proper cube action: [^,]+, pass", Pattern.MULTILINE);

    private final static Pattern onMoveEquities = Pattern.compile("\\s+1\\.\\s+Cubeful\\s+\\d-ply\\s+[^\\n]+\\n\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s+-\\s+[\\d.]+\\s+([\\d.]+)\\s+([\\d.]+)", Pattern.DOTALL);
    private final static Pattern onRollEquities = Pattern.compile("\\s*\\d-ply cube(ful|less) equity -?[\\d.]+[^\\n]+\\n\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s+-\\s+[\\d.]+\\s+([\\d.]+)\\s+([\\d.]+)", Pattern.DOTALL);

    public final static int HINT_TYPE_ON_MOVE = 0;
    public final static int HINT_TYPE_ON_ROLL = 1;
    public final static int HINT_TYPE_ON_DOUBLE = 2;

    Runner r;
    String hint;
    BufferedConsolePrinter eqPrinter;

    private String gnubgMatchId;
    private String gnubgPositionId;

    MatchEx(MatchDog server, String oppname, int matchlength) {
        super(server, oppname, matchlength);

        eqPrinter = new DefaultPrinter(
            server, "Equities:", UnixConsole.LIGHT_YELLOW, UnixConsole.BACKGROUND_BLACK
        );
    }

    public void onMatchEnd() {
        super.onMatchEnd();
        if (r != null && r.bg != null) {
            r.bg.kill(false);
            r.bg = null;
        }
        r = null;
    }

    public void getHint(int hintType) {
        try {
            (new Thread(new Runner(hintType))).start();
        } catch (RuntimeException ex) {
            server.printDebug("Retrying hint (type: " + hintType + ")");
            (new Thread(new Runner(hintType))).start();
        }
    }

    public String hint() {
        return hint;
    }

    private class Runner implements Runnable {

        private Gnubg bg;
        private final int hintType;

        Runner(int hintType) {
            this.hintType = hintType;
        }

        @Override
        public void run() {

            String[] fixedArgs = {"-t", "-q", "-s", server.configDir + "gnubg", "-D", server.dataDir + "gnubg"};
            bg = new GnubgHint(server, server.programPrefs.getGnubgCmdArr(), fixedArgs);
            bg.printer.setLabel("gnubg-hint");
            String content;

            try {
                getMatchHistory().writeToTempFile();
                getMatchHistory().writePositionToFile();
            } catch (IOException e) {
                bg.printer.printLine("Error writing temp file");
                return;
            }

            //String extraArgs = "-c " + getMatchHistory().getTempFileName() + ".tmp";
            String extraArgs = "-c " + getMatchHistory().getPosFileName() + ".pos.tmp";
            bg.start(extraArgs, true);

            try {
                content = Files.readString(
                    Path.of(server.getDataDir() + server.getPlayerName() + ".out.tmp"),
                    StandardCharsets.UTF_8
                );

            } catch (IOException e) {
                bg.printer.printLine("Error reading temp file");
                return;
            }

            Matcher mId = matchId.matcher(content);
            Matcher pId = positionId.matcher(content);
            String matchId = null, posId = null;
            while (mId.find()) {
                matchId = mId.group(1);
            }
            while (pId.find()) {
                posId = pId.group(1);
            }
            if (matchId != null && posId != null) {
                setGnubgMatchId(matchId);
                setGnubgPositionId(posId);
                bg.printer.printLine(getGnubgId());
                onNewGnubgId();
            }

            switch (hintType) {
                case HINT_TYPE_ON_MOVE -> {
                    Matcher m1 = onMove.matcher(content);
                    Matcher m0 = onMoveEquities.matcher(content);

                    if (m0.find()) {
                        for (int e = 1; e < 6; e++) {
                            equities[e-1] = Double.parseDouble(m0.group(e));
                            //server.printDebug("EQ: " + e + " " + m0.group(e) + " " + Double.parseDouble(m0.group(e)) + " " + equities[e-1]);
                        }
                        //equities[5] = 0.0; // cubeless equities not given here, but it's not used anyway
                        bg.printer.printLine("");
                        printEquities();
                        server.fibs.onEquitiesParsed();
                    } else {
                        bg.printer.printLine("! NO EQUITIES RESULT");
                    }

                    if (m1.find()) {
                        String cmd = transformCommand(m1.group(1));
                        server.fibsout.println(cmd);
                        bg.printer.printLine("");
                        server.fibs.printFibsCommand(m1.group(1) + " -> " + cmd);
                        getMatchHistory().addCommand(m1.group(1));
                    } else {
                        bg.printer.printLine("! NO HINT RESULT");
                        throw new RuntimeException("No hing on move");
                    }
                }
                case HINT_TYPE_ON_ROLL -> {
                    Matcher m2 = onRollRoll.matcher(content);
                    Matcher m3 = onRollDouble.matcher(content);
                    Matcher m6 = onRollEquities.matcher(content);
                    if (m2.find()) {
                        server.fibsout.println("roll");
                        bg.printer.printLine("");
                        server.fibs.printFibsCommand("roll");
                    } else if (m3.find()) {
                        server.fibsout.println("double");
                        bg.printer.printLine("");
                        server.fibs.printFibsCommand("double");
                    } else {
                        bg.printer.printLine("! NO HINT RESULT");
                        throw new RuntimeException("No hing on roll");
                    }

                    if (m6.find()) {
                        for (int e = 1; e < 6; e++) {
                            try {
                                equities[e-1] = Double.parseDouble(m6.group(e+1));
                            } catch (NumberFormatException ex) {
                                server.printDebug("NFEx parse eq on roll: " + e + " 1> " + m6.group(1) + " 0> " + m6.group(0) + " -> " + ex.getMessage());
                            }
                        }
                        bg.printer.printLine("On roll");
                        printEquities();
                    }
                }
                case HINT_TYPE_ON_DOUBLE -> {
                    Matcher m4 = onDoubleTake.matcher(content);
                    Matcher m5 = onDoubleDrop.matcher(content);
                    if (m4.find()) {
                        server.fibsout.println("accept");
                        bg.printer.printLine("");
                        server.fibs.printFibsCommand("accept");
                    } else if (m5.find()) {
                        server.fibsout.println("reject");
                        bg.printer.printLine("");
                        server.fibs.printFibsCommand("reject");
                        getMatchHistory().addCommand("reject");
                    } else {
                        bg.printer.printLine("! NO HINT RESULT");
                        throw new RuntimeException("No hing on double");
                    }
                }
            }
        }
    }

    private String orderMoves(String in) {
        ArrayList<String> bar = new ArrayList<>();
        ArrayList<String> other = new ArrayList<>();
        ArrayList<String> off = new ArrayList<>();
        String[] moves = in.split(" ");

        for (String m : moves) {
            if (m.startsWith("bar-")) {
                bar.add(m);
            } else if (m.endsWith("-off")) {
                off.add(m);
            } else {
                other.add(m);
            }
        }

        return  String.join(" ", bar) + " " +
                String.join(" ", other) + " " +
                String.join(" ", off).trim();
    }

    public String transformCommand(String in) {

        server.printDebug("transformCommand 0: " + in);
        String out = expandGnubgMoveHint(in, 1);
        server.printDebug("transformCommand 1: " + out);
        out = GnubgCommand.gnubgToFibsCommand(out);
        server.printDebug("transformCommand 2: " + out);
        out = orderMoves(out);
        server.printDebug("transformCommand 3: " + out);

        if(out.contains("-")) {

            if(isShiftmove()) {
                server.printer.printLine("SHIFTING");
                out = GnubgCommand.shift(out);
            }
            out = "move " + out;
        }
        return out;
    }

    public String expandGnubgMoveHint(String command, int pass) {

        server.printDebug("expandGnubgCommand: " + pass + " " + command);


        // recurse with m/n*?/o occurrences expanded to m/n n/o
        command = command.replaceAll("\\*", "")
                .replaceAll("bar", "25" )
                .replaceAll("off", "0" );
        Matcher tm = tripletMove.matcher(command);
        if (tm.find()) {
            return expandGnubgMoveHint(tm.replaceAll("$1/$2 $2/$3"),1);
        }

        // no multi-move to expand, skip to pass two
        if (pass == 1 && !command.contains("(")) {
            return expandGnubgMoveHint(command, 2);
        }

        // PASS ONE - EXPAND MULTI-MOVES
        String[] in = command.split(" ");
        String[] out = command.split(" ");
        String part;
        Matcher matcher;
        String clean;
        StringBuffer sb;
        int multi;

        for (int i = 0; pass == 1 && i < in.length; i++) {
            part = in[i].trim();

            server.printDebug("pass 1: " + part);

            // expand m/n(x) to m/n m/n [m/n ...]
            matcher = multiMove.matcher(part);
            sb = new StringBuffer();
            if (matcher.find()) {
                server.printDebug("multiMove: " + matcher.group(2) + " " + matcher.group(3));
                clean = "";
                try {
                    multi = Integer.parseInt(matcher.group(3));
                    for (int j = 0; j < multi; j++) {
                        clean = clean.concat(matcher.group(2)).concat(" ");
                    }
                    matcher.appendReplacement(sb, clean.trim());
                } catch (NumberFormatException ignore) {
                    server.printDebug("NFEx: " + matcher.group(3));
                }
                out[i] = sb.toString();
            }
        }

        // continue with pass two
        if (pass == 1) {
            return expandGnubgMoveHint(String.join(" ", out), 2);
        }

        // PASS TWO - EXPAND COLLAPSED MOVES + other bits
        int m = 0, n = 0;
        String[] split;
        String[] state = server.fibs.lastBoard.getBoard().getState();
        int myColour = server.fibs.lastBoard.getColour();
        int middleMove, movedSoFar = 0;
        int pieces, stateIndex = -1;
        for (int i = 0; i < out.length; i++) {
            part = out[i];
            split = part.split("/");

            server.printDebug("pass 2: " + part);

            try {
                m = Integer.parseInt(split[0].trim());
                n = Integer.parseInt(split[1].trim());
            } catch (NumberFormatException ignore) {
                server.printDebug("NFEx: " + split[0] + " " + split[1]);
            }
            boolean forward = m < n;

            // expand m/n when abs(m-n) = die1 + die2, this covers
            // - two different die (not double) in one move
            // - both parts of a double X X when both chequers moved X then X
            if ( myDice.getDie1() + myDice.getDie2() == Math.abs(m - n) /*||
                (myDice.getDie1() + myDice.getDie2() - movedSoFar  > Math.abs(m - n) && n == 0)*/) {

                if (myDice.isDouble()) {
                    if (forward) {
                        middleMove = m + myDice.getDie1();
                    } else {
                        middleMove = m - myDice.getDie1();
                    }
                } else {
                    // when expanding two different die in one move
                    // there may be only one possible order, e.g. for dice X Y
                    // only X then Y (and not Y then X) is valid
                    middleMove = -1;
                    try {
                        // check if one of the two available middle
                        // positions are blocked and select the other if so
                        if (forward) {
                            stateIndex = isShiftmove() ? 25 - (m + myDice.getSmaller()) : (m + myDice.getSmaller());
                            pieces = Integer.parseInt(state[stateIndex]);
                            server.printDebug("chk-move: forward -> " + myColour + " - " + pieces + " - " + m + " - " + myDice.getSmaller());
                            if (pieces == 0 || (pieces < 0 && myColour < 0) || (pieces > 0 && myColour > 0)) {
                                middleMove = m + myDice.getSmaller();
                            } else {
                                middleMove = m + myDice.getGreater();
                            }
                        } else {
                            stateIndex = isShiftmove() ? 25 - (m - myDice.getSmaller()) : (m - myDice.getSmaller());
                            pieces = Integer.parseInt(state[stateIndex]);
                            server.printDebug("chk-move: backward -> " + myColour + " - " + pieces + " - " + m + " - " + myDice.getSmaller());
                            if (pieces == 0 || (pieces < 0 && myColour < 0) || (pieces > 0 && myColour > 0)) {
                                middleMove = m - myDice.getSmaller();
                            } else {
                                middleMove = m - myDice.getGreater();
                            }
                        }
                    } catch (NumberFormatException e) {
                        server.printDebug("NFEx :" + stateIndex + " -> " + state[stateIndex]);
                    }
                }

                server.printDebug("2.1a: |" + out[i] + "|");
                out[i] = m + "/" + middleMove + " " + middleMove + "/" + n;
                server.printDebug("2.1b: |" + out[i] + "|");

                movedSoFar += Math.abs(m-n);
            }

            // expand m/n when abs(m-n) > greater die
            if (myDice.isDouble() && Math.abs(m - n) > myDice.getDie1()) {
                int mu = Math.abs(m - n) / myDice.getDie1(); // moved 2, 3 or 4 times X of a double
                if (Math.abs(m - n) % myDice.getDie1() > 0) {
                    mu++; // when we have remainder, do one more iteration for the partial die
                }
                String newOut = "";

                int from = m, to;
                if (forward) {
                    to = m + myDice.getDie1();
                } else {
                    to = m - myDice.getDie1();
                }

                for (int p = 1; p <= mu; p++) {
                    newOut = newOut.concat(from + "/" + to + " ");
                    from = to;
                    if (forward) {
                        to = Math.min(25, to + myDice.getDie1());
                    } else {
                        to = Math.max(0, to - myDice.getDie1());
                    }
                }

                server.printDebug("2.2a: |" + out[i] + "|");
                out[i] = newOut.trim();
                server.printDebug("2.2b: |" + out[i] + "|");
            }

            // expand m/n when ... ?
        }


        return String.join(" ", out).trim();
    }

    private void printEquities() {
        String [] eqLabel = {"W ", "W(g) ", "W(bg) ", "L(g) ", "L(bg) ", "Cubeless "};
        String eqStr = "";
        int c = 0; String value;
        for (double equity : equities) {
            value = UnixConsole.BLACK + UnixConsole.BACKGROUND_YELLOW +
                    String.format("%.2f%%", equity * 100) +
                    UnixConsole.RESET + " ";
            eqStr = eqStr.concat(eqLabel[c++]).concat(value);
        }
        eqPrinter.print(eqStr);
    }

    protected String getGnubgMatchId() {
        return gnubgMatchId;
    }

    protected void setGnubgMatchId(String gnubgMatchId) {
        this.gnubgMatchId = gnubgMatchId;
    }

    protected String getGnubgPositionId() {
        return gnubgPositionId;
    }

    protected void setGnubgPositionId(String gnubgPositionId) {
        this.gnubgPositionId = gnubgPositionId;
    }

    protected String getGnubgId() {
        if (getGnubgMatchId() == null || getGnubgPositionId() == null) {
            return null;
        }
        return getGnubgMatchId() + ":" + getGnubgPositionId();
    }

    protected void onNewGnubgId() {
        getMatchHistory().clearCommandsSinceLastId();
    }
}

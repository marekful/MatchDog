package matchdog;

import jcons.src.com.meyling.console.UnixConsole;
import matchdog.console.printer.BufferedConsolePrinter;
import matchdog.console.printer.DefaultPrinter;
import matchdog.fibsboard.Dice;
import matchdog.fibsboard.FibsBoard;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchEx extends Match {

    public final static int HINT_TYPE_ON_MOVE = 0;
    public final static int HINT_TYPE_ON_ROLL = 1;
    public final static int HINT_TYPE_ON_DOUBLE = 2;

    private final static Pattern multiMove = Pattern.compile("(\\s|^)([^\\s]+)\\((\\d+)\\)");
    private final static Pattern tripletMove = Pattern.compile("(\\d+|bar)\\*?/(\\d+)\\*?/(\\d+|off)\\*?");

    private final static Pattern positionId = Pattern.compile("Position ID\\s*: ([^\\s]+)[\\s\\n]");
    private final static Pattern matchId = Pattern.compile("Match ID\\s*: ([^\\s]+)[\\s\\n]");

    private final static Pattern onMove = Pattern.compile("^\\s+1\\.\\s+Cubeful\\s+\\d-ply\\s+(.*?)\\s+Eq.*$", Pattern.MULTILINE);
    private final static Pattern onRollRoll = Pattern.compile("Proper cube action: (No|Too good to) (re)?double,", Pattern.MULTILINE);
    private final static Pattern onRollDouble = Pattern.compile("Proper cube action: (Optional )?(Red|D)ouble,", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private final static Pattern onDoubleTake = Pattern.compile("Proper cube action: [^,]+, take", Pattern.MULTILINE);
    private final static Pattern onDoubleDrop = Pattern.compile("Proper cube action: [^,]+, pass", Pattern.MULTILINE);
    private final static Pattern youCannotDouble = Pattern.compile("You cannot double", Pattern.MULTILINE);

    private final static Pattern onMoveEquities = Pattern.compile("\\s+1\\.\\s+Cubeful\\s+\\d-ply\\s+[^\\n]+\\n\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s+-\\s+[\\d.]+\\s+([\\d.]+)\\s+([\\d.]+)", Pattern.DOTALL);
    private final static Pattern onRollEquities = Pattern.compile("\\s*\\d-ply cube(ful|less) equity \\+?-?[\\d.]+[^\\n]+\\n\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s+-\\s+[\\d.]+\\s+([\\d.]+)\\s+([\\d.]+)", Pattern.DOTALL);

    private Runner r;
    private BufferedConsolePrinter eqPrinter;

    private String gnubgMatchId;
    private String gnubgPositionId;

    MatchEx(MatchDog server, String oppname, int matchlength, boolean resume) {
        super(server, oppname, matchlength, resume);

        eqPrinter = new DefaultPrinter(
            server, "Equities:", UnixConsole.LIGHT_YELLOW, UnixConsole.BACKGROUND_BLACK
        );
    }

    public void onMatchEnd() {
        super.onMatchEnd();
        if (r != null && r.bg != null) {
            r.bg.kill(isDropped());
            r.interrupt();
            r.bg = null;
        }
        r = null;
    }

    public void getHint(int hintType) {
        Thread t = new Thread(new Runner(hintType));
        t.setUncaughtExceptionHandler((th, ex) -> {
            if (r != null && r.interrupt) {
                return;
            }
            // retry once
            server.printDebug("Retrying hint (" + ex.getMessage() + ")");
            (new Thread(new Runner(hintType))).start();
        });
        t.start();
    }

    private class Runner implements Runnable {

        private Gnubg bg;
        private final int hintType;
        private boolean interrupt;

        Runner(int hintType) {
            this.hintType = hintType;
            interrupt = false;
        }

        public void interrupt() {
            interrupt = true;
        }

        @Override
        public void run() {

            String[] fixedArgs = {"-t", "-q", "-s", server.configDir + "gnubg", "-D", server.dataDir + "gnubg"};
            bg = new GnubgHint(server, server.programPrefs.getGnubgCmdArr(), fixedArgs);
            bg.printer.setLabel("gnubg-hint");
            String content;

            try {
                //getMatchHistory().writeToTempFile();
                getMatchHistory().writePositionToFile();
            } catch (IOException e) {
                bg.printer.printLine("Error writing temp file");
                return;
            }

            if (interrupt) {
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

            if (interrupt) {
                return;
            }

            /*Matcher mId = matchId.matcher(content);
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
            }*/

            switch (hintType) {
                case HINT_TYPE_ON_MOVE -> {
                    Matcher m1 = onMove.matcher(content);
                    Matcher m0 = onMoveEquities.matcher(content);

                    if (m0.find()) {
                        for (int e = 1; e < 6; e++) {
                            try {
                                equities[e - 1] = Double.parseDouble(m0.group(e));
                            } catch (NumberFormatException ex) {
                                server.printDebug("NFEx on move equities: " + e + " " + m0.group(e) + " " + Double.parseDouble(m0.group(e)) + " " + equities[e-1]);
                            }
                        }
                        bg.printer.printLine("On move ");
                        printEquities();
                        server.fibs.onEquitiesParsed();
                    } else {
                        bg.printer.printLine("! NO EQUITIES RESULT");
                        throw new RuntimeException("No equities on move");
                    }

                    if (m1.find()) {
                        String cmd = transformCommand(m1.group(1));
                        server.fibsout.println(cmd);
                        bg.printer.printLine("");
                        server.fibs.printFibsCommand(m1.group(1) + " -> " + cmd);
                        getMatchHistory().addCommand(m1.group(1));
                    } else {
                        bg.printer.printLine("! NO HINT RESULT");
                        throw new RuntimeException("No hint on move");
                    }
                }
                case HINT_TYPE_ON_ROLL -> {
                    Matcher m2 = onRollRoll.matcher(content);
                    Matcher m3 = onRollDouble.matcher(content);
                    Matcher m6 = onRollEquities.matcher(content);
                    // this should only happen on an incorrect hint request (submit
                    // an on-roll request when I cannot double) catch it anyway
                    Matcher m7 = youCannotDouble.matcher(content);
                    if (m2.find()) {
                        server.fibsout.println("roll");
                        bg.printer.printLine("");
                        server.fibs.printFibsCommand("roll");
                    } else if (m3.find()) {
                        server.fibsout.println("double");
                        bg.printer.printLine("");
                        server.fibs.printFibsCommand("double");
                    } else if(m7.find()) {
                        server.fibsout.println("roll");
                        bg.printer.printLine("");
                        server.fibs.printFibsCommand("roll");
                    } else {
                        bg.printer.printLine("! NO HINT RESULT");
                        throw new RuntimeException("No hint on roll");
                    }

                    if (m6.find()) {
                        for (int e = 1; e < 6; e++) {
                            try {
                                equities[e-1] = Double.parseDouble(m6.group(e+1));
                            } catch (NumberFormatException ex) {
                                server.printDebug("NFEx parse eq on roll: " + e + " 1> " + m6.group(1) + " 0> " + m6.group(0) + " -> " + ex.getMessage());
                            }
                        }
                        bg.printer.printLine("On roll ");
                        printEquities();
                    } else {
                        bg.printer.printLine("! NO EQUITIES RESULT");
                        bg.printer.printLine("");
                        bg.printer.print(content);
                        //throw new RuntimeException("No equities on roll");
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
                        throw new RuntimeException("No hint on double");
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

        return  (String.join(" ", bar) + " " +
                String.join(" ", other) + " " +
                String.join(" ", off)).trim();
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
            Dice myDice = board.getMyDice();
            Dice oppDice = board.getOppDice();
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

    private static class BS extends BitSet {

        int ls = 0;

        BS() {}

        BS(int n) {
            super(n);
        }

        @Override
        public void set(int i, boolean v) {
            ls++;
            super.set(i, v);
        }

        @Override
        public void set(int i, int j, boolean v) {
            ls += (j - i);
            super.set(i, j, v);
        }

        @Override
        public String toString() {
            String o = "", o1 = "";
            for (int i = 0; i < ls; i++) {
                o1 = o1.concat(get(i) ? "1" : "0");

                if ((i + 1) % 8 == 0) {
                    o1 = new StringBuilder(o1).reverse().toString();
                    o1 = o1.concat(" ");
                    o = o.concat(o1);
                    o1 = "";
                }
            }

            if (o1.length() > 0) {
                o1 = new StringBuilder(o1).reverse().toString();
                o = o.concat(o1);
            }

            return o;
        }
    }

    private void calculateGnubgPositionId() {

        String[] b = board.getBoard().getState();
        BitSet posX = new BS();
        BitSet posO = new BS();

        ByteBuffer buff = ByteBuffer.allocate((80 - 1) / 8 + 1);

        // board
        int pX = 0, pO = 0, idxX =0, idxO = 0, bitsSetX = 0, bitsSetO = 0;
        for (int i = 1; i < 25; i++) {
            idxX = i;
            idxO = 25 - i;
            try {
                pX = Integer.parseInt(b[idxX]);
                pO = Integer.parseInt(b[idxO]);
            } catch (NumberFormatException e) {
                e.printStackTrace();;
            }

            if (pX < 0) {
                posX.set(bitsSetX, bitsSetX + Math.abs(pX), true);
                bitsSetX += Math.abs(pX);
            }
            posX.set(bitsSetX++, false);

            if (pO > 0) {
                posO.set(bitsSetO, bitsSetO + Math.abs(pO), true);
                bitsSetO += Math.abs(pO);
            }
            posO.set(bitsSetO++, false);

        }

        // bar
        try {
            pX = Math.abs(Integer.parseInt(b[0]));
            pO = Math.abs(Integer.parseInt(b[25]));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (pX > 0) {
            posX.set(bitsSetX, bitsSetX + Math.abs(pX), true);
            bitsSetX += Math.abs(pX);
        }
        posX.set(bitsSetX++, false);

        if (pO > 0) {
            posO.set(bitsSetO, bitsSetO + Math.abs(pO), true);
            bitsSetO += Math.abs(pO);
        }
        posO.set(bitsSetO++, false);



        /*int p = 80 - (bitsSetX + bitsSetO);
        BitSet pad = new BS(3);
        String ps = "";
        if (p > 0) {
            ps = "0".repeat(p);
        }*/

        int padX = 40 - bitsSetX, padO = 40 - bitsSetO;
        if (padX > 0) {
            posX.set(bitsSetX, bitsSetX + padX, false);
            bitsSetX += padX;
        }

        if (padO > 0) {
            posO.set(bitsSetO, bitsSetO + padO, false);
            bitsSetO += padO;
        }


        server.printDebug(" pid >>> " + bitsSetX + " " + posX.toString());
        server.printDebug(" pid >>> " + bitsSetO + " " + posO.toString() + "|" /*+ ps*/);

        //server.printDebug(" pid >2> " + pad.toString());


        String comb = posO.toString().replaceAll(" ", "")
                    + posX.toString().replaceAll(" ", "")
                    /*+ ps*/;



        server.printDebug("combo > > " + comb + " -> " + String.join(" ", comb.split("(?<=\\G.{8})")));

        String[] cs = comb.split("(?<=\\G.{8})");
        byte[] cb = new byte[cs.length];
        for (int i = 0; i < cs.length; i++) {
            String bys = cs[i];
            cb[i] = (byte) Integer.parseInt(bys, 2);
        }

        // combine
        /*byte[] ba = new byte[(posX.length() + 7) / 8];
        ba = Arrays.copyOf(posX.toByteArray(), ba.length);
        byte[] bb = new byte[(posO.length() + 7) / 8];
        bb = Arrays.copyOf(posO.toByteArray(), bb.length);
        byte[] bp = new byte[(pad.length() + 7) / 8];
        bp = Arrays.copyOf(pad.toByteArray(), bp.length);
        byte[] fin = new byte[ba.length + bb.length + bp.length];

        server.printDebug(" 00 > " + p + " > " + bp.length + " " + Arrays.toString(bp));

        System.arraycopy(ba, 0, fin, 0, ba.length);
        System.arraycopy(bb, 0, fin, ba.length, bb.length);
        System.arraycopy(bp, 0, fin, bb.length, bp.length);*/

        String id = new String(Base64.getEncoder().encode(cb));

        server.printDebug("dpi >> " + id + " > " + Arrays.toString(cb));

        // encode and set
        gnubgPositionId = id.substring(0, 14);
    }

    private void calculateGnubgMatchId() {

        String mIdStr = "";
        int mCube = (int)(Math.log(board.getDoublingCube()) / Math.log(2));
        mIdStr += new StringBuilder(Integer.toBinaryString(0b10000 | mCube).substring(1)).reverse();
        int mCubeOwner = board.iMayDouble() && board.oppMayDouble() ? 3 : (board.iMayDouble() ? 1 : 0);
        mIdStr += new StringBuilder(Integer.toBinaryString(0b100 | mCubeOwner).substring(1)).reverse();
        int mOnRoll = board.getColour() == board.getTurn() ? 0 : 1;
        mIdStr += mOnRoll == 0 ? "0" : "1";
        mIdStr += getCrawfordGame() == getGameno() ? "1" : "0";
        mIdStr += new StringBuilder("001").reverse();
        int mTurn = mOnRoll;
        if (mOnRoll == 0 && isOwnDoubleInProgress()) {
            mTurn = 1;
        }
        else if (mOnRoll == 1 && isOppDoubleInProgress()) {
            mTurn = 0;
        }
        mIdStr += mTurn == 0 ? "0" : "1";
        mIdStr += board.wasDoubled() ? "1" : "0";
        mIdStr += "00"; // resignation offered
        Dice d = isMyTurn() ? board.getMyDice() : board.getOppDice();
        String mDie1 = Integer.toBinaryString(0b1000 | d.getDie1()).substring(1);
        String mDie2 = Integer.toBinaryString(0b1000 | d.getDie2()).substring(1);
        mIdStr += new StringBuilder(mDie1).reverse();
        mIdStr += new StringBuilder(mDie2).reverse();
        String mMl = Integer.toBinaryString(0b1000000000000000 | board.getMatchLength()).substring(1);
        mIdStr += new StringBuilder(mMl).reverse();
        String mMyScore = Integer.toBinaryString(0b1000000000000000 | board.getMyScore()).substring(1);
        mIdStr += new StringBuilder(mMyScore).reverse();
        String mOppScore = Integer.toBinaryString(0b1000000000000000 | board.getOppScore()).substring(1);
        mIdStr += new StringBuilder(mOppScore).reverse();

        // pad with '0' out to 72, then add a non zero, then cut off byte
        // (BitSet.toByteArray() cuts off 0 bits from the end)
        mIdStr += "0000001";

        BitSet mId = new BS();
        for (int c = 0; c < mIdStr.length(); c++) {
            mId.set(c, mIdStr.charAt(c) == '1');
        }

        server.printDebug(" mid >>> " + mIdStr);
        server.printDebug(" mid >>> " + mId.toString());

        String id = new String(Base64.getEncoder().encode(mId.toByteArray()));

        server.printDebug(" dmi >> " + id);

        gnubgMatchId = id.substring(0, 12);
    }

    @Override
    public void setBoard(FibsBoard board) {
        super.setBoard(board);
        calculateGnubgMatchId();
        calculateGnubgPositionId();

        server.printDebug(" gnubgid >>> " + getGnubgId());
    }

}

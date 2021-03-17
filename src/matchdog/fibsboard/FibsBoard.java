package matchdog.fibsboard;

import java.util.Arrays;

public class FibsBoard {

    // board:You:someplayer:3:0:0:0:-2:0:0:0:0:5:0:3:0:0:0:-5:5:0:0:0:-3:0:-5:0:0:0:0:2:0:1:6:2:0:0:1:1:1:0:1:-1:0:25:0:0:0:0:2:0:0:0

    // split[2]
    String oppName;
    // split[3]
    private int matchLength;
    // split[4]
    private int myScore;
    // split[5]
    private int oppScore;
    // split[6-31]
    private BoardState board;
    // split[32]
    private int turn;
    // split[33-34]
    private Dice myDice;
    // split[35-36]
    private Dice oppDice;
    // split[37]
    private int doublingCube;
    // split[38]
    private boolean iMayDouble;
    // split[39]
    private boolean oppMayDouble;
    // split[40]
    private boolean wasDoubled;
    // split[41]
    private int colour;
    // split[42]
    private int direction;
    // split[43]
    private int homeBar0;
    // split[44]
    private int homeBar1;
    // split[45]
    private int iRemovedPieces;
    // split[46]
    private int oppRemovedPieces;
    // split[47]
    private int iHaveOnBar;
    // split[48]
    private int oppHasOnBar;
    // split[49]
    private int canMove;
    // split[50]
    private int forcedMoves;
    // split[51]
    boolean didCrawford;

    private final String rawInput;

    public FibsBoard(String boardStr) {
        this.rawInput = boardStr;
        parseBoardStr(boardStr);
    }

    private void parseBoardStr(String in) {

        if (!in.startsWith("board:")) {
            throw new IllegalArgumentException(String.format("Invalid input: %s", in));
        }

        String [] parts = in.split(":");

        if (parts.length != 53) {
            throw new IllegalArgumentException(String.format("Invalid input length: %d ", parts.length));
        }

        setOppName(parts[2]);
        setMatchLength(Integer.parseInt(parts[3]));
        setMyScore(Integer.parseInt(parts[4]));
        setOppScore(Integer.parseInt(parts[5]));
        setBoard(new BoardState(Arrays.copyOfRange(parts, 6, 32)));
        setTurn(Integer.parseInt(parts[32]));
        setMyDice(new Dice(Integer.parseInt(parts[33]), Integer.parseInt(parts[34])));
        setOppDice(new Dice(Integer.parseInt(parts[35]), Integer.parseInt(parts[36])));
        setDoublingCube(Integer.parseInt(parts[37]));
        setIMayDouble(parts[38].equals("1"));
        setOppMayDouble(parts[39].equals("1"));
        setWasDoubled(parts[40].equals("1"));
        setColour(Integer.parseInt(parts[41]));
        setDirection(Integer.parseInt(parts[42]));
        setHomeBar0(Integer.parseInt(parts[43]));
        setHomeBar1(Integer.parseInt(parts[44]));
        setIRemovedPieces(Integer.parseInt(parts[45]));
        setOppRemovedPieces(Integer.parseInt(parts[46]));
        setIHaveOnBar(Integer.parseInt(parts[47]));
        setOppHasOnBar(Integer.parseInt(parts[48]));
        setCanMove(Integer.parseInt(parts[49]));
        setForcedMoves(Integer.parseInt(parts[50]));
        setDidCrawford(Integer.parseInt(parts[51]) == 1);
    }

    public String getRawInput() {
        return rawInput;
    }

    public String getOppName() {
        return oppName;
    }

    public void setOppName(String oppName) {
        this.oppName = oppName;
    }

    public int getMatchLength() {
        return matchLength;
    }

    public void setMatchLength(int matchLength) {
        this.matchLength = matchLength;
    }

    public int getMyScore() {
        return myScore;
    }

    public void setMyScore(int myScore) {
        this.myScore = myScore;
    }

    public int getOppScore() {
        return oppScore;
    }

    public void setOppScore(int oppScore) {
        this.oppScore = oppScore;
    }

    public BoardState getBoard() {
        return board;
    }

    public void setBoard(BoardState board) {
        this.board = board;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public Dice getMyDice() {
        return myDice;
    }

    public void setMyDice(Dice myDice) {
        this.myDice = myDice;
    }

    public Dice getOppDice() {
        return oppDice;
    }

    public void setOppDice(Dice oppDice) {
        this.oppDice = oppDice;
    }

    public int getDoublingCube() {
        return doublingCube;
    }

    public void setDoublingCube(int doublingCube) {
        this.doublingCube = doublingCube;
    }

    public boolean iMayDouble() {
        return iMayDouble;
    }

    public void setIMayDouble(boolean iMayDouble) {
        this.iMayDouble = iMayDouble;
    }

    public boolean oppMayDouble() {
        return oppMayDouble;
    }

    public void setOppMayDouble(boolean oppMayDouble) {
        this.oppMayDouble = oppMayDouble;
    }

    public boolean wasDoubled() {
        return wasDoubled;
    }

    public void setWasDoubled(boolean wasDoubled) {
        this.wasDoubled = wasDoubled;
    }

    public int getColour() {
        return colour;
    }

    public void setColour(int colour) {
        this.colour = colour;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getHomeBar0() {
        return homeBar0;
    }

    public void setHomeBar0(int homeBar0) {
        this.homeBar0 = homeBar0;
    }

    public int getHomeBar1() {
        return homeBar1;
    }

    public void setHomeBar1(int homeBar1) {
        this.homeBar1 = homeBar1;
    }

    public int getIRemovedPieces() {
        return iRemovedPieces;
    }

    public void setIRemovedPieces(int iRemovedPieces) {
        this.iRemovedPieces = iRemovedPieces;
    }

    public int getOppRemovedPieces() {
        return oppRemovedPieces;
    }

    public void setOppRemovedPieces(int oppRemovedPieces) {
        this.oppRemovedPieces = oppRemovedPieces;
    }

    public int getIHaveOnBar() {
        return iHaveOnBar;
    }

    public void setIHaveOnBar(int iHaveOnBar) {
        this.iHaveOnBar = iHaveOnBar;
    }

    public int getOppHasOnBar() {
        return oppHasOnBar;
    }

    public void setOppHasOnBar(int oppHasOnBar) {
        this.oppHasOnBar = oppHasOnBar;
    }

    public int getCanMove() {
        return canMove;
    }

    public void setCanMove(int canMove) {
        this.canMove = canMove;
    }

    public int getForcedMoves() {
        return forcedMoves;
    }

    public void setForcedMoves(int forcedMoves) {
        this.forcedMoves = forcedMoves;
    }

    public int [] getScore() {
        return new int [] {myScore, oppScore};
    }

    public boolean didCrawford() {
        return didCrawford;
    }

    public void setDidCrawford(boolean didCrawford) {
        this.didCrawford = didCrawford;
    }

    public String toString() {
        String[] boardState = getBoard().getState();
        String[] out = new String[53];
        out[0] = "board";
        out[1] = "You";
        out[2] = oppName;
        out[3] = "" + matchLength;
        out[4] = "" + myScore;
        out[5] = "" + oppScore;

        out[6] = boardState[0];
        out[7] = boardState[1];
        out[8] = boardState[2];
        out[9] = boardState[3];
        out[10] = boardState[4];
        out[11] = boardState[5];
        out[12] = boardState[6];
        out[13] = boardState[7];
        out[14] = boardState[8];
        out[15] = boardState[9];
        out[16] = boardState[10];
        out[17] = boardState[11];
        out[18] = boardState[12];
        out[19] = boardState[13];
        out[20] = boardState[14];
        out[21] = boardState[15];
        out[22] = boardState[16];
        out[23] = boardState[17];
        out[24] = boardState[18];
        out[25] = boardState[19];
        out[26] = boardState[20];
        out[27] = boardState[21];
        out[28] = boardState[22];
        out[29] = boardState[23];
        out[30] = boardState[24];
        out[31] = boardState[25];

        out[32] = "" + turn;
        out[33] = "" + myDice.getDie1();
        out[34] = "" + myDice.getDie2();
        out[35] = "" + oppDice.getDie2();
        out[36] = "" + oppDice.getDie2();
        out[37] = "" + doublingCube;
        out[38] = "" + (iMayDouble ? "1" : "0");
        out[39] = "" + (oppMayDouble ? "1" : "0");
        out[40] = "" + (wasDoubled ? "1" : "0");
        out[41] = "" + colour;
        out[42] = "" + direction;
        out[43] = "" + homeBar0;
        out[44] = "" + homeBar1;
        out[45] = "" + iRemovedPieces;
        out[46] = "" + oppRemovedPieces;
        out[47] = "" + iHaveOnBar;
        out[48] = "" + oppHasOnBar;
        out[49] = "" + canMove;
        out[50] = "" + forcedMoves;
        out[51] = "" + (didCrawford ? "1" : "0");
        out[52] = "0";

        return String.join(":", out);
    }
}

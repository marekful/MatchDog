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
    // skip split[43-44]
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
    // split[51]
    boolean didCrawford;

    private String rawInput;

    public FibsBoard(String boardStr) {
        this.rawInput = boardStr;
        parseBoardStr(boardStr);
    }

    private void parseBoardStr(String in) {

        if (!in.startsWith("board:")) {
            throw new IllegalArgumentException(String.format("Invalid input: %s", in));
        }

        String [] parts = in.split(":");

        if (parts.length != 52) {
            throw new IllegalArgumentException(String.format("Invalid input length: %s", in));
        }

        setOppName(parts[2]);
        setMatchLength(Integer.parseInt(parts[3]));
        setMyScore(Integer.parseInt(parts[4]));
        setOppScore(Integer.parseInt(parts[5]));
        setBoard(new BoardState(Arrays.copyOfRange(parts, 6, 31)));
        setTurn(Integer.parseInt(parts[32]));
        setMyDice(new Dice(Integer.parseInt(parts[33]), Integer.parseInt(parts[34])));
        setOppDice(new Dice(Integer.parseInt(parts[35]), Integer.parseInt(parts[36])));
        setDoublingCube(Integer.parseInt(parts[37]));
        setIMayDouble(parts[38].equals("1"));
        setOppMayDouble(parts[39].equals("1"));
        setWasDoubled(parts[40].equals("1"));
        setColour(Integer.parseInt(parts[41]));
        setDirection(Integer.parseInt(parts[42]));
        setIRemovedPieces(Integer.parseInt(parts[45]));
        setOppRemovedPieces(Integer.parseInt(parts[46]));
        setIHaveOnBar(Integer.parseInt(parts[47]));
        setOppHasOnBar(Integer.parseInt(parts[48]));
        setCanMove(Integer.parseInt(parts[49]));
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

    public int [] getScore() {
        return new int [] {myScore, oppScore};
    }

    public boolean didCrawford() {
        return didCrawford;
    }

    public void setDidCrawford(boolean didCrawford) {
        this.didCrawford = didCrawford;
    }
}

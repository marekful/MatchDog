package matchdog.console.printer;

import matchdog.Match;
import matchdog.PrintableStreamSource;
import matchdog.fibsboard.Dice;
import matchdog.fibsboard.FibsBoard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AsciiBoardPrinter extends MatchInfoPrinter {

    private final String template;
    private FibsBoard board;

    public AsciiBoardPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
        super(source, label, color, bgColor);
        template = readAsciiTemplate();
    }

    private String readAsciiTemplate() {
        try {
            return Files.readString(new File("../config/asciiboard2.txt").toPath(), StandardCharsets.US_ASCII);
        } catch (IOException ignore) {
            return "";
        }
    }

    private String constituteTemplate(FibsBoard board) {

        String[] state = board.getBoard().getState();
        Match m = dog.getMatch();
        Dice myDice = m.getMyDice();
        Dice oppDice = m.getOppDice();

        String asciiBoard = template
            .replace("xb", "x" + state[0].replace("-", ""))
            .replace("ob",  "o" + state[25])

            .replace("d1", " " + (myDice != null && !m.isMyTurn() ? myDice.getDie1() : " "))
            .replace("d2", " " + (myDice != null && !m.isMyTurn() ? myDice.getDie2() : " "))
            .replace("d3", " " + (oppDice != null && !m.isOppsTurn() ? oppDice.getDie1() : " "))
            .replace("d4", " " + (oppDice != null && !m.isOppsTurn() ? oppDice.getDie2() : " "))

            .replace("xhh", "x" +board.getIRemovedPieces())
            .replace("ohh", "o" +board.getOppRemovedPieces())

            .replaceAll("\\*", m.isMyTurn() ? "^" : "⌄");

        int c = 0;
        String l1 = "", l2 = "";
        for (int i = 1; i < 25; i++) {
            try {
                c = Integer.parseInt(state[i]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            if (c < 0) {
                l1 = (c > -10 ? " " : "" ) + "x";
                l2 = (c > -10 ? " " : "" ) + (c == -2 ? "x" : (Math.abs(c)));
            } else if (c > 0) {
                l1 = (c < 10 ? " " : "" ) + "o";
                l2 = (c < 10 ? " " : "" ) + (c == 2 ? "o" : c);
            } else {
                l1 = " .";
                l2 = "  ";
            }

            asciiBoard = asciiBoard.replace((i < 10 ? "0" : "" ) + i, l1);
            asciiBoard = asciiBoard.replace("" + (i + 30), l2);
        }

        return asciiBoard;
    }

    public void printBoard(FibsBoard board) {
        this.board = board;;
        printLine(getTurnLabel());
    }

    @Override
    public String getLabel() {
        if (board == null) return "";
        return "\n" + constituteTemplate(board);
    }

    public String getTurnLabel() {

        if (dog.getMatch() == null) return "";

        if (dog.getMatch().isMyTurn()) {
            return "\n *** My turn ***   \n";
        } else if (dog.getMatch().isOppsTurn()) {
            return "\n *** Opp's turn ***\n";
        } else {
            return "\n                   \n";
        }
    }
}

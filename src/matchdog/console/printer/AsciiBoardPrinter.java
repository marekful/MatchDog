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
            return Files.readString(new File("../config/asciiboard.txt").toPath(), StandardCharsets.US_ASCII);
        } catch (IOException ignore) {
            return "";
        }
    }

    private String constituteTemplate(FibsBoard board) {

        String[] state = board.getBoard().getState();
        Match m = dog.getMatch();
        Dice myDice = m.getMyDice();
        Dice oppDice = m.getOppDice();

        return template
                .replace("xb", "x" + state[0].replace("-", ""))
                .replace("bo",  state[25] + "o")
                .replace("01", (state[1].length() == 1 ? " " : "") + state[1])
                .replace("02", (state[2].length() == 1 ? " " : "") + state[2])
                .replace("03", (state[3].length() == 1 ? " " : "") + state[3])
                .replace("04", (state[4].length() == 1 ? " " : "") + state[4])
                .replace("05", (state[5].length() == 1 ? " " : "") + state[5])
                .replace("06", (state[6].length() == 1 ? " " : "") + state[6])
                .replace("07", (state[7].length() == 1 ? " " : "") + state[7])
                .replace("08", (state[8].length() == 1 ? " " : "") + state[8])
                .replace("09", (state[9].length() == 1 ? " " : "") + state[9])
                .replace("10", (state[10].length() == 1 ? " " : "") + state[10])
                .replace("11", (state[11].length() == 1 ? " " : "") + state[11])
                .replace("12", (state[12].length() == 1 ? " " : "") + state[12])
                .replace("13", (state[13].length() == 1 ? " " : "") + state[13])
                .replace("14", (state[14].length() == 1 ? " " : "") + state[14])
                .replace("15", (state[15].length() == 1 ? " " : "") + state[15])
                .replace("16", (state[16].length() == 1 ? " " : "") + state[16])
                .replace("17", (state[17].length() == 1 ? " " : "") + state[17])
                .replace("18", (state[18].length() == 1 ? " " : "") + state[18])
                .replace("19", (state[19].length() == 1 ? " " : "") + state[19])
                .replace("20", (state[20].length() == 1 ? " " : "") + state[20])
                .replace("21", (state[21].length() == 1 ? " " : "") + state[21])
                .replace("22", (state[22].length() == 1 ? " " : "") + state[22])
                .replace("23", (state[23].length() == 1 ? " " : "") + state[23])
                .replace("24", (state[24].length() == 1 ? " " : "") + state[24])

                .replace("d1", " " + (myDice != null && !m.isMyTurn() ? myDice.getDie1() : " "))
                .replace("d2", " " + (myDice != null && !m.isMyTurn() ? myDice.getDie2() : " "))
                .replace("d3", " " + (oppDice != null && !m.isOppsTurn() ? oppDice.getDie1() : " "))
                .replace("d4", " " + (oppDice != null && !m.isOppsTurn() ? oppDice.getDie2() : " "))

                .replace("xhh", "x" +board.getIRemovedPieces())
                .replace("ohh", "o" +board.getOppRemovedPieces())

                .replaceAll("\\*", m.isMyTurn() ? "^" : "⌄")
                ;

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

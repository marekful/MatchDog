package matchdog.console.printer;

import matchdog.MatchDog;
import matchdog.PrintableStreamSource;

public class MatchDogPrinter extends BufferedConsolePrinter {

    public MatchDogPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
        super(source, label, color, bgColor);
    }

    public String getLabel() {
        MatchDog g = ((MatchDog)super.getSource());
        int c = g.getGPC();
        return g.getPlayerName() + ( c > -1 ? "[" + c + "]" : "") + ":";
    }
}

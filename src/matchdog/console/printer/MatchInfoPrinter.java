package matchdog.console.printer;

import matchdog.MatchDog;
import matchdog.PrintableStreamSource;

public class MatchInfoPrinter extends BufferedConsolePrinter {

    MatchDog dog;

    public MatchInfoPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
        super(source, label, color, bgColor);
        dog = (MatchDog) source;
    }

    @Override
    public String getLabel() {
        String info = "";

        if (dog.isPlaying()) {
            info += "[" + dog.getMatch().getPlayer1() + "]";
        }

        return getColor() + getBgColor() + "MatchInfo" + info + RESET;
    }

    public void printMatchInfo() {
        if (!dog.isPlaying()) {
            return;
        }

        printLine(dog.getMatch().matchInfo());
    }
}

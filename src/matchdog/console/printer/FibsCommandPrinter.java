package matchdog.console.printer;

import matchdog.PrintableStreamSource;

public class FibsCommandPrinter extends BufferedConsolePrinter {

    public FibsCommandPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
        super(source, label, color, bgColor);
    }

    public void printFibsCommand(String command) {
        setLabel(command);
        print("");
    }
}

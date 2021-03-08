package matchdog.console.printer;

import matchdog.PrintableStreamSource;

public class DefaultPrinter extends BufferedConsolePrinter {

    public DefaultPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
        super(source, label, color, bgColor);
    }
}

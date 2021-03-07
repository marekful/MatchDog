package matchdog;

import java.io.PrintStream;

public class ConsoleColorPrinter extends ConsolePrinter {
    public static final String DEFAULT_COLOR = "";
    public static final String DEFAULT_BGCOLOR = "";
    public static final String RESET = "\u001B[0m";

    public ConsoleColorPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
        super(source, label);
        this.color = color;
        this.bgColor = bgColor;
    }

    public String getColor() {
        if(color != null) {
            return color;
        }
        return DEFAULT_COLOR;
    }

    public String getBgColor() {
        if(bgColor != null) {
            return bgColor;
        }
        return DEFAULT_BGCOLOR;
    }

    public synchronized void print(String msg, PrintStream os, String label) {
		if (!label.equals("")) {
			label = getColor() + getBgColor() + label + RESET + " ";
		}
        os.print(label + msg);
        os.flush();
    }

}

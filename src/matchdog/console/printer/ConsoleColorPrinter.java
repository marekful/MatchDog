package matchdog.console.printer;

import jcons.src.com.meyling.console.ConsoleForegroundColor;
import jcons.src.com.meyling.console.UnixConsole;
import matchdog.PrintableStreamSource;

public class ConsoleColorPrinter extends ConsolePrinter {
    public static final String DEFAULT_COLOR = "";
    public static final String DEFAULT_BGCOLOR = "";
    public static final String RESET = "\u001B[0m";

    private String color, bgColor;

    ConsoleColorPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
        super(source, label);
        this.color = color;
        this.bgColor = bgColor;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = color;
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

    public String getPrompt() {
        return (getLabel() != null ? getColor() + getBgColor() + getLabel() + RESET + " " : "");
    }
}

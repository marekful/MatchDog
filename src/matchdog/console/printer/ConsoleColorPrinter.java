package matchdog.console.printer;

import matchdog.PrintableStreamSource;

public class ConsoleColorPrinter extends ConsolePrinter {
    public static final String DEFAULT_COLOR = "";
    public static final String DEFAULT_BGCOLOR = "";
    public static final String RESET = "\u001B[0m";

    private final String color, bgColor;

    ConsoleColorPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
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

    public String getPrompt() {
        return (getLabel() != null ? getColor() + getBgColor() + getLabel() + RESET + " " : "");
    }
}

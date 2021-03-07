package matchdog;

import java.io.*;

public class ConsolePrinter {
	
	public static final String DEFAULT_LABEL = "DebugPrinter: ";
	public static final String DEFAULT_COLOR = "";
	public static final String DEFAULT_BGCOLOR = "";
	public static final String RESET = "\u001B[0m";
	
	PrintableStreamSource source;
	String label, color, bgColor;
	
	ConsolePrinter(PrintableStreamSource source, String label, String color, String bgColor) {
		this.source = source;
		this.label = label;
		this.color = color;
		this.bgColor = bgColor;
	}
	
	public String getLabel() {
		if(label != null) {
			return label;
		}
		return ConsolePrinter.DEFAULT_LABEL;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getColor() {
		if(color != null) {
			return color;
		}
		return ConsolePrinter.DEFAULT_COLOR;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getBgColor() {
		if(bgColor != null) {
			return bgColor;
		}
		return ConsolePrinter.DEFAULT_BGCOLOR;
	}

	public void setBgColor(String bgColor) {
		this.bgColor = bgColor;
	}
	
	public synchronized void printDebug(String msg, PrintStream os) {
        printDebug(msg, os, getLabel());
	}
	
	public synchronized void printDebug(String msg) {
		for(PrintStream os : source.getPrintStreams()) {
			printDebug(msg, os);
		}
	}
	
	public synchronized void printDebug(String msg, PrintStream os, String label) {
        if (!label.equals("")) {
            label = getColor() + getBgColor() + label + ConsolePrinter.RESET + " ";
        }
        os.print(label + msg);
        os.flush();
	}
	
	public synchronized void printDebug(String msg, String label) {
		for(PrintStream os : source.getPrintStreams()) {
			printDebug(msg, os, label);
		}
	}
	
	public synchronized void printDebugln(String msg, PrintStream os) {
		os.println();
		printDebug(msg, os);
	}
	
	public synchronized void printDebugln(String msg) {
		for(PrintStream os : source.getPrintStreams()) {
			printDebugln(msg, os);
		}
	}
	
	public synchronized void printDebugln(String msg, PrintStream os, String label) {
		os.println();
		printDebug(msg, os, label);
	}
	
	public synchronized void printDebugln(String msg, String label) {
		for(PrintStream os : source.getPrintStreams()) {
			printDebugln(msg, os, label);
		}
	}
}

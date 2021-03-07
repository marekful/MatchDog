package matchdog;

import java.io.*;

public class ConsolePrinter {
	
	public static final String DEFAULT_LABEL = "DebugPrinter: ";

	
	PrintableStreamSource source;
	String label, color, bgColor;
	
	public ConsolePrinter(PrintableStreamSource source, String label) {
		this.source = source;
		this.label = label;
	}
	
	public String getLabel() {
		if(label != null) {
			return label;
		}
		return ConsolePrinter.DEFAULT_LABEL;
	}

	public synchronized void print(String msg, PrintStream os) {
        print(msg, os, getLabel());
	}
	
	public synchronized void print(String msg) {
		for(PrintStream os : source.getPrintStreams()) {
			print(msg, os);
		}
	}
	
	protected synchronized void print(String msg, PrintStream os, String label) {
		if (!label.equals("")) {
			label += " ";
		}
		os.print(label + msg);
        os.flush();
	}

	protected synchronized void print(String msg, String label) {
		for(PrintStream os : source.getPrintStreams()) {
			print(msg, os, label);
		}
	}
	
	public synchronized void printLine(String msg, PrintStream os) {
		os.println();
		print(msg, os);
	}
	
	public synchronized void printLine(String msg) {
		for(PrintStream os : source.getPrintStreams()) {
			printLine(msg, os);
		}
	}

	protected synchronized void printLine(String msg, PrintStream os, String label) {
		os.println();
		print(msg, os, label);
	}

	protected synchronized void printLine(String msg, String label) {
		for(PrintStream os : source.getPrintStreams()) {
			printLine(msg, os, label);
		}
	}
}

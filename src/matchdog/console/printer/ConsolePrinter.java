package matchdog.console.printer;

import matchdog.PrintableStreamSource;

import java.io.*;

public class ConsolePrinter {
	
	public static final String DEFAULT_LABEL = "DebugPrinter: ";

	
	private PrintableStreamSource source;
	private String label, origLabel;
	
	ConsolePrinter(PrintableStreamSource source, String label) {
		this.source = source;
		this.label = origLabel = label;
	}
	
	public String getLabel() {
		if(label != null) {
			return label;
		}
		return DEFAULT_LABEL;
	}

	public ConsolePrinter setLabel(String label) {
		this.label = label;
		return this;
	}

	public ConsolePrinter resetLabel() {
		label = origLabel;
		return this;
	}

	public String getPrompt() {
		return (getLabel() != null ? getLabel() + " " : "");
	}

	public synchronized void print(String msg, PrintStream os) {
        print(msg, os, getPrompt());
	}
	
	public synchronized ConsolePrinter print(String msg) {
		for(PrintStream os : source.getPrintStreams()) {
			print(msg, os);
		}
		return this;
	}
	
	protected synchronized ConsolePrinter print(String msg, PrintStream os, String prompt) {
		os.print(prompt + msg);
        os.flush();
		return this;
	}

	public synchronized ConsolePrinter print(String msg, String label) {
		for(PrintStream os : source.getPrintStreams()) {
			print(msg, os, label);
		}
		return this;
	}
	
	public synchronized ConsolePrinter printLine(String msg, PrintStream os) {
		os.println();
		print(msg, os);
		return this;
	}
	
	public synchronized ConsolePrinter printLine(String msg) {
		for(PrintStream os : source.getPrintStreams()) {
			printLine(msg, os);
		}
		return this;
	}

	protected synchronized ConsolePrinter printLine(String msg, PrintStream os, String label) {
		os.println();
		print(msg, os, label);
		return this;
	}

	public synchronized ConsolePrinter printLine(String msg, String label) {
		for(PrintStream os : source.getPrintStreams()) {
			printLine(msg, os, label);
		}
		return this;
	}
}

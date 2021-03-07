
package matchdog;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

public class BufferedConsolePrinter extends ConsoleColorPrinter {

    private static final Object lock = new Object();
    private static final HashMap<PrintStream, ArrayList<String>> buff;

    static {
        buff = new HashMap<PrintStream, ArrayList<String>>();
    }

    static synchronized void removeOutputBuffer(PrintStream os) {
        if(buff.get(os) == null) return;
        buff.remove(os);
    }

    private final PrintableStreamSource source;
    private final HashMap<PrintStream, Boolean> suspended;
    private final String lineSeparator;

    public BufferedConsolePrinter(PrintableStreamSource source, String label, String color, String bgColor) {
		super(source, label, color, bgColor);
		this.source = source;
		suspended = new HashMap<PrintStream, Boolean>();
		lineSeparator = System.getProperty("line.separator");
	}
	
	private void _flush(PrintStream os) {
		synchronized (lock) {
            if(buff.get(os) == null) return;
			for(String msg : buff.get(os)) {
				os.print(msg);
			}
			os.flush();
			buff.get(os).clear();
		}
	}

	private void _buff(PrintStream os, String msg, String label) {
		synchronized (lock) {
			buff.computeIfAbsent(os, k -> new ArrayList<String>());
            buff.get(os).add(label + " " + msg);
		}
	}

	public synchronized void print(String msg, PrintStream os, String label) {
		if(isSuspended(os)) {
			if(!label.equals("")) {
				label = getColor() + getBgColor()+ label + RESET + " ";
			}
			_buff(os, msg, label);
		} else {
			super.print(msg, os, label);
		}
	}
	
	public synchronized void printLine(String msg, PrintStream os) {
		if(isSuspended(os)) {
			_buff(os, msg, lineSeparator + getColor() + getBgColor()
					+ getLabel() + RESET);
		} else {
			super.printLine(msg, os);
		}
	}
	
	
	public synchronized void printLine(String msg, PrintStream os, String label) {
		if(isSuspended(os)) {
			if(!label.equals("")) {
				label = lineSeparator + getColor() + getBgColor()+ label + RESET;
			}
			_buff(os, msg, label);
		} else {
			super.printLine(msg, os, label);
		}
	}
	
	public boolean isSuspended(PrintStream os) {
		return suspended.containsKey(os) && suspended.get(os);
	}

	public void setSuspended(PrintStream os, boolean suspended) {
		if(isSuspended(os) && !suspended && buff.size() > 0) {
			_flush(os);
		}
		this.suspended.put(os, suspended);
	}

    public PrintableStreamSource getSource() {
        return source;
    }
}

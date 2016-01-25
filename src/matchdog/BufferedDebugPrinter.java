
package matchdog;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

public class BufferedDebugPrinter extends DebugPrinter {
	
	PrintableStreamSource source;
	ArrayList<String> buff;
	HashMap<PrintStream, Boolean> suspended;
	String lineSeparator;

    private static final Object lock = new Object();

    public BufferedDebugPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
		super(source, label, color, bgColor);
		this.source = source;
		suspended = new HashMap<PrintStream, Boolean>();
		buff = new ArrayList<String>();
		lineSeparator = System.getProperty("line.separator");
	}
	
	private void _flush(PrintStream os) {
		synchronized (lock) {
			for(String msg : buff) {
				os.print(msg);
			}
			os.flush();
			buff.clear();
		}
	}

	private void _buff(String msg, String label) {
		synchronized (lock) {
			buff.add(label + " " + msg);
		}
	}

	public synchronized void printDebug(String msg, PrintStream os, String label) {
		if(isSuspended(os)) {
			if(!label.equals("")) {
				label = getColor() + getBgColor()+ label + DebugPrinter.RESET + " ";
			}
			_buff(msg, label);
		} else {
			super.printDebug(msg, os, label);
		}
	}
	
	public synchronized void printDebugln(String msg, PrintStream os) {
		if(isSuspended(os)) {
			_buff(msg, lineSeparator + getColor() + getBgColor() 
					+ getLabel() + DebugPrinter.RESET);
		} else {
			super.printDebugln(msg, os);
		}
	}
	
	
	public synchronized void printDebugln(String msg, PrintStream os, String label) {
		if(isSuspended(os)) {
			if(!label.equals("")) {
				label = lineSeparator + getColor() + getBgColor()+ label + DebugPrinter.RESET;
			}
			_buff(msg, label);
		} else {
			super.printDebugln(msg, os, label);
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

    public ArrayList<String> getBuff() {
        return buff;
    }

    public void setBuff(ArrayList<String> buff) {
        this.buff = buff;
    }
}

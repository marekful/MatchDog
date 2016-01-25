package matchdog;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import jcons.src.com.meyling.console.UnixConsole;

public class BGSocket extends Thread  {
	InetAddress sa = null;
	Socket s = null;
	InputStream sin = null;
	OutputStream sout = null;
	BufferedReader in;
	PrintWriter out;
	MatchDog server;
	BufferedDebugPrinter printer;
	boolean busy, run, connected, dead, evalcmd, wMonitor;

	long sockettime, replytime;
	
	String line = "";
	
	BGSocket(MatchDog server) {
			
		this.server = server;
		wMonitor = false;
		busy = false;
		connected = false;
		dead = false;
		evalcmd = false;
		setName("BGSocket");
		printer = new BufferedDebugPrinter(
			server, "gnubg-external:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_BLUE
		);
		printer.setBuff(server.outputBuffer);
	}

	@Override
	public void run() {
		run = true;
		try {
			synchronized(MatchDog.lock) {
                MatchDog.lock.notify();
			}
			while(( line = in.readLine()) != null && run) {
	
				if(!line.startsWith("Error")){
					processReply();
				} else {
					busy = false;
					server.printDebug(" --**--> GNUBG ERROR: ");
					server.printDebug(line);
					
				}
			}
			sin.close();
			sout.close();
			in.close();
			out.close();
		} catch (IOException e) {

            server.systemPrinter.printDebugln(e.toString());
		}
		server.systemPrinter.printDebugln("Exiting BGSocket thread");
		dead = true;
	}
	

	/*private void printDebug(String msg, PrintStream os) {
		printDebug(msg, os, getDebugLabel());
	}
	
	private void printDebug(String msg) {
		for(PrintStream os : server.listeners.values()) {
			printDebug(msg, os);
		}
	}
	
	protected void printDebugln(String msg, PrintStream os) {
		os.println();
		printDebug(msg, os);
	}
	
	protected void printDebugln(String msg) {
		for(PrintStream os : server.listeners.values()) {
			printDebugln(msg, os);
		}
	}
	
	protected void printDebug(String msg, PrintStream os, String label) {
		os.print(UnixConsole.BLACK);
		os.print(UnixConsole.BACKGROUND_WHITE);
		os.print(label);
		os.print(UnixConsole.RESET + " ");
		os.print(msg);
		os.flush();
	}
	
	protected void printDebug(String msg, String label) {
		for(PrintStream os : server.listeners.values()) {
			printDebug(msg, os, label);
		}
	}
	
	private String getDebugLabel() {
		return "gnubgextrnal:";
	}*/

	public synchronized void println(String lineIn) {

		if(busy) {
			// FIXME
			// This is actually not a bug: can be caused by a 'kibitz move'
			// during gameplay or by the stamptimer.
			// Could be handled better.
			printer.printDebugln("*** !! *** NOT SENDING new line to socket");
			return;
		}
		busy = true;
		
		if(lineIn.trim().equals("")) {
			printer.printDebugln("*** !! *** NOT SENDING empty line");
			return;
		}
		
		if(isEvalcmd()) {
			
			printer.printDebugln("sending EVALUATION CMD... ");	
		} else {
			printer.printDebugln("sending BOARD STATE... ");	
		}
		
		sockettime = System.nanoTime();
		out.printf("%s", lineIn.trim() + "\r\n");

		printer.printDebug("(" + ((System.nanoTime() - sockettime) / 1000000.0) + " ms) OK, waiting for reply", "");
		replytime = System.nanoTime();
	}
	
	synchronized void processReply() {

		double replydiff = 0.0;
		String unit = "";
	
		if((replydiff = (System.nanoTime() - replytime) / 1000000000.0) < 0.001) {
			replydiff *= 1000;
			unit = " ms";	
		} else {
			unit = " seconds";
		}
		
		if(isEvalcmd()) {

			printer.printDebugln("gnubg EQUITIES (in " 
					+ replydiff + unit 
					+ "): " + line);
			
			parseEquities(line);
			setEvalcmd(false);

			if(wMonitor) {
				wMonitor = false;
				synchronized (server.fibs) {
					server.fibs.notify();
				}
			}
			busy = false;
			return;
		} else {
			printer.printDebugln("gnubg says (in "
					+ replydiff + unit 
					+ "): " + line);
		}
		
		String fibscommand =  line.replace("25", "bar").
								  replace("/0", "/off").
								  replace("/", "-").
								  replace("*", "").
								  replace("take", "accept").
								  replace("drop", "reject");
		if(server.fibsmode == 2) {
			if(fibscommand.contains("-")) {
				
				if(server.fibs.match.isShiftmove()) {
					fibscommand = shift(fibscommand);
				}
				
				fibscommand = "move " + fibscommand;

			}
			server.fibs.sleepFibs(100);
			server.fibsout.println(fibscommand);
			printer.printDebugln("sent to fibs: ");
			server.fibs.printFibsCommand(fibscommand);
			printer.printDebugln("");
			server.fibs.printMatchInfo();
				
		}
		busy = false;		
	}

	private void parseEquities(String in) {
		
		// FIXME
		//// this is bug... 
		// sometimes a 'not-to-roll' case is not detected
		// and a board: which results in a 'roll' or other
		// non expected reply is sent from gnubg.
		if(in.startsWith("roll") || in.startsWith("double") || in.contains("/") || in.equals("")) {
			printer.printDebugln("*** !! BUG parseEquities: " + in);
			/*server.fibs.match.equities[0] = .5;
			server.fibs.match.equities[1] = .5;
			server.fibs.match.equities[2] = .5;
			server.fibs.match.equities[3] = .0;
			server.fibs.match.equities[4] = .0;*/
			return;
		}

		String [] split0 = line.split(" ");
		for(int i = 0; i < 6; i++) {
			if(!split0[i].equals("")) {
				server.fibs.match.equities[i] = Double.parseDouble(split0[i]);
			}
		}
	}

	private String shift(String in) {
		server.printDebug("SHIFTING");
		String [] arr0 = in.split(" ");
		String returnStr = "";
		for(int i = 0; i < arr0.length; i++) {
			String [] arr1 = arr0[i].split("-");
			
			try {
				int n = Integer.parseInt(arr1[0]);
				
				if(n < 25 && n > 0) {
					n = 25 - n;
					arr1[0] = Integer.toString(n);
				}
				
			} catch (Exception e) {
				//server.printDebug("not shifting " + arr1[0]);
			}
			try {
				int m = Integer.parseInt(arr1[1]);
				
				if(m < 25 && m > 0) {
					m = 25 - m;
					arr1[1] = Integer.toString(m);
				}
				
			} catch (Exception e) {
				//server.printDebug("not shifting " + arr1[1]);
			}
			arr0[i] = arr1[0] + "-" + arr1[1];
			returnStr += arr0[i] + " ";
		}
		//server.printDebug("shift result: " + returnStr);
		return returnStr;
	}

	public void connect() {
		
		while(!connected) {
			try {
				BGSocket.sleep(100);
				sa = InetAddress.getByName("localhost");
				s = new Socket(sa, server.prefs.getGnuBgPort());
				if(s.isConnected()) {
					server.systemPrinter.printDebugln("Successfully connected to bg socket");
					connected = true;					
				}
				sin = s.getInputStream();
				sout = s.getOutputStream();	
				in = new BufferedReader(new InputStreamReader(sin ));
				out = new PrintWriter(sout, true);	
				
			} catch(InterruptedException e) {
				return;
			} catch(Exception e) {
				server.systemPrinter.printDebugln("Exception in BGSocket.connect(): " + e.getMessage() + ", retrying...");
			}
		}
	}	
	
	public void terminate() {
		this.run = false;

	}

	public synchronized boolean isEvalcmd() {
		return evalcmd;
	}

	public synchronized void setEvalcmd(boolean evalcmd) {
		this.evalcmd = evalcmd;
	}
}

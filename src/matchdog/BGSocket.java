package matchdog;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class BGSocket extends Thread  {
	InetAddress sa = null;
	Socket s = null;
	InputStream sin = null;
	OutputStream sout = null;
	BufferedReader in;
	PrintWriter out;
	MatchDog server;
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
	}

	@Override
	public void run() {
		run = true;
		try {
			synchronized(server) {
				server.notify();
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
	
			server.printDebug(e.toString());
		}
		server.printDebug("Exiting BGSocket thread");
		dead = true;
	}

	public synchronized void println(String lineIn) {

		if(busy) {
			// FIXME
			// This is actually not a bug: can be caused by a 'kibitz move'
			// during gameplay or by the stamptimer.
			// Could be handled better.
			server.printDebug("*** !! *** NOT SENDING new line to socket");
			return;
		}
		busy = true;
		
		if(lineIn.trim().equals("")) {
			server.printDebug("*** !! *** NOT SENDING empty line");
			return;
		}
		
		if(isEvalcmd()) {
			
			server.printDebug("bgsocket: sending EVALUATION CMD to gnubg... ");	
		} else {
			server.printDebug("bgsocket: sending BOARD STATE to gnubg... ");	
		}
		
		sockettime = System.nanoTime();
		out.printf("%s", lineIn.trim() + "\r\n");

		System.out.print("(" + ((System.nanoTime() - sockettime) / 1000000.0) + " ms) OK, waiting for reply");
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

			server.printDebug("bgsocket: gnubg EQUITIES (in " 
					+ replydiff + unit 
					+ "): " + line);
			
			parseEquities(line);
			setEvalcmd(false);

			if(wMonitor == true) {
				wMonitor = false;
				synchronized (server.fibs) {
					server.fibs.notify();
				}
			}
			busy = false;
			return;
		} else {
			server.printDebug("bgsocket: gnubg says (in "
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
			server.printDebug("bgsocket: sent2fibs: ");
			server.fibs.fibsCommand(fibscommand);
			server.printDebug("");
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
			server.printDebug("*** !! BUG parseEquities: " + in);
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
		server.printDebug("Connecting to bgsocket ");
		int c = 0, cc = 0;
		while(!connected) {
			try {
				sa = InetAddress.getByName("localhost");
				s = new Socket(sa, server.prefs.getGnuBgPort());
				if(s.isConnected()) {
					server.printDebug("Successfully connected to bg socket");
					connected = true;					
				}
				sin = s.getInputStream();
				sout = s.getOutputStream();	
				in = new BufferedReader(new InputStreamReader(sin ));
				out = new PrintWriter(sout, true);			
			} catch (Exception e) {
				c++;
				if(c % 1000 == 1) {
					cc++;
					System.out.print(".");
				}
			}
			if(cc > 50) {
				server.printDebug("CONNECTION TO SOCKET TIMED OUT (localhost:" 
						+ server.prefs.getGnuBgPort() + ")");
				break;
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

package matchdog;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Collection;
import java.io.*;

import jcons.src.com.meyling.console.UnixConsole;

import etc.TorExitNodeChecker;

public class MatchDog extends Thread implements PrintableStreamSource {
	
	protected static final String PROMPT = "? ";

	BGRunner gnubg;
	FibsRunner fibs;
	OutputStream gnubgos;
	PrintWriter gnubgout;
	OutputStream fibsos;
	PrintWriter fibsout, writer;
	BGSocket bgsocket;
	Inviter inviter;
	PlayerPrefs prefs;
	Map<Integer, String> savedMatches;
	String lastopp;
	int replayTimeout = 90, // sec TODO move this to prefs
		fibsCount; 
	Date serverStartedAt, lastfinish;
	PlayerStats playerstats;
	StatWriter statview;
	String pstatspath;
	String outputName;
	double ownRating;
	boolean contd, listenLocal;
	
	HashMap<Integer, String> bl = new HashMap<Integer, String>();
	HashMap<String, String> statrequests = new HashMap<String, String>();
	
	Timer keepalivetimer;
	TimerTask keepalivetimertask;
	HashMap<Integer, PrintStream> listeners = new HashMap<Integer, PrintStream>();
	
	protected int fibsmode;
	
	DebugPrinter printer;
	DebugPrinter systemPrinter;
	
	MatchDog(PlayerPrefs prefs, HashMap<Integer, String> bl, boolean listenLocal) {
		
		this.prefs = prefs;
		this.listenLocal = listenLocal;
		
		gnubg = null;
		fibs = null;
		gnubgos = null;
		gnubgout = null;
		fibsos = null;
		fibsout = null;
		writer = null;
		inviter = null;
		
		lastopp = null;
		lastfinish = null;

		fibsmode = 0;
		fibsCount = 0;
		
		savedMatches = new HashMap<Integer, String>();
		playerstats = null;
		ownRating = 0;
		outputName = "";
		serverStartedAt = new Date();
		
		// global blacklist
		this.bl = bl;
		
		printer = new DebugPrinter(
			this, "MatchDog:", UnixConsole.BLACK, UnixConsole.BACKGROUND_WHITE
		);
		systemPrinter = new DebugPrinter(
			this, "system:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_RED
		);
	}
	
	public void run() {
	
		pstatspath = prefs.name + ".pstats";
		
		if(listenLocal == true) {
			listeners.put(0, System.out);
		}
		
		try {
			if(prefs.getGnuBgPort() == 0) {
				systemPrinter.printDebugln("NOT starting gnubg");
				setFibsmode(3);
			} else {

				initGnuBg();
				
				initBgSocket();
				
				initPlayerStats();
				statview = new StatWriter(this);
				
				initFibs();
				
				keepAliveFibs();

			}
			

		} catch (Exception e) {
			systemPrinter.printDebugln(this.getClass().toString() 
									+ "(runServer): " + e);
			e.printStackTrace();
		}
		
		if(listenLocal == true) {
			listen(System.in, System.out);
		}
	}
	
	
	public void listen(InputStream in, OutputStream out) {
		
		boolean exit = true;
		int listenerId = listeners.size();
		
		
		contd = false;
		//Scanner scanner = new java.util.Scanner(System.in);
		BufferedReader input = new BufferedReader(new InputStreamReader(in));
		//PrintWriter output = new PrintWriter(out);
		PrintStream output = new PrintStream(out);
		if(out.equals(System.out) == false) {
			listeners.put(listenerId, output);
		}
		String line = "";
		for(;;) {
			
			if(line.equals("exit")) 
				break;
			
			if(line.equals("close") && in.equals(System.in) == false) {
				exit = false;
				break;
			}
			
			if(contd) {
				systemPrinter.printDebug(PROMPT, output);
			}
			
			
			///line = scanner.nextLine();
			try {
				line = input.readLine();
			} catch(IOException e) {
				exit = false;
				break;
			}
			if(line == null) {
				exit = false;
				break;
			}
			contd = true;

			if(line.equals("2")) {
				writer = gnubgout;
				outputName = "gnubgout";
			} else if(line.equals("1")) {
				writer = fibsout;
				outputName = "fibsout";
			}  else if(line.equals("6")) {
				writer = bgsocket.out;
				outputName = "bgsocket.out";
			}  else if(line.equals("8")) {
				if(fibs.lastboard != null) {
					bgsocket.out.printf("%s", fibs.lastboard.trim() + "\r\n");
				}
				continue;
			} else if(line.trim().equals("")) { 
				continue;
			}  else if(line.equals("16")) {
				statview.dumpPlayerStats("", 0);
				continue;
			}  else if(line.equals("17")) {
				statview.dumpPlayerStats("", 1);
				continue;
			}   else if(line.split(" ")[0].equals("200")) {
				String host = line.split(" ")[1];
				printDebug("TOR CHECHK [" + host + "] result: " 
						+ TorExitNodeChecker.isTorExitNode(host));
				;
				continue;
			}  else if(line.equals("166")) {
				removePlayerStat("marekful");
				continue;
			}  else if(line.split(" ")[0].equals("16")) {
				if(!line.split(" ")[1].trim().equals("")) 
					statview.dumpPlayerStats(line.split(" ")[1], 0);
				continue;
			}  else if(line.split(" ")[0].equals("17")) {
				if(!line.split(" ")[1].trim().equals("")) 
					statview.dumpPlayerStats(line.split(" ")[1], 1);
				continue;
			}  else if(line.split(" ")[0].equals("18")) {
				if(!line.split(" ")[1].trim().equals("")) 
					statview.dumpPlayerStats(line.split(" ")[1], 3);
				continue;
			}  else if(line.equals("111")) {
				output.println(prefs.showPrefs());
				continue;
			}  else if(line.equals("88")) {
				resendLastBoard();
			} else if(line.equals("31")) {
				prefs.setAutoinvite( (prefs.isAutoinvite()) ? false : true );
				printDebug("autoinvite is now: " + prefs.isAutoinvite());
				output.println();
				continue;
			} else if(line.equals("32")) {
				prefs.setAutojoin((prefs.isAutojoin()) ? false : true );
				printDebug("autojoin is now: " + prefs.isAutojoin());
				output.println();
				continue;
			} else if(line.equals("9")) {
				
				if(fibs.match != null) {
					if(fibs.match.isCrawford()){
						fibs.match.setCrawford(false); 
						printDebug("crawford is now: " + fibs.match.isCrawford());
					} else { 
						fibs.match.setCrawford(true);
						printDebug("crawford is now: " + fibs.match.isCrawford());
					}
				}
				continue;
			}  else if(line.equals("7")) {
				fibs.getSavedMatches();
				continue;
			}   else if(line.equals("29")) {
				fibs.procGPcounter = 0;
				continue;
			}  else if(line.equals("3")) {
				fibs.setShowMsgType(0, true);
				continue;
			}  else if(line.split(" ")[0].equals("3")) {
				String [] split = line.split(" ");
				int index = Integer.parseInt(split[1]);
				fibs.setShowMsgType(index, false);
				continue;
			} else if(line.equals("4")) {
				fibs.login();
				continue;
			} else if(line.equals("refibs")) {
				restartFibs();
				continue;
			} else if(line.equals("uptime")) {
				output.println(serverStartedAt);
				continue;
			} else if(line.equals("stat")) {
				output.println("started at " + serverStartedAt + " match# " 
									+ fibs.matchCount + " fibs# " + fibsCount 
									+ " GPc# " + fibs.procGPcounter);
				continue;
			} else if (line.equals("19")) {
				output.println(prefs.getPreferredOpps().toString());
				
				continue;
			} else {
				continue;
			}
		
			output.print(outputName + "$ ");
			output.flush();
			contd = false;
			///line = scanner.nextLine();
			try {
				line = input.readLine();
			} catch(IOException e) {
				exit = false;
				break;
			}
			
			try {
				writer.println(line);
			} catch(NullPointerException e) {
				//
			} catch(Exception e) {
				printDebug("Exception in runServer(): " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		if(exit == true) {
			stopInviter();
			stopBgSocket();
			stopGnubg();
			keepalivetimertask.cancel();
			fibs.terminate();
			if(gnubg == null && bgsocket == null) {
				printDebug("waiting fibs 0");
				while(fibs != null || !fibs.dead) {}
			} else {
				printDebug("waiting fibs 1");
				while(!(bgsocket.dead && gnubg.dead && (fibs == null || fibs.dead))) {}
			}
			printDebug("Exiting MatchDog Server");
			printDebug("bye");
			output.println();
			System.exit(0);
		}
		if(out.equals(System.out) == false) {
			listeners.remove(listenerId);
		}
	}

	private synchronized void initFibs() {
		systemPrinter.printDebugln("Initialising fibs");
		fibs = new FibsRunner(this, getFibsHost(), getFibsPort(), fibsCount);
		
		fibs.start();				

		systemPrinter.printDebugln("Connecting to fibs ");
		
		try {
			while(!fibs.init) { // init is 
			    this.wait();
			}
			fibsCount++;
			fibsos = fibs.s.getOutputStream();	
			
		} catch(InterruptedException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		fibsout = new PrintWriter(fibsos, true);
		fibs.outOK = true;
		synchronized (fibs) {
			fibs.notify();
		}
	}
	
	protected synchronized void restartFibs() {
		fibs.terminate();

		try {
			
			while(fibs != null) {
				printDebug("Waiting for fibs to quit");
				this.wait();
			}
			
		} catch(Exception e) {
			printDebug("Exception in restartFibs()" + e.getMessage());
		}
		initFibs();
	}


	protected synchronized void initGnuBg() {

		systemPrinter.printDebugln("Initialising gnubg");
		gnubg = new BGRunner(getGnuBgCmd(), this, prefs.getGnuBgPort());
		gnubg.start();	
		
		systemPrinter.printDebugln("Waiting for gnubg ");
		try {
			while(!gnubg.init) {
				this.wait();
			};
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		systemPrinter.printDebugln("gnubg running");

		gnubgos = gnubg.p.getOutputStream();	
		gnubgout = new PrintWriter(gnubgos, true);
		
		int checkquerply = prefs.getCheckquerply();
		int cubedecply = prefs.getCubedecply();
		gnubgout.println("show version");
		
		gnubgout.println("set eval sameasanalysis off");
		
		if(prefs.getMaxml() == 1) {
			gnubgout.println("set evaluation chequer eval cubeful off");
		}
		gnubgout.println("set threads 8");
		gnubgout.println("set evaluation chequer eval plies " + checkquerply);
		gnubgout.println("set evaluation cubedecision eval plies " + cubedecply);
		
		for(int i = 0; i < 10; i++) {
			gnubgout.println("set evaluation movefilter " + prefs.getMoveFilter(i) );
			printDebug(prefs.getMoveFilter(i));
		}
		
		gnubgout.println("external localhost:" + prefs.getGnuBgPort());
		
	}

	protected void stopGnubg() {
		if(gnubg == null)
			return;
		gnubg.terminate();
		printDebug("Gnubg terminated");
	}
	
	protected void initBgSocket() {
		systemPrinter.printDebugln("Initialising bg socket");
		bgsocket = new BGSocket(this);
		systemPrinter.printDebugln("Connecting bg socket");
		bgsocket.connect();
		bgsocket.start();
	}
	
	protected void stopBgSocket() {
		if(bgsocket == null)
			return;
		try {
			bgsocket.s.close();
			bgsocket.terminate();	
		} catch (IOException e) {

			e.printStackTrace();
		}			
		printDebug("Socket terminated");
		
	}
	
	protected void resendLastBoard() {
		fibsout.println("b");
		printDebug("Resending last board: " + fibs.lastboard);
		fibs.sleepFibs(600);
		bgsocket.out.printf("%s", fibs.lastboard.trim() + "\r\n");
	}

	protected void initPlayerStats() {
		printDebug("OPENING PlayerStats file...");

		if (openPlayerStats(pstatspath)) {
			printDebug("OK (" + pstatspath + ")");
		} else {
			printDebug("FAILED, CREATING EMPTY PlayerStats ("
					+ "will be written when first match stops --> " + pstatspath
					+ ")");
			playerstats = new PlayerStats();
		}
	}
	
	protected void startInviter(String name, int ml) {
		if(fibs.match != null || fibsmode == 2) 
			return;
		if(inviter == null) {
			inviter = new Inviter(this, name, ml);
		}
	}
	
	protected void stopInviter() {
		if(inviter != null) {
			inviter.setWait(false);
			inviter.t.cancel();
			
			printDebug("Inviter terminated");	
			inviter = null;
		}		
	}
	
	private void keepAliveFibs() {
		
		if(keepalivetimertask != null) {
			keepalivetimer.cancel();
		}
		
		keepalivetimer = new Timer();
		keepalivetimertask = new TimerTask() {
			
			@Override 
			public void run() {
				
				printDebug("KeepAlive");
				printDebug("KeepAlive(" + keepalivetimer.hashCode() + ") " 
						+ fibs.s.isInputShutdown() + " - " + fibs.s.isOutputShutdown() + " - "
						+ fibs.terminating + " | last fibsline: " + fibs.getFibsLastLineSecondsAgo() + " sec");
				
				if(fibs.terminating == true) {
					return;
				}
				
				// fibs.input.ensureOpen()
				
				if(fibs.getFibsLastLineSecondsAgo() > FibsRunner.timoutSeconds && fibs.init == true) {
					printDebug(" *** No line from fibs for " + FibsRunner.timoutSeconds + " seconds, RESTARTING ***");
					restartFibs();
				}
				
			}
		};	
		keepalivetimer.schedule(keepalivetimertask, 48000L, 28000L);
	}
	
	/*protected void print(String msg, PrintStream os) {
		os.print(UnixConsole.LIGHT_WHITE);
		os.print(UnixConsole.BACKGROUND_RED);
		os.print("system:" + UnixConsole.RESET + " ");				
		os.print(msg);
		os.flush();
	}
	
	protected void print(String msg) {
		for(PrintStream os : listeners.values()) {
			print(msg, os);
		}
	}
	
	protected void println(String msg, PrintStream os) {
		os.println();
		print(msg, os);
	}
	
	protected void println(String msg) {
		for(PrintStream os : listeners.values()) {
			println(msg, os);
		}
	}*/
	
	protected void printDebug(String str) {
		int c = 0;
		if(fibs != null) {
			c = fibs.procGPcounter;
		}
		printer.printDebugln(str, "MatchDog[" + c + "]");
	}
	
	public boolean openPlayerStats(String path) {
	      
	      
	    try {
			FileInputStream fin = new FileInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(fin);
			playerstats = (PlayerStats) ois.readObject();
			ois.close();
		} catch (IOException e) {
			printDebug(e.toString());
			//e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			printDebug(e.toString());
			//e.printStackTrace();
			return false;
		} catch (Exception e) {
			printDebug(e.toString());
			//e.printStackTrace();
			return false;
		}

		return true;
	}
		
	boolean writePlayerStats(String filepath) {
		
	 try {
	      FileOutputStream fout = new FileOutputStream(filepath);
	      ObjectOutputStream oos = new ObjectOutputStream(fout);
	      oos.writeObject(playerstats);
	      oos.close();
	      return true;
	 } catch (Exception e) { e.printStackTrace(); return false; }	
	}

	public Map<Integer, String> getSavedMatches() {
		return savedMatches;
	}

	public void setSavedMatches(Map<Integer, String> savedMatches) {
		this.savedMatches = savedMatches;
	}

	public int getFibsmode() {
		return fibsmode;
	}

	public void setFibsmode(int fibsmode) {
		this.fibsmode = fibsmode;
	}
	
	public String getFibsHost() {
		return MatchDogServer.fibshost;
	}	
	public int getFibsPort() {
		return MatchDogServer.fibsport;
	}	
	public String getFtpScriptPath() {
		return MatchDogServer.ftpscriptpath;
	}	
	public String getGnuBgCmd() {
		return MatchDogServer.gnubgcmd;
	}
	public String getPlatformName() {
		return MatchDogServer.platform;
	}
	public String getVersion() {
		return MatchDogServer.version;
	}	
	public String getLastopp() {
		return lastopp;
	}

	public void setLastopp(String lastopp) {
		this.lastopp = lastopp;
	}

	public Date getLastfinish() {
		return lastfinish;
	}

	public void setLastfinish(Date lastfinish) {
		this.lastfinish = lastfinish;
	}

	public String getPstatspath() {
		return pstatspath;
	}

	public void removePlayerStat(String player) {
		playerstats.pstats.remove(player);
	}
	
	public double getOwnRating() {
		return ownRating;
	}

	public void setOwnRating(double ownRating) {
		this.ownRating = ownRating;
	}
	
	public boolean isBlacklisted(String name) {
		return bl.containsValue(name);
	}
	
	public Collection<PrintStream> getPrintStreams() {
		return listeners.values();
	}

}

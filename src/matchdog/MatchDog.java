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


import jcons.src.com.meyling.console.Console;
import jcons.src.com.meyling.console.ConsoleBackgroundColor;
import jcons.src.com.meyling.console.ConsoleFactory;
import jcons.src.com.meyling.console.ConsoleForegroundColor;
import jcons.src.com.meyling.console.UnixConsole;

import etc.TorExitNodeChecker;

public class MatchDog extends Thread  {
	
	static Console console = ConsoleFactory.getConsole();
	
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
	boolean contd;
	
	HashMap<Integer, String> bl = new HashMap<Integer, String>();
	HashMap<String, String> statrequests = new HashMap<String, String>();
	
	protected int fibsmode;	
	
	MatchDog(PlayerPrefs prefs, HashMap<Integer, String> bl) {
		
		this.prefs = prefs;
		
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
		
		console = ConsoleFactory.getConsole();
		fibsmode = 0;
		fibsCount = 0;
		
		savedMatches = new HashMap<Integer, String>();
		playerstats = null;
		ownRating = 0;
		outputName = "";
		serverStartedAt = new Date();
		
		// global blacklist
		this.bl = bl;
	}
	
	public void run() {
	
		pstatspath = prefs.name + ".pstats";
		String line = "";
		Scanner scanner = new java.util.Scanner(System.in);

		try {
			if(prefs.getGnuBgPort() == 0) {
				print("NOT starting gnubg");
				setFibsmode(3);
			} else {
				synchronized (this) {
					initGnuBg();
					while(!gnubg.init) {
						this.wait();
					}
					
					initBgSocket();
					while(!bgsocket.run) {
						this.wait();
					}
					
					initPlayerStats();
					statview = new StatWriter(this);
					
					initFibs();
					
				}
			}
			

		} catch (Exception e) {
			System.out.println(this.getClass().toString() 
									+ "(runServer): " + e);
			e.printStackTrace();
		}
		
		contd = false;
		for(;;) {
			
			if(line.equals("exit")) 
				break;
			
			if(contd) {
				print(PROMPT);
			}
			
			line = scanner.nextLine();
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
				System.out.println(prefs.showPrefs());
				continue;
			}  else if(line.equals("88")) {
				resendLastBoard();
			} else if(line.equals("31")) {
				prefs.setAutoinvite( (prefs.isAutoinvite()) ? false : true );
				printDebug("autoinvite is now: " + prefs.isAutoinvite());
				continue;
			} else if(line.equals("32")) {
				prefs.setAutojoin((prefs.isAutojoin()) ? false : true );
				printDebug("autojoin is now: " + prefs.isAutojoin());
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
				fibsout.println("show saved");
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
				System.out.println(serverStartedAt);
				continue;
			} else if(line.equals("stat")) {
				System.out.println("started at " + serverStartedAt + " match# " 
									+ fibs.matchCount + " fibs# " + fibsCount 
									+ " GPc# " + fibs.procGPcounter);
				continue;
			} else if (line.equals("19")) {
				System.out.println(prefs.getPreferredOpps().toString());
				
				continue;
			} else {
				continue;
			}
		
			System.out.print(outputName + "$ ");
			contd = false;
			line = scanner.nextLine();
			
			try {
				writer.println(line);
			} catch(NullPointerException e) {
				//
			} catch(Exception e) {
				printDebug("Exception in runServer(): " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		stopInviter();
		stopBgSocket();
		stopGnubg();
		fibs.terminate();
		if(gnubg == null && bgsocket == null) {
			printDebug("waiting fibs");
			while(fibs != null || !fibs.dead) {}
		} else {
			printDebug("waiting fibs");
			while(!(bgsocket.dead && gnubg.dead && fibs.dead)) {}
		}
		printDebug("Exiting MatchDog Server");
		printDebug("bye");
		System.out.println();
		System.exit(0);
	}


	private void initFibs() {
		System.out.println();
		print("Initialising fibs");
		fibs = new FibsRunner(this, getFibsHost(), getFibsPort());
		
		fibs.start();				

		System.out.println();
		print("Connecting to fibs ");
		
		int c = 0;
		while(!fibs.init) {
		    fibs.sleepFibs(500);
		    
		    if(c % 10 == 0) {
		    	System.out.print(". ");
		    }
		    c++;
		}
		fibsCount++;
		
		try {
			fibsos = fibs.s.getOutputStream();	
		} catch(IOException e) {
			e.printStackTrace();
		}
		fibsout = new PrintWriter(fibsos, true);
		fibs.outOK = true;
	}
	
	protected void restartFibs() {
		fibs.terminate();
		initFibs();
	}


	protected void initGnuBg() {
		System.out.println();
		print("Initialising gnubg");
		gnubg = new BGRunner(getGnuBgCmd(), this, prefs.getGnuBgPort());
		gnubg.start();	
		
		System.out.println();
		print("Waiting for gnubg ");
		int c = 0, cc = 0;
		while(!gnubg.init) {
			c++;
			if(c % 100000000 == 1) {
				cc++;
				System.out.print(".");
			}
		};

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
		bgsocket = new BGSocket(this);
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
		printDebug("lastboard: " + fibs.lastboard);
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
	
	private static void print(String msg) {
		
		console.setForegroundColor(ConsoleForegroundColor.WHITE);
		console.setBackgroundColor(ConsoleBackgroundColor.DARK_RED);
		System.out.print("system: " + UnixConsole.RESET );				
		System.out.print(msg);
		
	}
	
	protected void printDebug(String str) {
		System.out.println();
		console.setForegroundColor(ConsoleForegroundColor.BLACK);
		console.setBackgroundColor(ConsoleBackgroundColor.GREY);
		int c = 0;
		if(fibs != null) {
			c = fibs.procGPcounter;
		}
		System.out.print("MatchDog[" + c + "]: " + UnixConsole.RESET );
	
		System.out.print( str );
		
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
	public String getTelnetCmd() {
		return MatchDogServer.telnetcmd;
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

}

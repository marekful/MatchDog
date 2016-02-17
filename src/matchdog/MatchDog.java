package matchdog;

import etc.TorExitNodeChecker;
import jcons.src.com.meyling.console.UnixConsole;

import java.io.*;
import java.util.*;

public class MatchDog extends Prefs implements Runnable, PrintableStreamSource {
	
	protected static final String PROMPT = "$ ";
    static final Object lock = new Object();

	BGRunner gnubg;
	FibsRunner fibs;
	OutputStream gnubgos;
	PrintWriter gnubgout;
	OutputStream fibsos;
	PrintWriter fibsout, writer;
	Inviter inviter;
    ProgramPrefs programPrefs;
	PlayerPrefs prefs;
	Map<Integer, String> savedMatches;
	//HashMap<String, HashMap<Integer, String>> savedMatches;
	String lastopp;
	int fibsCount, matchCount;
	Date serverStartedAt, lastfinish;
	PlayerStats playerstats;
	StatWriter statview;
	String pstatspath;
	String outputName;
    String hostName;

    double ownRating;
	boolean listenLocal;
	
	HashMap<Integer, String> bl = new HashMap<Integer, String>();
	HashMap<String, String> statrequests = new HashMap<String, String>();
	
	Timer keepalivetimer;
	TimerTask keepalivetimertask;
	HashMap<Integer, PrintStream> listeners = new HashMap<Integer, PrintStream>();
	
	protected int fibsmode;

    boolean contd, welcome;
	
	BufferedDebugPrinter printer, systemPrinter;
	
	MatchDog(ProgramPrefs progPrefs, PlayerPrefs prefs, HashMap<Integer, String> bl, String hostName, boolean listenLocal) {

        this.programPrefs = progPrefs;
		this.prefs = prefs;
		this.listenLocal = listenLocal;
        this.hostName = hostName;
		
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
		matchCount = 0;
		
		savedMatches = new HashMap<Integer, String>();
		playerstats = null;
		ownRating = 0;
		outputName = "";
		serverStartedAt = new Date();
		
		// global blacklist
		this.bl = bl;
		
		printer = new BufferedDebugPrinter(
			this, "MatchDog:", UnixConsole.BLACK, UnixConsole.BACKGROUND_WHITE
		);
		systemPrinter = new BufferedDebugPrinter(
			this, "system:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_RED
		);
	}
	
	public void run() {
	
		pstatspath = prefs.username + ".pstats";
		
		if(listenLocal) {
			listeners.put(0, System.out);
		}
		
		try {
			if(prefs.getGnuBgPort() == 0) {
				systemPrinter.printDebugln("NOT starting gnubg");
				setFibsmode(3);
			} else {

				initGnuBg();
				
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
		
		if(listenLocal) {
			listen(System.in, System.out);
		}
	}

    private String getPrompt() {
        return prefs.getUsername() + "@" + hostName + PROMPT;
    }

    private void leaveShell(PrintStream os) {
        contd = false;
        welcome = true;
        os.print(new char[]{27, 91, 65});
    }
	
	public void listen(InputStream in, PrintStream out) {
		
		boolean exit = true;
        contd = false;
        welcome = true;
		int listenerId = listeners.size();

		BufferedReader input = new BufferedReader(new InputStreamReader(in));
		PrintStream output = out;

		if(!out.equals(System.out)) {
			listeners.put(listenerId, output);
		}

        try {
            String line = "";
            for (; ; ) {

                if (contd) {
                    if (welcome) {
                        printWelcome(output);
                        output.print(getPrompt());
                        welcome = false;
                    } else {
                        output.print(getPrompt());
                    }
                    suspendOutput(output);
                } else {
                    unSuspendOutput(output);
                }

                try {
                    line = input.readLine();
                } catch (IOException e) {
                    exit = false;
                    break;
                }

                if(out.checkError()) {
                    exit = false;
                    break;
                }

                // enter shell
                if (line != null && !contd) {
                    contd = true;
                    continue;
                }

                // when socket read fails, fibs will restart
                if (line == null) {
                    exit = false;
                    break;
                }

                // empty line, new shell prompt
                if (line.equals("")) {
                    continue;
                }

                if (line.length() == 1) {
                    int code = (int) line.charAt(0);
                    output.print(code);
                    switch (code) {
                        // exit shell
                        case 27:
                            //output.print("Leaving MatchDog Shell");
                            leaveShell(output);
                            continue;
                    }
                }

                contd = true;

                if (line.equals("exit")) {
                    unSuspendOutput(output);
                    break;
                }
                if (line.equals("close") && !in.equals(System.in)) {
                    exit = false;
                    break;
                }

                if (line.equals("2")) {
                    writer = gnubg.getProcOut();
                    outputName = "gnubgout";
                } else if (line.equals("1")) {
                    writer = fibsout;
                    outputName = "fibsout";
                } else if (line.equals("6")) {
                    writer = gnubg.getScokOut();
                    outputName = "bgsocket.out";
                } else if (line.equals("8")) {
                    if (fibs.lastboard != null) {
                        gnubg.execBoard(fibs.lastboard.trim());
                    }
                    continue;
                } else if (line.equals("16")) {
                    statview.dumpPlayerStats("", 0);
                    continue;
                } else if (line.equals("17")) {
                    statview.dumpPlayerStats("", 1);
                    continue;
                } else if (line.split(" ")[0].equals("200")) {
                    String host = line.split(" ")[1];
                    printDebug("TOR CHECHK [" + host + "] result: "
                            + TorExitNodeChecker.isTorExitNode(host));
                    continue;
                } else if (line.equals("166")) {
                    removePlayerStat("marekful");
                    continue;
                } else if (line.split(" ")[0].equals("16")) {
                    if (!line.split(" ")[1].trim().equals(""))
                        statview.dumpPlayerStats(line.split(" ")[1], 0);
                    leaveShell(output);
                    continue;
                } else if (line.split(" ")[0].equals("17")) {
                    if (!line.split(" ")[1].trim().equals(""))
                        statview.dumpPlayerStats(line.split(" ")[1], 1);
                    leaveShell(output);

                    continue;
                } else if (line.split(" ")[0].equals("18")) {
                    if (!line.split(" ")[1].trim().equals(""))
                        statview.dumpPlayerStats(line.split(" ")[1], 3);
                    leaveShell(output);
                    continue;
                } else if (line.equals("111")) {
                    output.print(prefs.showPrefs());
                    output.println();
                    leaveShell(output);
                    continue;
                } else if (line.equals("88")) {
                    resendLastBoard();
                    leaveShell(output);
                    continue;
                } else if (line.equals("31")) {
                    prefs.setAutoinvite((prefs.isAutoinvite()) ? false : true);
                    output.print("autoinvite is now: " + prefs.isAutoinvite());
                    output.println();
                    leaveShell(output);
                    continue;
                } else if (line.equals("32")) {
                    prefs.setAutojoin((prefs.isAutojoin()) ? false : true);
                    output.print("autojoin is now: " + prefs.isAutojoin());
                    output.println();
                    leaveShell(output);
                    continue;
                } else if (line.equals("9")) {

                    if (fibs.match != null) {
                        if (fibs.match.isCrawford()) {
                            fibs.match.setCrawford(false);
                            output.print("crawford is now: " + fibs.match.isCrawford());
                            output.println();
                        } else {
                            fibs.match.setCrawford(true);
                            output.print("crawford is now: " + fibs.match.isCrawford());
                            output.println();
                        }
                    }
                    continue;
                } else if (line.equals("5")) {
                    fibsout.println("whois " + prefs.getUsername());
                    leaveShell(output);
                    continue;
                } else if (line.equals("7")) {
                    fibs.getSavedMatches();
                    leaveShell(output);
                    continue;
                } else if (line.equals("29")) {
                    fibs.procGPcounter = 0;
                    continue;
                } else if (line.equals("3")) {
                    fibs.setShowMsgType(0, true);
                    continue;
                } else if (line.split(" ")[0].equals("3")) {
                    String[] split = line.split(" ");
                    int index = Integer.parseInt(split[1]);
                    fibs.setShowMsgType(index, false);
                    continue;
                } else if (line.equals("4")) {
                    fibs.login();
                    leaveShell(output);
                    continue;
                } else if (line.equals("refibs")) {
                    leaveShell(output);
                    unSuspendOutput(output);
                    restartFibs();
                    continue;
                } else if (line.equals("uptime")) {
                    output.print(TimeAgo.fromMs(System.currentTimeMillis() - serverStartedAt.getTime()));
                    output.println();
                    continue;
                } else if (line.equals("stat")) {
                    output.print("uptime: "
                            + TimeAgo.fromMs(System.currentTimeMillis() - serverStartedAt.getTime())
                            + " | match# "
                            + matchCount + " fibs# " + fibsCount
                            + " pGP# " + fibs.procGPcounter);
                    output.println();
                    continue;
                } else if (line.equals("19")) {
                    output.print(prefs.getPreferredOpps().toString());
                    output.println();
                    continue;
                } else {
                    output.print(getPrompt() + "Unknown command: '" + line + "' (" + line.length() + ") --> ");
                    for(int c = 0; c < line.length(); c++) {
                        output.print((int)line.charAt(c) + " ");
                    }
                    output.println();
                    continue;
                }

                output.print(outputName + "$ ");
                output.flush();
                contd = false;
                welcome = true;

                try {
                    line = input.readLine();
                } catch (IOException e) {
                    exit = false;
                    break;
                }
                output.print("Leaving MatchDog Shell");

                try {
                    writer.println(line);
                } catch (NullPointerException e) {
                    //
                } catch (Exception e) {
                    printDebug("Exception in runServer(): " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        catch(NullPointerException e) {
            System.out.println(" MD.listen NPE ");
            e.printStackTrace();
            exit = false;
        }
        catch(Exception e) {
            System.out.println(" MD.listen E ");
            e.printStackTrace();
            exit = false;
        }
		
		if(exit) {
            stopServer();
		}
		if(!out.equals(System.out)) {
			listeners.remove(listenerId);
            BufferedDebugPrinter.removeOutputBuffer(output);
		}
	}

    protected void stopServer() {
        stopInviter();
        stopGnubg();
        keepalivetimertask.cancel();
        fibs.terminate();

        systemPrinter.printDebugln("Exiting MatchDog Server");
        systemPrinter.printDebugln("bye");
        systemPrinter.printDebugln("", "");
        System.exit(0);
    }

	private void initFibs() {

		systemPrinter.printDebugln("Initialising fibs");
		fibs = new FibsRunner(this, programPrefs.getFibshost(), programPrefs.getFibsport(), fibsCount);
		
		fibs.start();				

		systemPrinter.printDebugln("Connecting to fibs ");

        synchronized (lock) {
            try {
                while (!fibs.init) { // init is
                    lock.wait();
                }
                fibsCount++;
                fibsos = fibs.s.getOutputStream();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		fibsout = new PrintWriter(fibsos, true);
		fibs.outOK = true;
		synchronized (FibsRunner.lock) {
            FibsRunner.lock.notify();
		}
	}
	
	protected synchronized void restartFibs() {

        fibs.terminate();
		initFibs();
	}


	protected void initGnuBg() {
        systemPrinter.printDebugln("Initialising gnubg...");
        gnubg = new BGRunner(programPrefs.getGnubgCmdArr(), this);

        if(!gnubg.launch()) {
            stopServer();
            return;
        }
        gnubg.setup();

        gnubg.connectSocket();
	}

	protected void stopGnubg() {
		if(gnubg == null)
			return;
		gnubg.terminate();
        systemPrinter.printDebugln("Gnubg terminated");
	}
	
	protected void resendLastBoard() {
		fibsout.println("b");
		printDebug("Resending last board: " + fibs.lastboard);
		fibs.sleepFibs(600);
		gnubg.execBoard(fibs.lastboard.trim());
	}

	protected void initPlayerStats() {
		printDebug("OPENING PlayerStats file...");

		if ((playerstats = openPlayerStats(pstatspath)) != null) {
			printDebug("OK (" + pstatspath + ")");
            mergePlayerStats();
		} else {
			printDebug("FAILED, CREATING EMPTY PlayerStats ("
					+ "will be written when first match stops --> " + pstatspath
					+ ")");
			playerstats = new PlayerStats();
		}
	}

    private void mergePlayerStats() {
        String importPath = pstatspath + ".import";
        PlayerStats importPs;

        if ((importPs = openPlayerStats(importPath)) == null) {
            return;
        }

        printDebug("IMPORTING from .pstat.import file (" + importPath + ")");

        int mc = 0;
        for(String player : importPs.pstats.keySet()) {
            PlayerStats.PlayerStat ps = importPs.pstats.get(player);
            printDebug(" ** Player: " + player + " history size: " + ps.history.size());

            if(ps.history.size() == 0) continue;

            if(playerstats.hasPlayer(player)) {
                printDebug(" ** Appending to existing player");
            } else {
                printDebug(" ** Player doesn't exist, creating");
                playerstats.cratePlayer(player);
            }
            printDebug(" ** Found "
                    + ps.history.size() + " match(es) to import in addition to "
                    + playerstats.getByName(player).history.size()
                    + " existing match(es)");

            //playerstats.getByName(player).history.putAll(ps.history);
            printDebug("");

            MatchLog toImport, existing;
            for(Date d : ps.getHistory().keySet()) {
                toImport = ps.getHistory().get(d);
                existing = playerstats.getByName(player).getHistory().get(d);

                printDebug(" ** Checking match " + d);
                //printDebug(" **   import - ml: " + toImport.length + " score: " + toImport.finalscore
                //        + " game count: " + toImport.gamecount + " time: " + toImport.getTotalTimeStr(0));

                if(existing != null) {
                    //printDebug(" **   existing - ml: " + existing.length + " score: " + existing.finalscore
                    //        + " game count: " + existing.gamecount + " time: " + existing.getTotalTimeStr(0));
                } else {
                    printDebug(" ** IMPORTING MATCH ** ** ** ");
                    playerstats.getByName(player).putMatch(d, toImport);
                    mc++;
                }
            }

            printDebug("");
        }

        if(mc > 0) {
            printDebug(" ** Imported " + mc + " match(es), WRITING PSTATS FILE...");
            writePlayerStats(getPstatspath());
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
			
			Date firstRun = null;
			
			@Override 
			public void run() {
				
				if(firstRun == null) {
					firstRun = new Date();
				}
				
				try {
					if(fibs.terminating) {
						return;
					}

                    if(fibs.getFibsLastLineSecondsAgo() > FibsRunner.timoutSeconds / 3 * 2) {
                        printDebug("KeepAlive(" + keepalivetimer.hashCode() + ") "
                            + fibs.s.isInputShutdown() + " - " + fibs.s.isOutputShutdown() + " - "
                            + fibs.terminating + " | last fibsline: " + fibs.getFibsLastLineSecondsAgo() + " sec"
                            + " | started: " + ((new Date()).getTime() - firstRun.getTime()) / 1000 + " sec ago"
                        );
                    }
					
					if(fibs.getFibsLastLineSecondsAgo() > FibsRunner.timoutSeconds && fibs.loggedIn) {
						printDebug(" *** No line from fibs for " + FibsRunner.timoutSeconds + " seconds, RESTARTING ***");
						restartFibs();
					}
				} catch(NullPointerException e) {
					printDebug("KeepAlive - NullPointerException, probably fibs is restarting");
				} catch(Exception e) {
					printDebug("KeepAlive - Exception: " + e.getMessage());
				}
				
			}
		};	
		keepalivetimer.schedule(keepalivetimertask, 48000L, 28000L);
		
		keepalivetimer.schedule(new TimerTask() {
			@Override 
			public void run() {
				printDebug("KeepAlive+");
				fibs.keepAlive();
			}
		}, 300000L, 300000L);
	}

    protected void suspendOutput(PrintStream output) {
        fibs.linePrinter.setSuspended(output, true);
        systemPrinter.setSuspended(output, true);
        printer.setSuspended(output, true);
        gnubg.s_printer.setSuspended(output, true);
        fibs.matchinfoPrinter.setSuspended(output, true);
    }

    protected void unSuspendOutput(PrintStream output) {
        fibs.linePrinter.setSuspended(output, false);
        systemPrinter.setSuspended(output, false);
        printer.setSuspended(output, false);
        gnubg.s_printer.setSuspended(output, false);
        fibs.matchinfoPrinter.setSuspended(output, false);
    }

    protected void printWelcome(PrintStream output) {
        output.print(UnixConsole.LIGHT_WHITE);
        output.println("Entering MatchDog Shell - to select output enter number");
        output.println("1: fibs 2: gnubg 6: gnubg-external | for other commands ");
        output.println("see 'help', to leave the shell press ESC then ENTER");
        output.print(UnixConsole.RESET);
    }

    protected void printDebug(String str) {
		int c = 0;
		if(fibs != null) {
			c = fibs.procGPcounter;
		}
		printer.printDebugln(str, "MatchDog[" + c + "]:");
	}
	
	public PlayerStats openPlayerStats(String path) {
	    try {
			FileInputStream fin = new FileInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(fin);
			PlayerStats ps = (PlayerStats) ois.readObject();
			ois.close();
            return ps;
		} catch (IOException e) {
			printDebug(e.toString());
			//e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			printDebug(e.toString());
			//e.printStackTrace();
			return null;
		} catch (Exception e) {
			printDebug(e.toString());
			//e.printStackTrace();
			return null;
		}
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

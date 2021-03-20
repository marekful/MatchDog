package matchdog;

import etc.TorExitNodeChecker;
import jcons.src.com.meyling.console.UnixConsole;
import matchdog.console.printer.BufferedConsolePrinter;
import matchdog.console.printer.DefaultPrinter;
import matchdog.console.printer.MatchDogPrinter;
import sun.misc.Signal;

import java.io.*;
import java.util.*;

public class MatchDog extends Prefs implements Runnable, PrintableStreamSource {
	
	protected static final String PROMPT = "$ ";
    static final Object lock = new Object();

    String configDir;
    String dataDir;
    long pid;

	BGRunner bgRunner;
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
	
	Timer keepaliveTimer;
	TimerTask keepaliveTimerTask;
	HashMap<Integer, PrintStream> connectionListeners;
	HashMap<PrintStream, Boolean> inMatchDogShell;
	
	protected int fibsMode;

    boolean contd, welcome;
	
	MatchDogPrinter printer;
    DefaultPrinter systemPrinter;
    DefaultPrinter socketServerPrinter;

    Map<String, Boolean> debug;
	
	MatchDog(ProgramPrefs progPrefs,
             PlayerPrefs prefs,
             HashMap<Integer, String> bl,
             String hostName,
             boolean listenLocal,
             String configDir,
             String dataDir
    ) {
        this.programPrefs = progPrefs;
		this.prefs = prefs;
		this.listenLocal = listenLocal;
        this.hostName = hostName;
        this.configDir = configDir;
        this.dataDir = dataDir;
		
		bgRunner = null;
		fibs = null;
		gnubgos = null;
		gnubgout = null;
		fibsos = null;
		fibsout = null;
		writer = null;
		inviter = null;
		
		lastopp = null;
		lastfinish = null;

		fibsMode = 0;
		fibsCount = 0;
		matchCount = 0;
		
		savedMatches = new HashMap<Integer, String>();
		playerstats = null;
		ownRating = 0;
		outputName = "";
		serverStartedAt = new Date();

        connectionListeners = new HashMap<Integer, PrintStream>();
        inMatchDogShell = new HashMap<PrintStream, Boolean>();

        // global blacklist
		this.bl = bl;
		
		printer = new MatchDogPrinter(
			this, "MatchDog:", UnixConsole.BLACK, UnixConsole.BACKGROUND_WHITE
		);
        systemPrinter = new DefaultPrinter(
            this, "system:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_RED
        );
        socketServerPrinter = new DefaultPrinter(
            this, "socketServer[port=" + prefs.getListenerport() + "]:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_RED
        );

        debug = new HashMap<>();
        debug.put("printGnubgCommand", false);

        if(listenLocal) {
            connectionListeners.put(0, System.out);
        }

        pid = Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        systemPrinter.printLine("Started MatchDog server PID: " + pid)
                .setLabel("system[pid=" + pid + "]:");
    }

    public void run() {
	
		pstatspath = prefs.username + ".pstats";

		try {
			if(prefs.getGnuBgPort() == 0) {
				systemPrinter.printLine("NOT starting gnubg");
				setFibsMode(3);
			} else {

                bgRunner = new BGRunner(programPrefs.getGnubgCmdArr(), this);
				
				initPlayerStats();
				statview = new StatWriter(this);
				
				initFibs();
				
				keepAliveFibs();

			}

		} catch (Exception e) {
			systemPrinter.printLine(this.getClass().toString()
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
        inMatchDogShell.remove(os);
        os.print(new char[]{27, 91, 65});
    }

    private boolean inMatchDogShell(PrintStream p) {
	    if (!inMatchDogShell.containsKey(p)) {
	        return false;
        }
	    return inMatchDogShell.get(p);
    }

    public String getPlayerName() {
	    return prefs.getUsername();
    }

    public int getGPC() {
	    if (fibs == null) {
	        return -1;
        }
	    return fibs.procGPcounter;
    }

    public void listen(InputStream in, PrintStream out) {
		
		boolean exit = true;
        contd = false;
        welcome = true;
		int listenerId = connectionListeners.size();

		/*
		* Add SIGINT handler for each listener - stdin and/or socket connection(s)
		* (This requires the undocumented -XDignore.symbol.file to suppress
		* compiler warnings for using sun.* packages)
		*/
        Signal.handle(new Signal("INT"), signal -> {
            if (systemPrinter.isSuspended(out)) {
                unSuspendOutput(out);
            }
            if (inMatchDogShell(out)) {
                out.println("\nLeaving MatchDog Shell");
                leaveShell(out);
            } else {
                systemPrinter.printLine("Got signal " + signal);
                stopServer();
            }
        });

		BufferedReader input = new BufferedReader(new InputStreamReader(in));

        if(!out.equals(System.out)) {
			connectionListeners.put(listenerId, out);
		}

        try {
            String line = "";
            for (; ; ) {

                // ENTER/LEAVE MatchDog SHELL //
                /*
                * The default is the parent shell where every printer prints,
                * by sending newline (empty line + enter), printers are suspended
                * and we enter "MatchDog shell"
                * */
                if (contd) {
                    inMatchDogShell.put(out, true);
                    if (welcome) {
                        printWelcome(out);
                        out.print(getPrompt());
                        welcome = false;
                    } else {
                        out.print(getPrompt());
                    }
                    suspendOutput(out);
                } else {
                    inMatchDogShell.replace(out, false);
                    unSuspendOutput(out);
                }

                // READ INPUT / HANDLE ERROR / SHELL CONTROL
                try {
                    line = input.readLine();
                } catch (IOException e) {
                    exit = false;
                    break;
                }

                if (out.checkError()) {
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
                    // exit shell
                    if (line.charAt(0) == 27) {
                        out.println("Leaving MatchDog Shell");
                        leaveShell(out);
                        continue;
                    }
                }

                contd = true;

                //onLi

                if (line.equals("exit")) {
                    unSuspendOutput(out);
                    break;
                }
                if (line.equals("close") && !in.equals(System.in)) {
                    exit = false;
                    break;
                }

                if (line.equals("2")) {
                    writer = bgRunner.getProcOut();
                    outputName = "gnubgout";
                } else if (line.equals("1")) {
                    writer = fibsout;
                    outputName = "fibsout";
                } else if (line.equals("6")) {
                    writer = bgRunner.getScokOut();
                    outputName = "bgsocket.out";
                } else if (line.equals("8")) {
                    if (fibs.lastboard != null) {
                        bgRunner.execBoard(fibs.lastboard.trim());
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
                    printer.printLine("TOR CHECHK [" + host + "] result: "
                            + TorExitNodeChecker.isTorExitNode(host));
                    continue;
                } else if (line.equals("166")) {
                    removePlayerStat("marekful");
                    continue;
                } else if (line.split(" ")[0].equals("16")) {
                    if (!line.split(" ")[1].trim().equals(""))
                        statview.dumpPlayerStats(line.split(" ")[1], 0);
                    leaveShell(out);
                    continue;
                } else if (line.split(" ")[0].equals("17")) {
                    if (!line.split(" ")[1].trim().equals(""))
                        statview.dumpPlayerStats(line.split(" ")[1], 1);
                    leaveShell(out);

                    continue;
                } else if (line.split(" ")[0].equals("18")) {
                    if (!line.split(" ")[1].trim().equals(""))
                        statview.dumpPlayerStats(line.split(" ")[1], 3);
                    leaveShell(out);
                    continue;
                } else if (line.equals("111")) {
                    out.print(Arrays.stream(prefs.showPrefs()).sequential());
                    out.println();
                    leaveShell(out);
                    continue;
                } else if (line.equals("88")) {
                    resendLastBoard();
                    leaveShell(out);
                    continue;
                } else if (line.equals("31")) {
                    prefs.setAutoinvite(!prefs.isAutoinvite());
                    out.print("autoinvite is now: " + prefs.isAutoinvite());
                    out.println();
                    leaveShell(out);
                    continue;
                } else if (line.equals("32")) {
                    prefs.setAutojoin(!prefs.isAutojoin());
                    out.print("autojoin is now: " + prefs.isAutojoin());
                    out.println();
                    leaveShell(out);
                    continue;
                } else if (line.equals("9")) {

                    if (fibs.match != null) {
                        if (fibs.match.isCrawford()) {
                            fibs.match.setCrawford(false);
                            out.print("crawford is now: " + fibs.match.isCrawford());
                            out.println();
                        } else {
                            fibs.match.setCrawford(true);
                            out.print("crawford is now: " + fibs.match.isCrawford());
                            out.println();
                        }
                    }
                    continue;
                } else if (line.equals("5")) {
                    fibsout.println("whois " + prefs.getUsername());
                    leaveShell(out);
                    continue;
                } else if (line.equals("7")) {
                    fibs.getSavedMatches();
                    leaveShell(out);
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
                    leaveShell(out);
                    continue;
                } else if (line.equals("44")) {
                    bgRunner.closeSocket();
                    Runtime.getRuntime().exec("kill -SIGINT " + bgRunner.getPid());
                    leaveShell(out);
                    continue;
                } else if (line.equals("55")) {
                    debug.put("printGnubgCommand", true);
                    leaveShell(out);
                    continue;
                } else if (line.equals("refibs")) {
                    leaveShell(out);
                    unSuspendOutput(out);
                    restartFibs();
                    continue;
                } else if (line.equals("rebg")) {
                    leaveShell(out);
                    unSuspendOutput(out);
                    bgRunner.restartGnubg();
                    continue;
                } else if (line.equals("uptime")) {
                    out.print(TimeAgo.fromMs(System.currentTimeMillis() - serverStartedAt.getTime()));
                    out.println();
                    continue;
                } else if (line.equals("stat")) {
                    out.print("uptime: "
                            + TimeAgo.fromMs(System.currentTimeMillis() - serverStartedAt.getTime())
                            + " | match# "
                            + matchCount + " fibs# " + fibsCount
                            + " pGP# " + fibs.procGPcounter);
                    out.println();
                    continue;
                } else if (line.equals("log")) {
                    out.println(statview.dumpMatchlog());
                    leaveShell(out);
                    continue;
                } else if (line.startsWith("log ")) {
                    String[] split = line.split(" ");
                    int len = 10;
                    if(split.length > 1) {
                        try {
                            len = Integer.parseInt(split[1]);
                        } catch (NumberFormatException e) {}
                        out.println(statview.dumpMatchlog(len));
                        leaveShell(out);
                    }
                    continue;
                } else if (line.equals("19")) {
                    out.print(prefs.getPreferredOpps().toString());
                    out.println();
                    continue;
                } else {
                    out.print(getPrompt() + "Unknown command: '" + line + "' (" + line.length() + ") --> ");
                    for(int c = 0; c < line.length(); c++) {
                        out.print((int)line.charAt(c) + " ");
                    }
                    out.println();
                    continue;
                }

                out.print(outputName + "$ ");
                out.flush();
                contd = false;
                welcome = true;

                try {
                    line = input.readLine();
                } catch (IOException e) {
                    exit = false;
                    break;
                }
                out.print("Leaving MatchDog Shell");

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
			connectionListeners.remove(listenerId);
            BufferedConsolePrinter.removeOutputBuffer(out);
		}
	}

    protected void stopServer() {
        stopInviter();
        stopGnubg();
        keepaliveTimerTask.cancel();
        fibs.terminate();

        systemPrinter.printLine("Exiting MatchDog Server");
        systemPrinter.printLine("bye");
        systemPrinter.printLine("", "");
        System.exit(0);
    }

	private void initFibs() {

		systemPrinter.printLine("Initialising fibs");
		fibs = new FibsRunner(this, programPrefs.getFibsHost(), programPrefs.getFibsPort(), fibsCount);
		
		fibs.start();				

		systemPrinter.printLine("Connecting to fibs ");

        synchronized (lock) {
            try {
                // init is true when FIBS spited out its banner
                // and ready receive a login command
                while (!fibs.init) {
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

	protected void stopGnubg() {
		if(bgRunner == null)
			return;
		bgRunner.killGnubg(false);
		bgRunner = null;
        systemPrinter.printLine("Gnubg terminated");
	}

	public boolean isPlaying() {
	    return fibs.match != null;
    }

    public Match getMatch() {
	    return fibs.match;
    }
	
	protected void resendLastBoard() {
		fibsout.println("b");
		printer.printLine("Resending last board: " + fibs.lastboard);
		fibs.sleepFibs(600);
		bgRunner.execBoard(fibs.lastboard.trim());
	}

	protected void initPlayerStats() {
		printer.printLine("OPENING PlayerStats file...");

		if ((playerstats = openPlayerStats(pstatspath)) != null) {
			printer.printLine("OK (" + pstatspath + ")");
            mergePlayerStats();
		} else {
			printer.printLine("FAILED, CREATING EMPTY PlayerStats ("
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

        printer.printLine("IMPORTING from .pstat.import file (" + importPath + ")");

        int mc = 0;
        for(String player : importPs.pstats.keySet()) {
            PlayerStats.PlayerStat ps = importPs.pstats.get(player);
            printer.printLine(" ** Player: " + player + " history size: " + ps.history.size());

            if(ps.history.size() == 0) continue;

            if(playerstats.hasPlayer(player)) {
                printer.printLine(" ** Appending to existing player");
            } else {
                printer.printLine(" ** Player doesn't exist, creating");
                playerstats.cratePlayer(player);
            }
            printer.printLine(" ** Found "
                    + ps.history.size() + " match(es) to import in addition to "
                    + playerstats.getByName(player).history.size()
                    + " existing match(es)");

            //playerstats.getByName(player).history.putAll(ps.history);
            printer.printLine("");

            MatchLog toImport, existing;
            for(Date d : ps.getHistory().keySet()) {
                toImport = ps.getHistory().get(d);
                existing = playerstats.getByName(player).getHistory().get(d);

                printer.printLine(" ** Checking match " + d);
                //printer.printLine(" **   import - ml: " + toImport.length + " score: " + toImport.finalscore
                //        + " game count: " + toImport.gamecount + " time: " + toImport.getTotalTimeStr(0));

                if(existing != null) {
                    //printer.printLine(" **   existing - ml: " + existing.length + " score: " + existing.finalscore
                    //        + " game count: " + existing.gamecount + " time: " + existing.getTotalTimeStr(0));
                } else {
                    printer.printLine(" ** IMPORTING MATCH ** ** ** ");
                    playerstats.getByName(player).putMatch(d, toImport);
                    mc++;
                }
            }

            printer.printLine("");
        }

        if(mc > 0) {
            printer.printLine(" ** Imported " + mc + " match(es), WRITING PSTATS FILE...");
            writePlayerStats(getPstatspath());
        }
    }
	
	protected void startInviter(String name, int ml) {
		if(fibs.match != null || fibsMode == 2)
			return;
		if(inviter == null) {
			inviter = new Inviter(this, name, ml);
		}
	}
	
	protected void stopInviter() {
		if(inviter != null) {
			inviter.setWait(false);
			inviter.t.cancel();
			
			printer.printLine("Inviter terminated");
			inviter = null;
		}		
	}
	
	private void keepAliveFibs() {
		
		if(keepaliveTimerTask != null) {
			keepaliveTimer.cancel();
		}
		
		keepaliveTimer = new Timer();
		keepaliveTimerTask = new TimerTask() {
			
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
                        printer.printLine("KeepAlive(" + keepaliveTimer.hashCode() + ") "
                            + fibs.s.isInputShutdown() + " - " + fibs.s.isOutputShutdown() + " - "
                            + fibs.terminating + " | last fibsline: " + fibs.getFibsLastLineSecondsAgo() + " sec"
                            + " | started: " + ((new Date()).getTime() - firstRun.getTime()) / 1000 + " sec ago"
                        );
                    }
					
					if(fibs.getFibsLastLineSecondsAgo() > FibsRunner.timoutSeconds && fibs.loggedIn) {
						printer.printLine(" *** No line from fibs for " + FibsRunner.timoutSeconds + " seconds, RESTARTING ***");
						restartFibs();
					}
				} catch(NullPointerException e) {
					printer.printLine("KeepAlive - NullPointerException, probably fibs is restarting");
				} catch(Exception e) {
					printer.printLine("KeepAlive - Exception: " + e.getMessage());
				}
				
			}
		};	
		keepaliveTimer.schedule(keepaliveTimerTask, 48000L, 28000L);
		
		keepaliveTimer.schedule(new TimerTask() {
			@Override 
			public void run() {
				printer.printLine("KeepAlive+");
				fibs.keepAlive();
			}
		}, 300000L, 300000L);
	}

	protected ArrayList<BufferedConsolePrinter> getPrinters() {
        ArrayList<BufferedConsolePrinter> p = new ArrayList<>();

	    if (fibs.terminating) {
	        return p;
        }

        p.add(systemPrinter);
        p.add(socketServerPrinter);
        p.add(printer);
        p.add(bgRunner.matchPrinter);
        p.add(bgRunner.matchPrinter);
        p.add(bgRunner.eqPrinter);
        p.add(fibs.matchInfoPrinter);
        p.add(fibs.fibsCommandPrinter);
        p.add(fibs.boardPrinter);
        p.add(fibs.linePrinter);

        return p;
    }

    protected void suspendOutput(PrintStream output) {
	    for (BufferedConsolePrinter p : getPrinters()) {
	        try {
	            p.setSuspended(output, true);
            } catch (NullPointerException ignore) {}
        }
    }

    protected void unSuspendOutput(PrintStream output) {
        for (BufferedConsolePrinter p : getPrinters()) {
            try {
                p.setSuspended(output, false);
            } catch (NullPointerException ignore) {}
        }
    }

    protected void printWelcome(PrintStream output) {
        output.print(UnixConsole.LIGHT_WHITE);
        output.println("Entering MatchDog Shell - to select output enter number");
        output.println("1: fibs 2: gnubg 6: gnubg-external | for other commands ");
        output.println("see 'help', to leave the shell press ESC then ENTER");
        output.print(UnixConsole.RESET);
    }

    protected void printDebug(String str) {
		printer.printLine(str);
	}
	
	public PlayerStats openPlayerStats(String path) {
	    try {
			FileInputStream fin = new FileInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(fin);
			PlayerStats ps = (PlayerStats) ois.readObject();
			ois.close();
            return ps;
		} catch (IOException e) {
			printer.printLine(e.toString());
			return null;
		} catch (ClassNotFoundException e) {
			printer.printLine(e.toString());
			return null;
		} catch (Exception e) {
			printer.printLine(e.toString());
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

	public int getFibsMode() {
		return fibsMode;
	}

	public void setFibsMode(int fibsMode) {
		this.fibsMode = fibsMode;
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
		return connectionListeners.values();
	}

    public String getConfigDir() {
        return configDir;
    }

    public String getDataDir() {
        return dataDir;
    }
}

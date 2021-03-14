package matchdog;

import etc.TorExitNodeChecker;
import jcons.src.com.meyling.console.UnixConsole;
import matchdog.console.printer.FibsPrinter;
import matchdog.console.printer.FibsCommandPrinter;
import matchdog.console.printer.MatchInfoPrinter;
import matchdog.fibsboard.FibsBoard;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Date;

public class FibsRunner extends Thread {

	static final int FIBS_MODE_LOGGED_IN = 0;
	static final int FIBS_MODE_WATCH = 1;
	static final int FIBS_MODE_GAME_PLAY = 2;
	static final int FIBS_MODE_TOR_MONIOTOR = 3;

	static final int timoutSeconds = 90;
	static final String FIBSMSG_WAVES_GOODBYE = "waves goodbye";
    static final Object lock = new Object();
	
	int id;

	Socket s;
	InputStream sin;
	BufferedReader input;
	boolean init, run, loggedIn, dead, outOK, resendlastboard;

	// used in processGamePlay
	boolean wOppMoveBoard, wOppDoubleBoard, wRollBoard, wMoveBoard, wResumeBoard;
	boolean ownDoubleInProgress, wScoreBoard, resume, wasResumed;


	// Fibs message types from 1 to 20 (0 doesn't count) to process 
	// (and print to console) during game play. 
	// Default is 7, 12, 15, 16, 19.
	int[] showMsgType = { 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0,
							 0, 1, 0, 0, 1, 1, 0, 0, 1, 0 };

	int doubledInRound;

	// used when setting resume parameters
	boolean[] resumeBits = { false, false, false, false };

	int inithelper = 0;
	String host;
	int port;
	MatchDog server;
	FibsPrinter linePrinter;
	MatchInfoPrinter matchInfoPrinter;
	FibsCommandPrinter fibsCommandPrinter;
	Match match, lastmatch;
	String lastboard, filteredInput;
	boolean processNextLine, terminating;
	boolean getSavedMatches, gettingSaved, getOwnRating,
	getOppRating, invitationInProgress;
	Date terminatedAt;

	FibsBoard lastBoard;
	
	// Normally 'login()' will invoke 'getSavedMatches()' too but
	// if that is not getting through to fibs the 'savedMatchs'
	// map remains empty that would allow for an inviter to
	// overwrite a saved match. In case of invitation before
	// 'savedMatches' is populated, that invitation will be
	// ignored and 'getSavedMatches()' is invoked again.
	boolean gotSavedMatches;
	
	// TODO Is this really needed now?? 
	// FIBS sometimes sends double accept notification 
	// more than once. These are used to ensure the cube
	// is changed only once.
	boolean waitLastBoard, lastBoardSet;
	
	//double oppRatingtmp;
	
	int procGPcounter;
	
	boolean torMonitorMode;

	// These are used in 'processInput()'
	// to 
	String opp = "", resumeopp = "";
	int ml = 0, oppexp = 0, opprep = 0;
	boolean canStart = false;

	boolean getExp = false;
	boolean getRep = false;
	
	// For tor login filtering
	String user;
	//TorExitNodeChecker torcheck = new TorExitNodeChecker();
	
	long lastLine;
	
	FibsRunner(MatchDog server, String host, int port, int id) {
		super("FibsThred");
		s = null;
		this.id = id;
		init = false;
        loggedIn = false;
		terminating = false;
		terminatedAt = null;
		wOppMoveBoard = false;
		wOppDoubleBoard = false;
		wRollBoard = false;
		wResumeBoard = false;
		wMoveBoard = false;
		ownDoubleInProgress = false;
		resume = false;
		wasResumed = false;
		wScoreBoard = false; // ? not in use
		this.host = host;
		this.port = port;
		this.server = server;
		match = null;
        user = null;
		lastmatch = null;
		lastboard = "";
		filteredInput = null;
		run = true;
		dead = false;
		outOK = false;
		processNextLine = false;
		getSavedMatches = false;
		gettingSaved = false;
		getOwnRating = false;
		getOppRating = false;

		lastBoard = null;
		
		resendlastboard = false;
		invitationInProgress = false;

		waitLastBoard = false;
		
		procGPcounter = 0;
		
		doubledInRound = 0;
        torMonitorMode = server.prefs.getGnuBgPort() == 0;
		
		//final String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		setName("FibsRunner-" + this.id);

        linePrinter = new FibsPrinter(
            server, "fibs:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_GREEN
        );

        matchInfoPrinter = new MatchInfoPrinter(
			server, "MatchInfo:", UnixConsole.BLACK, UnixConsole.BACKGROUND_YELLOW
		);

		fibsCommandPrinter = new FibsCommandPrinter(
			server, "", UnixConsole.BLACK, UnixConsole.BACKGROUND_YELLOW
		);
	}

	@Override
	public void run() {
		String inputLine;
		boolean connected = false;
		s = new Socket();
		
		while(!connected) {

			try {	
				InetAddress fa = InetAddress.getByName("fibs.com");
				s = new Socket(fa, server.programPrefs.getFibsport());
			} 
			catch(Exception e) {
                // retry
            }
			
			if(s.isConnected()) {
				connected = true;
				continue;
			}
			
			try {
				FibsRunner.sleep(100);
			} catch(InterruptedException e) {
				return;
			}
		}
		
		try {
			
			sin = s.getInputStream();
			input = new BufferedReader(new InputStreamReader(sin));

			while ((inputLine = input.readLine()) != null) {
				
				if(!run || s.isInputShutdown() || s.isOutputShutdown()) {
					server.systemPrinter.printLine("Stopping fibs: " + s.isInputShutdown() + " - " + s.isOutputShutdown());
					break;
				}
				
				lastLine = System.nanoTime();

				switch (server.getFibsMode()) {

					case FIBS_MODE_LOGGED_IN: // logged in mode
					case FIBS_MODE_WATCH: // watch mode
					case FIBS_MODE_TOR_MONIOTOR: // tor monitor mode
						processInput(inputLine);
						break;

					case FIBS_MODE_GAME_PLAY: // play mode

						processNextLine = true;

						processInput(inputLine);

						if (filteredInput != null && processNextLine) {
							processGamePlay(filteredInput);
							filteredInput = null;
						}
						break;
				}
			}

            server.systemPrinter.printLine("Exiting FibsRunner thread - " + getName());
			dead = true;
			terminate();
		}
		catch (SocketException e) {
			server.systemPrinter.printLine("Fibs connection closed - " + getName());
		}
		catch (Exception err) {
			server.systemPrinter.printLine("FibsRunner(run): " + err +  " - " + getName());
            for(PrintStream os : server.getPrintStreams()) {
                err.printStackTrace(os);
            }
		}
        if(!terminating) {
            server.systemPrinter.printLine("FibsRunner(run): *** RESTARING FIBS ***" + getName());
            sleepFibs(1000);
            restart();
        }
	}

	private synchronized void processInput(String in) {
	
		if(in.equals("6")) {
			in = "6 ";
		}
		
		if (in.length() < 2)
			return;

		boolean inputHasBoard = inputHasBoard(in);
	
		if (server.inviter != null) {
	
			if (in.trim().toLowerCase().startsWith(
					"5 " + server.inviter.name.toLowerCase())) {
				server.inviter.setStateStr(in);
				server.inviter.setWait(false);
	
				server.printDebug("fibs: sending stateline to inviter");
			}
		}
	
		//// TOR FILTER
		if(torMonitorMode) {

			if(in.startsWith("7 ")) {
				user = in.split(" ")[1];				
			}
			if(user != null && in.startsWith("5 " + user)) {
				String [] split = in.split(" ");
				String host = split[10];
				String answer, star;
				server.printDebug("User " + user + " logged in from : "
						+ host + ".");
				
				boolean isTor;
				try {
					isTor = TorExitNodeChecker.isTorExitNode(host);
					star = "";
				} catch(Exception e) {
					isTor = false;
					star = "*";
				}
				if(isTor) {
					answer = " > IS A TOR ADDRESS !!";
					sleepFibs(300);
					server.fibsout.println("message inim " + host + answer
							+ "(fibs user: " + user + " time: "
							+ Calendar.getInstance().getTime() + ")");
				} else {
					answer = " > Not a tor address. ";
				}
				server.printDebug( answer + star );
				server.printDebug("");
				user = null;			
			}
			if(init) {
				return;
			}
		}
		
		// filter fibs output line for
		if (	torMonitorMode
			|| in.contains(FibsRunner.FIBSMSG_WAVES_GOODBYE)
			|| (in.contains("point match")
				&& (   in.contains("wins a") 
					|| in.contains("start a") 
					|| in.contains("are resuming")))) {
			filteredInput = null;
			processNextLine = false;
		} else {
			filteredInput = in;
		}
		
		// filter messages from fibs
		String s = in.substring(0, 2);
		int messagetype;
        try {
			messagetype = Integer.parseInt(s.trim());
	
			if (messagetype > -1) {
	
				processNextLine = false;
	
				if (showMsgType[messagetype] == 0
                    && !((messagetype == 5) && getExp)
                    && !(match != null && match.isWaitRateCalc())
                    && !(match != null && messagetype == 5 && getOwnRating && in.contains(server.prefs.getUsername()))
                    && !(match != null && messagetype == 5 && getOppRating && in.contains(match.getPlayer1()))) {
					
					filteredInput = null;
					return;
				}
			}
	
		} catch (Exception e) {
            // do nothing
		}
	
		// print fibs output
		// currently gameplay and tormonitor modes are filtered
		if (server.getFibsMode() > FIBS_MODE_WATCH) {
			if (filteredInput != null) {
				linePrinter.printLine(filteredInput);
			}
		} else {
			linePrinter.printLine(in);
		}
	
		//// LOGIN
		//("FibsRunner.processInput> >>> inithelper: " + inithelper + " init: " + init);
		if (inithelper > 0 && !init) {
	
			init = true;
			synchronized (MatchDog.lock) {
				MatchDog.lock.notify();
			}
	
			if (server.prefs.isAutologin()) {
				synchronized (lock) {
					try {
						while(!outOK) {
							lock.wait();
						}
					} catch(InterruptedException e) {
						return;
					}
				}
				
				login();
				sleepFibs(400);
				getSavedMatches();
			}
		}
		//// END:LOGIN

        if(!loggedIn && in.startsWith("1 " + server.prefs.getUsername())) {
            loggedIn = true;
        }
		
		// DROPPED CONNECTION
		if(in.startsWith("Connection timed out")) {
			sleepFibs(200);
			run = false;
			server.restartFibs();
		}
		
		// OWN Rating
		if (in.startsWith("5 " + server.prefs.getUsername()) && getOwnRating) {
			getOwnRating = false;
			String [] split0 = in.split(" ");
			server.setOwnRating(Double.parseDouble(split0[6]));
			sleepFibs(200);
			if(match != null) {
				match.setOwnRating(server.getOwnRating());
			
				server.printDebug("OWN RATING set to: " + match.getOwnRating());
			}
			//?//server.savedMatches.clear();
		} else if(in.startsWith("5 ") && getOwnRating) {
			server.printDebug("who " + server.prefs.getUsername());
			server.fibsout.println("who " + server.prefs.getUsername());
		}
	
		// OPP Rating
		if (match != null && in.startsWith("5 " + match.getPlayer1()) && getOppRating) {
			getOppRating = false;
			String [] split0 = in.split(" ");
			
			
			if(match != null) {
				match.setOppRating(Double.parseDouble(split0[6]));
			
				server.printDebug("OPP RATING set to: " + match.getOppRating());
			}
			//?//server.savedMatches.clear();
		} else if(match != null && in.startsWith("5 ") && getOppRating) {
			server.printDebug("who " + match.getPlayer1());
			server.fibsout.println("who " + match.getPlayer1());
		}		
		
		//// GET SAVED MATCHES
		// if 'show saved' was issued store saved matches
		if (in.startsWith("  opponent") && getSavedMatches) {
			gettingSaved = true;
            server.savedMatches.clear();
			server.printDebug("start getting saved matches");
		}
		else if (in.contains("no saved games.")) {
			gettingSaved = false;
            synchronized (MatchDog.lock) {
                MatchDog.lock.notify();
            }
			gotSavedMatches = true;
			getSavedMatches = false;
		}
		if (gettingSaved
				&& (in.startsWith("  ") || in.startsWith(" *") || in
						.startsWith("**"))) {
			if (!in.startsWith("  opponent")) {
				String[] arr1 = in.substring(2).split(" ");
				String opp = arr1[0].trim();
				server.savedMatches.put(server.savedMatches.size(), opp);
			}
		} else if (gettingSaved) {
			gettingSaved = false;
            synchronized (MatchDog.lock) {
                MatchDog.lock.notify();
            }
			server.printDebug(server.savedMatches.size() + " saved match(es)");
			gotSavedMatches = true;
		}
		//// END: GET SAVED MATCHES
	
		// save last board state
		if (inputHasBoard) {
			lastboard = in;
			lastBoard = new FibsBoard(in);
			if(waitLastBoard) {
				waitLastBoard = false;
				lastBoardSet = true;
			}
			if(resendlastboard) {
				resendlastboard = false;
				server.bgRunner.execBoard(in);
			}
		}
	
		
		// AUTOINVITE PREFERRED PLAYERS (from PlayerPrefs)
		if (in.contains("wins a") || in.contains("has left the game")
				|| (in.startsWith("7 ") && in.contains("logs in"))) {
			if (server.getFibsMode() < FIBS_MODE_GAME_PLAY && server.prefs.isAutoinvite()) {
	
				for (String opp : server.prefs.getPreferredOpps().keySet()) {
	
					if (in.toLowerCase().contains(" " + opp.toLowerCase() + " ")) {
						server.startInviter(opp, server.prefs
								.getPreferredOpps().get(opp));
						break;
					}
				}
			}
		}
		// AUTOINVITE FOR SAVED MATCHES
		if (in.contains("wins a") || in.contains("has left the game")
				|| ((in.startsWith("7 ") && in.contains("logs in")))) {
	
			for (String opp : server.getSavedMatches().values()) {
	
				if (!opp.equals("")
						&& in.toLowerCase().contains(
								" " + opp.toLowerCase() + " ")) {
					if(lastmatch != null && server.getFibsMode() == FIBS_MODE_GAME_PLAY && lastmatch.getWaitfor().equals(opp)) {
						server.printDebug("WAITFOR OPP LOGGED IN, inviting");
						sleepFibs(2000);
						server.fibsout.println("invite " + opp);
					} else	if (server.getFibsMode() < FIBS_MODE_GAME_PLAY
							&& server.prefs.isAutoinvitesaved()) {
						server.fibsout.println("tell " + opp + " Hi " + opp
								+ "! Let's play our saved match.");
						server.printDebug("Inviting for SAVED MATCH (" + opp
								+ ")");
						server.startInviter(opp, 0);
					} else if (server.getFibsMode() == FIBS_MODE_GAME_PLAY) {
						server.fibsout.println("tell " + opp + " Hi " + opp
								+ "! There is still a saved match to finish.");
					}
	
					break;
				}
			}
		}
	
		// JOIN AN INVITATION
		if (in.contains("wants to play a") && in.startsWith("board:")) {
			// fibs bug?
		}
		if (in.contains("wants to play a") && !in.contains("unlimited match") 
				&& !in.contains("saved match") && !isInvitationInProgress()
				&& !gettingSaved // <-- Also an opportunity for someonea
								 // to overwrite a saved match.
								 // Should saved matches be serialized?
				// When another msg is appended to a board:... line and
				// contains 'point match' could cause exception.
				// Don't allow.
				&& !in.startsWith("board:")
				&& server.getFibsMode() < FIBS_MODE_GAME_PLAY) {
			
			// If 'show saved' didn't get through to fibs after login
			// or wasn't issued after manual login the 'savedMatches'
			// map is empty after login which could allow someone
			// to overwrite a saved match. Don't allow.
			if(!gotSavedMatches) {
				getSavedMatches();
				return;
			}
			setInvitationInProgress(true);
			String[] split0 = in.split(" wants to play a");
			String[] split1 = split0[1].split(" point match");
			opp = split0[0];
			server.fibsout.println("whois " + opp);
	
			// check lastopp timeout
			long tmpl;
			if(server.getLastopp() != null) {
				tmpl = server.prefs.getReplayTimeout() * 1000
						- (Calendar.getInstance().getTimeInMillis() 
						- server.getLastfinish().getTime());
				if(tmpl <= 0) {
					server.setLastfinish(null);
					server.setLastopp(null);
				}
			}
			
			// waitfor
			if(lastmatch != null ) {
				server.printDebug("NOT ACCEPTING invtiation: " + opp);
				setInvitationInProgress(false);
				long tmpw = 45000 - (Calendar.getInstance().getTimeInMillis() 
										- lastmatch.getDroppedat().getTime());
				server.printDebug("(waiting for: " + lastmatch.getWaitfor() 
									+ ", " + tmpw / 1000 + " seconds left)");
				
				if(lastmatch.getWaitfor().equals(opp)) {
					
					sleepFibs(350);			
					server.fibsout.println("invite " + opp );
				} else {
	
					tmpw = 45000 - (Calendar.getInstance().getTimeInMillis() 
												- lastmatch.getDroppedat().getTime());sleepFibs(300);
					server.fibsout.println("tell " + opp + " I'm waiting for someone to resume a dropped match.");
					sleepFibs(500);			
					server.fibsout.println("tell " + opp + " Try again in " + tmpw / 1000 + " seconds.");
				}
			// lastopp
			} else if(opp.equals(server.getLastopp())) {
				setInvitationInProgress(false);
				tmpl = server.prefs.getReplayTimeout() * 1000
								- (Calendar.getInstance().getTimeInMillis() 
								- server.getLastfinish().getTime());
				
				server.printDebug("OPP: " + opp + " wants to replay, time left: " + tmpl / 1000);
				sleepFibs(100);
				server.fibsout.println("tell " + opp + " I'd love to but other fibsters may " 
										+ "also want to enjoy playing with me. Try again after " 
										+ tmpl / 1000 + " seconds or see you later...");
			
			} else if(server.isBlacklisted(opp)) {
				setInvitationInProgress(false);
				server.printDebug("Not joining invitation - blackslited opp");
			// OK
			} else {
				// If execution gets here, we can start gathering information
				// about our opponent candidate.
				// FIXME
				// The below is somewhat fixed with the use of the
				// 'ownInvitationInProgress' boolean. A competition
				// situation would be the best where more inviters are allowed.
				
				// Concurrent invitaions aren't handled correctly.
				// There's only one placeholder (with some associated
				// variables) for an inviter. If another invitation is 
				// arriving before a pending one is accepted or ignored,
				// the latter one will overwrite the earlier and may
				// result in incorrect handling (not all associated variables 
				// change accordingly)
				sleepFibs(200);
				getExp = true;
				server.fibsout.println("who " + opp);
	
				sleepFibs(150);
				ml = Integer.parseInt(split1[0].trim());
	
				server.printDebug("opp: " + opp + " ml: " + ml);
			}
		}
		else if(isInvitationInProgress()) {
			if(in.startsWith("** " + opp + " didn't invite you")) {
				// cancel an invitation that expires, i.e. after a 'want's to play a' from an 'opp'
				// a sent 'invite opp' results in this (e.g. after a long net lag)
				setInvitationInProgress(false);
				server.printDebug("opp: " + opp + " invited but now FIBS thinks not");
			}
			else if(in.startsWith("** Error: " + opp + " is already playing")) {
				setInvitationInProgress(false);
				server.printDebug("opp: " + opp + " invited but playing with someone else now");
			}
			else if(in.startsWith("** There is no one called " + opp )) {
				setInvitationInProgress(false);
				server.printDebug("opp: " + opp + " invited but disappeared");
			}
		}
		
		if (in.startsWith("5 " + opp) && getExp) {
			getExp = false;
			getRep = true;
			
			String[] split0 = in.split(" ");
			oppexp = Integer.parseInt(split0[7]);
			////oppRatingtmp = Double.parseDouble(split0[6]);
			server.printDebug(opp + " experience: " + oppexp);
			sleepFibs(200);
			server.fibsout.println("tell RepBotNG ask " + opp);
			return;
			//match.setOppRating();
		
		} else if(in.startsWith("5 ") && getExp) {
			sleepFibs(200);
			server.fibsout.println("who " + opp);
			return;
		}
		if (in.startsWith("12 RepBotNG " + opp) && getRep) {
			getRep = false;
			String[] split0 = in.split(" reputation is ");
			String[] split1 = {};
			if (split0[1].contains("GOOD")) {
				split1 = split0[1].split("GOOD");
			} else if (split0[1].contains("BAD")) {
				split1 = split0[1].split("BAD");
			}
			opprep = Integer.parseInt(split1[0].substring(0,
					split1[0].length() - 1).trim());
			server.printDebug(opp + " reputation: " + opprep);
			canStart = true;
		}
		else if(in.startsWith("** There is no one called RepBotNG") && getRep) {
			getRep = false;
			canStart = true;
		}
		else if (in.startsWith("12 RepBotNG User " + opp + " has no") && getRep) {
			opprep = 0;
			getRep = false;
			canStart = true;
		}  
		/*else if(getRep) {
			server.fibsout.println("tell RepBotNG ask " + opp);
			return;
		}*/
		if (canStart) {
			canStart = false;
			// if user has saved match don't join
			if (server.getSavedMatches().containsValue(opp)) {
				setInvitationInProgress(false);
				sleepFibs(150);
				server.fibsout.println("invite " + opp);
				sleepFibs(330);
				server.fibsout.println("tell " + opp + " Hi " + opp
						+ "! Let's play our saved match first.");
				server.printDebug("Inviting for SAVED MATCH (" + opp + ")");
	
			} else if (server.prefs.isAutojoin()) {
				if (ml > server.prefs.getMaxml()) {
					setInvitationInProgress(false);
					server.printDebug("Not joining invitation - maxml: " + ml + " > " + server.prefs.getMaxml()
							+ " invited for:" + ml);
					server.fibsout.println("tell " + opp
							+ " Hi " + opp + "! I am a bot.");
	
					if (server.prefs.getMaxml() >= 9) {
						sleepFibs(500);
						server.fibsout.println("tell " + opp
								+ " How about a 5-7-9 pointer?");
					}
					
					if(server.prefs.getMaxml() == 1) {
						sleepFibs(750);
						server.fibsout.println("tell " + opp
								+ " I play only 1-pointers.");
					} else {
						server.fibsout.println("tell " + opp
							+ " I play matches up to "
							+ server.prefs.getMaxml() + " points.");
					}
				} else if (oppexp / server.prefs.getExpdivider() < ml) {
					setInvitationInProgress(false);
					server.printDebug("Not joining invitation - not enough exp: " + (oppexp / server.prefs.getExpdivider()) + " < " + ml);
					server.fibsout.println("tell " + opp
                            + " You are not experienced enough for that long match, sorry.");
					sleepFibs(200);
					server.fibsout.println("tell " + opp + " (Let match length be less than your experience / "
                            + server.prefs.getExpdivider() + ")");
				} else if (opprep < server.prefs.getReplimit()) {
					setInvitationInProgress(false);
					server.printDebug("Not joining invitation - bad rep: " + opprep + " < " + server.prefs.getReplimit());
					server.fibsout.println("tell " + opp
							+ " Sorry not now. You need to improve you reputation.");
	
				} else {
					// set it to false in startMatch()
					//setInvitationInProgress(false);
					sleepFibs(200);
					server.fibsout.println("join " + opp);
					server.printDebug("joining invitation - NEW match (oppname: " + opp + ")");
					server.printDebug("2nd 'join' ..");
					sleepFibs(200);
					server.fibsout.println("join " + opp);
					return;
				}
	
			} else {
				setInvitationInProgress(false); 
				sleepFibs(200);
				server.fibsout.println("tell " + opp + " Sorry, I'm only accepting invitations for saved matches now. Try again later.");
			}
		}
	
		// JOIN AN INVTITATION TO RESUME A MATCH
		if (in.contains("wants to resume a saved match with you")) {

            if (!server.prefs.isAutojoinsaved()) {
                return;
            }

			setInvitationInProgress(true);
			String[] arr0 = in.split(" wants to resume a saved match with you");
			resumeopp = arr0[0];
			if(lastmatch != null) {
				if(lastmatch.getWaitfor().equals(resumeopp)) {
					
					server.printDebug("WAITFOR OPP arrived (oppname: " + resumeopp
							+ "), getting rating");
					lastmatch.cancelWaitfor();
					lastmatch = null;
					sleepFibs(500);	
					server.fibsout.println("join " + resumeopp);
					return;
				} else {
					setInvitationInProgress(false);
					sleepFibs(200);
					server.fibsout.println("tell " + resumeopp + " I'm waiting for someone to resume a dropped match.");
					sleepFibs(300);
					long tmp = 30000 - (Calendar.getInstance().getTimeInMillis() 
												- lastmatch.getDroppedat().getTime());
					server.fibsout.println("tell " + resumeopp + " Try again in " + tmp / 1000 + " seconds.");
				}
			} else if (server.prefs.isAutojoinsaved()) {
				server.printDebug("getting RESUME opp rating (oppname: " + resumeopp + ")");
				sleepFibs(500);	
				server.fibsout.println("join " + resumeopp);
                return;
            }
	
		}
/*		if(in.startsWith("5 " + resumeopp) && getResumeRating) {
			getResumeRating = false;
			sleepFibs(300);
			server.fibsout.println("join " + resumeopp);
			resumeopp = "";
			return;
		} else if(in.startsWith("5 ") && getResumeRating) {
			server.fibsout.println("who " + resumeopp);
			return;
		}*/
		
		
		// TELL ME MORE
		if (in.startsWith("15") && in.toLowerCase().contains("tell me more")) {
			server.fibsout
					.println("kibitz I use the evaluation engine of GNU Backgammon (TM)");
			try {
				FibsRunner.sleep(100);
				server.fibsout
						.println("kibitz kibitz 'show settings' if you want to see the gnubg settings I am using");
			} catch (InterruptedException e) {
			}
		}
	
		// SHOW SETTINGS
		if (in.startsWith("15") && in.toLowerCase().contains("show settings")) {
			String [] settings = server.prefs.showPrefs();
			for(String line : settings) {
				if(!line.equals("")) {
					sleepFibs(350);
					server.fibsout.println("k " + line);
				}
			}
	
		}
		
		// If opponent kibitzes 'move' try sending last saved board to gnubg
		if (match != null &&
				(in.startsWith("15 " + opp + " move") 
				|| in.startsWith("15 " + opp + " roll")
				|| in.startsWith("15 " + opp + " play"))) {
			if(match.isOppsTurn()) {
				server.fibsout.println("k Is it not your turn?");
			}
			server.printDebug("RESENDING lastboard to gnubg");
			
			sleepFibs(150);
			server.fibsout.println("b");
			sleepFibs(750);
			resendlastboard = true;
			return;
			
		}
		// CHAT
		if(match != null && match.getRound() == 30 && !match.isDropped()
				&& match.getGameno() == 2 && in.startsWith("You roll" )) {
			sleepFibs(250);
			server.fibsout.println("k " + match.generalChat() );
			sleepFibs(200);
		}
	
		// SHOW STAT
		if((in.startsWith("12 ") || in.startsWith("15 "))
						&& in.toLowerCase().contains("show stat")) {
			String [] split0 = in.split(" ");
			String statfor = split0[1];
			if(!server.statview.dumpPlayerStat(statfor, 2)) {
				server.fibsout.println("tell " + statfor 
						+ " No statistics for you (since "
						+ server.playerstats.getSince(1) + ").");
			}
		}
		
		// SHOW LOG
		if((in.startsWith("12 ") || in.startsWith("15 ")) 
						&& in.toLowerCase().contains("show log")) {
			String [] split0 = in.split(" ");
			String statfor = split0[1];
			
			if(server.statview.dumpPlayerStat(statfor, 3)) {
				while(!server.statrequests.containsKey(statfor)) {}
				String fn = server.statrequests.get(statfor);
				Uploader ul = new Uploader(server, 0, fn);
				ul.start();
			} else {
				server.fibsout.println("tell " + statfor 
						+ " No log for you since " 
						+ server.playerstats.getSince(1));
			}
		}			
		
		// GREET WATCHER
		if(in.contains(" is watching you")) {
			String [] split0 = in.split(" is watching you");
			String watcher = split0[0];
			if(watcher.equals("marekful")) {
				server.fibsout.println("k Hey marekful!");
			} else {
				server.fibsout.println("k Hi " + watcher + "!");
			}
		}
		
		if (in.contains("One account per person only!"))
			inithelper++;
		if (in.startsWith("You're now watching"))
			server.setFibsMode(1);
	
		// START A MATCH
        if (   in.contains(" has joined you")
            || in.contains("You are now playing")) {
			
			startMatch(in);
		}
	}

	private boolean inputHasBoard(String input) {
		return input.startsWith("board:");
	}

	private FibsBoard inputGetBoard(String input) {
		return new FibsBoard(input);
	}

	private synchronized void processGamePlay(String in) {

		if (in == null)
			return;

		boolean inputHasBoard = inputHasBoard(in);
		FibsBoard inputBoard = null;

		if (inputHasBoard) {
			inputBoard = new FibsBoard(in);
		}

		///// PROCESS RESUME PARAMS ////
		// If match was started with resume=true this section is processed
		// until resume is set to false when all resume parameters are set.
		if (resume) {

			match.setGameno(1);
			match.setRound(1);

			String parseStr;
			int score;
			int ml;

			if (in.toLowerCase().startsWith("match length: "))
            {
				server.printDebug("setting resumeparams: " + in);
				try {
					parseStr = in.split("match length: ")[1];
					ml = Integer.parseInt(parseStr.trim());
					match.setMl(ml);
					resumeBits[1] = true;

					server.printDebug("ml set to: " + ml);
				} catch (NumberFormatException e) {

				}
			}

			if (in.toLowerCase().startsWith("points for "
                    + server.prefs.getUsername().toLowerCase() + ": "))
            {
				server.printDebug("setting resumeparams: " + in);
				try {
					parseStr = in.split("points for " + server.prefs.getUsername()
							+ ": ")[1];
					score = Integer.parseInt(parseStr.trim());
					match.score[0] = score;
					resumeBits[2] = true;
				} catch (NumberFormatException e) {

				}
			}

			if (in.toLowerCase().startsWith(
					"points for " + match.getPlayer1().toLowerCase() + ": "))
            {
				server.printDebug("setting resumeparams: " + in);
				try {
					parseStr = in.toLowerCase().split(
							"points for " + match.getPlayer1().toLowerCase()
									+ ": ")[1];
					score = Integer.parseInt(parseStr.trim());
					match.score[1] = score;
					resumeBits[3] = true;
				} catch (NumberFormatException e) {

				}
			}

			if (in.toLowerCase().startsWith(
					"turn: " + server.prefs.getUsername().toLowerCase()))
            {
				server.printDebug("setting resumeparams: " + in);
				match.setTurn(new int[] { 1, 0 });
				resumeBits[0] = true;

				server.printDebug("turn set to: " + match.getTurn()[0] + " "
						+ match.getTurn()[1]);
			}

			if (in.toLowerCase().startsWith(
					"turn: " + match.getPlayer1().toLowerCase()))
            {
				server.printDebug("setting resumeparams: " + in);
				match.setTurn(new int[] { 0, 1 });
				resumeBits[0] = true;
				server.printDebug("turn set to: " + match.getTurn()[0] + " "
						+ match.getTurn()[1]);
			}
		}

		
		if (resume && resumeBits[0] && resumeBits[1]
                   && resumeBits[2] && resumeBits[3])
        {
            server.printDebug("finished setting resumeparams, requesting board from fibs");
            resume = false;
            wasResumed = true;
            match.setWasResumed(true);
            resetWaitFlags();
            wResumeBoard = true;
            sleepFibs(300);
            server.fibsout.println("board");
            sleepFibs(100);

			return;
		}
		///// END: PROCESS RESUME PARAMS ////
		procGPcounter++;

		/// START GAME ///
		if (match != null && in.equals("Starting a new game with "
                + match.getPlayer1() + "."))
        {
			resetWaitFlags();
			match.setIngame(true);
			match.setCube(1);
		}// / END: START GAME ///

		// / PROCESS INITIAL ROLL ///
		if (in.startsWith("You rolled ") && match.getRound() == 0) {
			//server.printDebug("in: " + in);
			server.printDebug("initial roll");
			String[] arr0 = in.split("You rolled");
			//server.printDebug("dbg: arr0[0]" + arr0[0]);
			//server.printDebug("dbg: arr0[1]" + arr0[1]);
			String[] arr1 = arr0[1].toLowerCase().split(
					", " + match.getPlayer1().toLowerCase() + " rolled ");
			//server.printDebug("dbg: arr1[0]" + arr1[0]);
			//server.printDebug("dbg: arr1[1]" + arr1[1]);

			int d1 = Integer.parseInt(arr1[0].trim());
			int d2 = Integer.parseInt(arr1[1].trim());
			try {

				if (d1 == d2)
					return;

				match.setRound(1);
				if (d1 > d2) {
					match.setTurn(new int[] { 1, 0 });
					wRollBoard = true;
				}
				if (d2 > d1) {
					match.setTurn(new int[] { 0, 1 });
				}

				match.setDice(new int[] { d1, d2 });

				if (match.getGameno() < 1) {
					match.setGameno(1);
				}

				wScoreBoard = true;
				// server.gnubgout.println("set dice " + d1 + " " + d2);

			} catch (NumberFormatException e) {
				server.systemPrinter.printLine("FibsRunner(processGamePlay): " + e);
			}
			//match.stat.newGame(d1, d2);

		} // / END: PROCESS INITIAL ROLL ///

		// / DROPPED MATCH ///
		if (match != null && in.startsWith( "** Player "
                + match.getPlayer1() + " has left the game."))
        {
			//server.printDebug("in: " + in);
			server.printDebug("END MATCH - opponent left");

			match.setDropped(true);
			stopMatch();
			return;
		}
        else if (match != null && in.startsWith(
				match.getPlayer1() + " drops connection."))
        {
			//server.printDebug("in: " + in);
			server.printDebug("END MATCH - OPP drops connection");
			
			match.setDropped(true);
			stopMatch();
			return;
		}
        else if (match != null && in.toLowerCase().startsWith(
				match.getPlayer1().toLowerCase() + " logs out"))
        {
			//server.printDebug("in: " + in);
			server.printDebug("END MATCH - OPP logged out");

			match.setDropped(true);
			stopMatch();
			return;
		}
        else if (match != null && in.startsWith("Network error with")
				&& in.contains(match.getPlayer1()))
        {
			//server.printDebug("in: " + in);
			server.printDebug("END MATCH - network error with OPP");

			match.setDropped(true);
			stopMatch();
			return;
		}
        else if (match != null && in.startsWith("Closed old connection with user " + match.getPlayer1())
				&& in.toLowerCase().contains(match.getPlayer1().toLowerCase()))
        {

			server.printDebug("END MATCH - closed old. conn.with OPP");

			match.setDropped(true);
			stopMatch();
			return;
		}
        else if (match != null && in.startsWith("** You terminated the game"))
        {
			//server.printDebug("in: " + in);
			server.printDebug("END MATCH - I left the game");

			match.setDropped(true);
			stopMatch();
			return;
		}
        else if (match != null && in.startsWith("** Error: No one to leave."))
        {
			server.printDebug("in: " + in);
			server.printDebug("END MATCH - ? (I left the game earlier but got no response?)");

			match.setDropped(true);
			stopMatch();
		}
        else if (match != null && in.startsWith("** You're not playing."))
        {
			//server.printDebug("in: " + in);
			server.printDebug("END MATCH - ? (mathc timed out during a long network lag?)");

			match.setDropped(true);
			stopMatch();
		}
		/// END: DROPPED MATCH

		/// END MATCH
		if (in.startsWith("You win ") && in.contains("point match")) {
			//server.printDebug("in: " + in);
			server.printDebug("END MATCH");

			String [] split0 = in.replace(".", "").split("point match ");
			match.setFinalscore(split0[1].trim());
			
			
			stopMatch();

			return;
		} else if (in.toLowerCase().startsWith(
				match.getPlayer1().toLowerCase() + " wins")
				&& in.contains("point match")) {
			//server.printDebug("in: " + in);
			server.printDebug("END MATCH");
			
			String [] split0 = in.replace(".", "").split(" point match ");
			String [] split1 = split0[1].trim().split("-");
			match.setFinalscore(split1[1] + "-" + split1[0]);
			
			
			stopMatch();

			return;
		}
		if(in.startsWith("rating calculation:")) {
			match.setWaitRateCalc(true);
		}
		if(match.isWaitRateCalc() && in.startsWith("change for " + server.prefs.getUsername())) {
			String [] split0 = in.split("=");
			match.setOwnRatingChange(Double.parseDouble(split0[1]));
			server.printDebug("OWN RATING CHANGE set to: " + match.getOwnRatingChange());
		
		} else if(match.isWaitRateCalc() && in.startsWith("change for " + match.getPlayer1())) {
			String [] split0 = in.split("=");
			match.setOppRatingChange(Double.parseDouble(split0[1]));
			server.printDebug("OPP RATING CHANGE set to: " + match.getOppRatingChange());
		}
		//// END: END MATCH

		//// END GAME ///
		if (in.startsWith("You win the game")) {
			//server.printDebug("in: " + in);
			server.printDebug("END GAME - MatchDog won");

			match.setRound(0);
			match.setTurn(new int[] { 0, 0 });
			return;
		} else if (in.toLowerCase().startsWith(
				match.getPlayer1().toLowerCase() + " wins the game and gets")) {
			server.printDebug("in: " + in);
			server.printDebug("END GAME - OPP won");

			match.setRound(0);
			match.setTurn(new int[] { 0, 0 });
			return;
		} else if (in.startsWith("You give up")
				&& in.contains(
						match.getPlayer1() + " wins the game")) {
			//server.printDebug("in: " + in);
			server.printDebug("END GAME - MatchDog gave up double");

			match.setRound(0);
			match.setTurn(new int[] { 0, 0 });
			return;
		} else if (in.startsWith("You accept and win ")) {
			//server.printDebug("in: " + in);
			server.printDebug("END GAME - OPP resign accepted");

			match.setRound(0);
			match.setTurn(new int[] { 0, 0 });
			return;
		} else if (in.toLowerCase().contains(
				match.getPlayer1().toLowerCase() + " wins")
				&& in.contains(" point")) {
			//server.printDebug("in: " + in);
			server.printDebug("END GAME - MatchDog ??gave up double");

			match.setRound(0);
			match.setTurn(new int[] { 0, 0 });
			return;
		} else if(in.startsWith(match.getPlayer1() + " accepts") 
									&& match.isOwnResignInProgress()) {
			match.setOwnResignInProgress(false);
			//server.printDebug("in: " + in);
			server.printDebug("END GAME - MatchDog's resign accepted");

			match.setRound(0);
			match.setTurn(new int[] { 0, 0 });
			return;
		}
		if (in.startsWith(match.getPlayer1() + " gives up.")
				&& ownDoubleInProgress) {
			server.printDebug("END GAME - got GIVE UP response to own double");
			//server.printDebug("got GIVE UP response to own double");
			printMatchInfo();
			ownDoubleInProgress = false;
			match.setIngame(false);
			match.setRound(0);
			match.setTurn(new int[] { 0, 0 });
			return;
		}
		//// END: END GAME ////

		//// JOIN NEXT GAME ////
		if (in.startsWith("score in " + match.getMl() + " point match:")) {

			if (match.getMl() > 1) {

				server.printDebug("UPDATING MATCH LOG info (game#: " + match.getGameno() + ")");
				
				String [] split0 = in.split("point match: ");
				String [] split1 = split0[1].trim().split(" ");
				String [] split2 = split1[0].trim().split("-");
				String [] split3 = split1[1].trim().split("-");
				
				String scorestr = "";
				if(split2[0].equals(server.prefs.getUsername())) {
					scorestr += split2[1] + "-" + split3[1] + " ";
				} else if(split2[0].equals(match.getPlayer1())) {
					scorestr += split3[1] + "-" + split2[1] + " ";
				}
				
				// --------> TODO move this code and instances to a method (x 3 occurences now)
				// update rpg
				match.roundspergames.put(match.getGameno(), match.roundsave);
				match.roundsave = 0;
				
				// update timehistory
				//String tmp = match.getTotalTime() / 1000 / 60 + ":"
				//+ (match.getTotalTime() / 1000 - match.getTotalTime() / 1000 / 60 * 60);
				match.timehistory.put(match.getGameno(), match.getGameTime());
				match.setGameTime(Calendar.getInstance().getTimeInMillis());
				
				// update scorehistory
				match.scorehistory.put(match.getGameno(), scorestr);
				// <-----------
				
				
				match.setGameno(match.getGameno() + 1);
				match.setPostcrawford(false);
				
				server.fibsout.println("join");
				server.printDebug("joining next game");
				server.printDebug("gameno set to: " + match.getGameno());
				server.printer.printLine("\n\n ** ** STARTING GAME " + match.getGameno() + " ** **\n\n", "");

				matchInfoPrinter.printMatchInfo();
			}
			return;
		} //// END: JOIN NEXT GAME

		//// HANDLE OWN RESIGN REJECTED
		if(match.isOwnResignInProgress() && in.startsWith(match.getPlayer1() + " rejects")) {
			match.setOwnResignInProgress(false);
			server.printDebug("Resignation rejected by opp");
			wMoveBoard = true;
			server.bgRunner.execBoard(lastboard);
			return;
		}
		//// END:HANDLE OWN RESIGN REJECTED
		
		//// HANDLE OPP RESIGNATION
		if (in.startsWith(match.getPlayer1() + " wants to resign.")) {
			
			match.setOppResignInProgress(true);
			
			//server.printDebug("in: " + in);
			server.printDebug("OPP wants to resign");

			String[] arr0 = in.split("You will win ");
			String ptstr = arr0[1].substring(0, 2);
			int resignpts = 0;
			try {
				resignpts = Integer.parseInt(ptstr.trim());
			} catch (Exception e) {

			}
			
			
			// OPP RESIGNATION WINS THE MATCH
			if (resignpts >= match.getMl() - match.getScore()[0]) {
				server.printDebug("** RESIGN wins, ACCEPTED [resignpts: " + resignpts + " ml: "
						+ match.getMl() + "  ]");
				server.fibsout.println("accept");

				//stopMatch();
				return;
			}

			//// GET EQUITITES before deciding on opp resign attempt
			server.printDebug("EXTRA get equities -> lastboard: " + lastboard);

			//-//
            /*String[] split = lastboard.split(":");
            split[32] = split[42] == "1" ? "-1" : "1";
            String eqboard = ""; boolean first = true;
            for(String s : split) {
                if(first) first = false;
                else eqboard += ":";
                eqboard += s;
            }*/

            String eqboard = getEquitiesBoard(lastBoard); //-

			server.printDebug("EXTRA get equities ->   eqboard: " + eqboard);
			server.bgRunner.execEval(eqboard);

			server.printDebug("EXTRA get equities finished");
			
			// OPP RESIGN NORMAL
			if (resignpts == lastBoard.getDoublingCube()) {

				// there's chance for win/gammon, so reject
				if(match.equities[1] > 0.01 ) {
					server.printDebug("** RESIGN n REJECTED [resignpts: " + resignpts
							+ " ml: " + match.getMl() + " score: " + match.getScore()[0] 
							+ "-" + match.getScore()[1] + " EQ win/normal: " 
							+ match.equities[0] + " EQ win/gammon: " 
							+ match.equities[1] + "]");
					sleepFibs(200);
					server.fibsout.println("reject");
					return;
				}
				
				else {
					server.printDebug("RESIGN n ACCEPTED [resignpts: " + resignpts
							+ " ml: " + match.getMl() + " score: " + match.getScore()[0] 
  							+ "-" + match.getScore()[1] + " EQ win/normal: " 
  							+ match.equities[0] + " EQwin/ gammon: " 
  							+ match.equities[1] + "]");
					
					sleepFibs(200);
					server.fibsout.println("accept");
					return;
				}					
			} 
			// OPP RESIGN GAMMON
			else if (resignpts == lastBoard.getDoublingCube() * 2) {
				
				// there's chance to win backgammon, so reject
				if(match.equities[2] > 0.0) {
					server.printDebug("** RESIGN g REJECTED [resignpts: " + resignpts
							+ " ml: " + match.getMl() + " score: " + match.getScore()[0] 
  							+ "-" + match.getScore()[1] + " EQ win/normal: " 
  							+ match.equities[0] + " EQ win/backgammon: " 
  							+ match.equities[2] + "]");				
					sleepFibs(250);
					server.fibsout.println("reject");
					return;
				}

				// it's sure we can't win backgammon, so accept
				else if(match.equities[2] == 0.0) {
					server.printDebug("** RESIGN g ACCEPTED [resignpts: " + resignpts
							+ " ml: " + match.getMl() + " score: " + match.getScore()[0] 
  							+ "-" + match.getScore()[1] + " EQ win/normal: " 
  							+ match.equities[0] + " EQ win/gammon: " 
  							+ match.equities[1] + "]");
					sleepFibs(250);
					server.fibsout.println("accept");
					return;
				} 	
			} 
			// OPP RESIGN BACKGAMMON
			else if (resignpts == lastBoard.getDoublingCube() * 3) {

				server.printDebug("** RESIGN bg ACCEPTED [resignpts: " + resignpts
						+ " ml: " + match.getMl() + " score: " + match.getScore()[0] 
						+ "-" + match.getScore()[1] + " EQ win/normal: " 
						+ match.equities[0] + " EQ win/gammon: " 
						+ match.equities[1] + "]");
				sleepFibs(250);
				server.fibsout.println("accept");
				return;
				
			}
			match.setOppResignInProgress(false);
		}
		//// END: HANDLE RESIGNATION ///

		//// PROCESS TURN ////
		if (match.getRound() > 0) {
			
			// GREET OPPONENT 
			if (!match.oppgreeted && inputHasBoard)
				greetOpponent(inputBoard);

			// FIRST ROUND
			if (match.getRound() == 1 && inputHasBoard) {

				server.printDebug("setting score to: " + inputBoard.getMyScore() + " "
						+ inputBoard.getOppScore());

				//-//match.setScore(new int[] { getScore(in)[0], getScore(in)[1] });
				match.setScore(inputBoard.getScore());
				server.printDebug(" [ ml: " + match.getMl() + " opp's score: "
						+ inputBoard.getOppScore() + " crawford score: "
						+ match.getCrawfordscore() + " wasResumed: "
						+ wasResumed + " turn: " + match.getTurn()[0] + " " + match.getTurn()[1] +" ]");
				// CRAWFORD
				if (match.getMl() > 1
						&& (match.getMl() - inputBoard.getOppScore() == 1)
						&& !match.oneToWin()) {

					if (match.getCrawfordscore() == -1
							&& !(wasResumed && inputBoard.didCrawford())) {
						match.setCrawford(true);
						match.setCrawfordscore(match.getScore()[0]);
						server.printDebug("setting CRAWFORD ON");
					} else if (match.getScore()[0] > match.getCrawfordscore()
							&& match.isCrawford()) {
						match.setCrawford(false);
						match.setPostcrawford(true);
						// does this work??
						//server.gnubgout.println("set crawford off");
						//server.printDebug("setting CRAWFORD OFF");
					}
				} else {
					server.printDebug("NOT setting CRAWFORD");
					// post crawford is active for one game after the crawford game only now
					match.setPostcrawford(false);
				}
			} //// END: CRAWFORD ///

			///// MY TURN ////////////////////
			if (match.isMyTurn()) {
	
				if (inputHasBoard && wResumeBoard) {
					server.printDebug("got resume board, sending to bgsocket");
					//-//setResumeCube(in);
					match.setCube(inputBoard.getDoublingCube()); //-
					wResumeBoard = false;
					//-//setDirection(in);
					match.setShiftmove(inputBoard.getDirection() == 1); //-

					if (inputBoard.getMyDice().getDie1() > 0 && inputBoard.getMyDice().getDie2() > 0) {
						match.setTurn(new int[] { 0, 1 });
					}
					match.setRound(match.getRound() + 1);
					server.bgRunner.execBoard(in);
					return;
				}

				if (in.startsWith("You roll ")) {
					server.printDebug("got 'You roll ...', waiting for own roll board");
					//server.printDebug("");
					//printMatchInfo();
					wRollBoard = true;
					match.touchStamps();
					return;
				}
				if (inputHasBoard && wRollBoard) {
					server.printDebug("got roll board" + (inputBoard.getCanMove() > 0 ? ", sending to bgsocket" : ""));
					wRollBoard = false;
					//-//setDirection(in);
					match.setShiftmove(inputBoard.getDirection() == 1); //-

					//// GET EQUITITES
					server.bgRunner.execEval(in);
					
					//// RESIGN ////					
					if(match.getMl() == 1) {
						
						if(match.equities[0] < 0.005) {
							server.printDebug("**** RESIGNING 1-ptr match");
							match.setOwnResignInProgress(true);
							sleepFibs(200);
							server.fibsout.println("resign n");
							return;
						}
						
					} else {
						
						if(match.equities[0] < 0.005 && match.equities[3] < 0.03) {
							if(inputBoard.getIHaveOnBar() == 0) {

								server.printDebug("**** !! RESIGNING NON-1-ptr match -- NORMAL");
								sleepFibs(200);
								match.setOwnResignInProgress(true);
								server.fibsout.println("resign n");
								return;
							}
						}
						
						if(match.equities[0] == 0.0 && match.equities[4] == 1.0) {
							server.printDebug("**** !!! RESIGNING NON-1-ptr match -- BACKGAMMON");
							sleepFibs(200);
							match.setOwnResignInProgress(true);
							server.fibsout.println("resign b");
							return;
							
						} else if(match.equities[0] == 0.0 && match.equities[3] > 0.99) {
							server.printDebug("**** !!! RESIGNING NON-1-ptr match -- GAMMON");
							sleepFibs(200);
							match.setOwnResignInProgress(true);
							server.fibsout.println("resign g");
							return;
						}
					
					} //// END: RESIGN ////
					
					wMoveBoard = true;
                    if(inputBoard.getCanMove() > 0) {
                        server.bgRunner.execBoard(in);
                    }
					return;
				}
				
				if((inputHasBoard || in.startsWith("You can't move") ) && wMoveBoard) {
					wMoveBoard = false;
					match.touchStamps();
					match.setTurn(new int[] { 0, 1 });
					match.setRound(match.getRound() + 1);

                    server.printer.printLine("\n *** OPP's turn ***\n", "");
                    printMatchInfo();
				}


				if (in.startsWith("You double.")) {
					server.printDebug("got 'You double.'");
					server.printDebug("turn: " + match.turn[0] + " " + match.turn[1]);

					ownDoubleInProgress = true;
					match.touchStamps();
					return;
				}

				if (in.startsWith(match.getPlayer1() + " accepts")
						&& ownDoubleInProgress) {
					server.printDebug("last doubled in round: " + doubledInRound);
					if(match.getRound() > doubledInRound) {
						server.printDebug("got ACCEPT response to own double, waiting for roll board");
						//server.printDebug("");
						//printMatchInfo();

						match.setCube(match.getCube() * 2);
						doubledInRound = match.getRound();
					}

				}
			////// <-----

			//////// OPP'S TURN ////////////////////
			} else if (match.isOppsTurn()) {

				if(inputHasBoard && wResumeBoard) {
					//-//setResumeCube(in);
					match.setCube(inputBoard.getDoublingCube());
					wResumeBoard = false;
				}
				
				if (in.toLowerCase().startsWith(
						match.getPlayer1().toLowerCase() + " moves")) {
					server.printDebug("got 'OPP moves...', waiting for OPP move board");


                    wOppMoveBoard = true;
					match.touchStamps();
					return;
				} else if (in.startsWith(match.getPlayer1() + " can't move")) {
					server.printDebug("got 'OPP can't move'");

					match.touchStamps();
					if (match.getMl() > 1 && lastBoard.iMayDouble()
							&& !match.isCrawford() && !match.oneToWin()
							&& !(match.wasResumed() && match.getRound() == 1)) {

						wOppMoveBoard = true;
						server.printer.print("waiting for OPP move board", "");
						

					} else {

                        match.setTurn(new int[] { 1, 0 });
                        match.setRound(match.getRound() + 1);

                        server.printer.printLine("\n *** My turn ***\n", "");
                        printMatchInfo();

						return;
					}
					server.fibsout.println("board");
					return;
				}

				if (inputHasBoard && wOppMoveBoard) {

                    server.printDebug("got OPP move board...");
					wOppMoveBoard = false;
					match.setTurn(new int[] { 1, 0 });
					match.setRound(match.getRound() + 1);

					/*if(match.isPostcrawford() && canDouble(in)[0]
					               && match.getRound() == 1) {
						//match.setPostcrawford(false);
						server.printDebug("POSTCRAWFORD DOUBLING");
						sleepFibs(100);
						server.fibsout.println("double");
					
					} else */
                    if (match.getMl() > 1 && inputBoard.iMayDouble()
							&& !match.isCrawford() && !match.oneToWin()) {

						server.printDebug("sending. [candouble: " + inputBoard.iMayDouble()
								+ " " + inputBoard.oppMayDouble() + " ml: "
								+ match.getMl() + " score: " + inputBoard.getMyScore()
								+ " " + inputBoard.getOppScore() + " crawford: "
								+ match.isCrawford() + "]");

						//String _eqBrd = getEquitiesBoard(lastBoard);
						//server.gnubg.execBoard(_eqBrd);

						server.bgRunner.execBoard(in);

					} else {
						server.printDebug("NOT sending. [candouble: "
								+ inputBoard.iMayDouble() + " " + inputBoard.oppMayDouble()
								+ " ml: " + match.getMl() + " score: "
								+ inputBoard.getMyScore() + " " + inputBoard.getOppScore()
								+ " crawford: " + match.isCrawford() + "]");
					}

                    server.printer.printLine("\n *** My turn ***\n", "");
                    printMatchInfo();

					return;

				}

				if (in.toLowerCase().startsWith(
						match.getPlayer1().toLowerCase() + " doubles.")) {
					server.printDebug("got OPP doubles, waiting for OPP double board");
					//printMatchInfo();

					wOppDoubleBoard = true;
					server.fibsout.println("board");
					return;
				}
				if (in.startsWith("You accept the double.")) {
					server.printDebug("last doubled in round: " + doubledInRound);
					if(match.getRound() > doubledInRound) {
						server.printDebug("OPP's double accepted");
						match.setCube(match.getCube() * 2);
						doubledInRound = match.getRound();

                        server.printer.printLine("\n *** OPP's turn ***\n", "");
                        printMatchInfo();

						return;
					}
				}
				if (inputHasBoard && wOppDoubleBoard) {

                    server.printer.printLine("\n *** My turn ***\n", "");
                    printMatchInfo();

					server.printDebug("got OPP double board, sending to bgsocket");
					wOppDoubleBoard = false;
					server.bgRunner.execBoard(in);
				}
			}
		} //// END: PROCESS TURN
	} //// END: PROCESSGAMEPLAY

	private void startMatch(String in) {
		if(server.getFibsMode() == FIBS_MODE_GAME_PLAY) {
			// should never happen
			server.printDebug(" ** !!! BUG !!! ** ");
			return;
		}
		server.printDebug("startMatch, old fibsmode:" + server.getFibsMode());
	
		server.stopInviter();

		server.bgRunner.startGnubg();
	
		String[] arr0, arr1, arr2;
		String oppname = "", mlstr = "0";
		if (in.contains(" has joined you for a ")) {
	
			arr0 = in.split(" has joined you for a ");
			arr1 = arr0[0].split(" Player ");
			oppname = arr1[1];
			arr2 = arr0[1].split(" point match.");
			mlstr = arr2[0];
			server.printDebug("own invitation - NEW match");
		} else if (in.contains("has joined you.")) {
			arr0 = in.split("has joined you.");
			oppname = arr0[0].trim();
			mlstr = "0";
			resetResumeBits();
			resume = true;
			server.printDebug("own invitation - RESUME match (oppname: " + oppname
					+ ")");
		} else if (in.startsWith("You are now playing with")) {
			arr0 = in.split(" are now playing with ");
			arr1 = arr0[1].split(
					". Your running match was loaded.");
			oppname = arr1[0];
			mlstr = "0";
			resetResumeBits();
			resume = true;
			server.printDebug("opponent's invitation - RESUME match (oppname: "
					+ oppname + ")");
		} else if (in.startsWith("** You are now playing a ")) {
			arr0 = in.split("You are now playing a ");
			arr1 = arr0[1].split(" point match with ");
			oppname = arr1[1];
			mlstr = arr1[0];
			server.printDebug("opponent's invitation - NEW match (oppname: " + oppname
					+ ")");
		}
		
		if(lastmatch != null) {
			lastmatch.cancelWaitfor();
			lastmatch = null;
		}
		
		try {
	
			int ml = Integer.parseInt(mlstr);
			this.match = new Match(server, oppname, ml);
			server.matchCount++;
			server.setFibsMode(2);

			//while (!server.bgsocket.run) {}
	
			server.printDebug("new fibsmode:" + server.getFibsMode() + ", round: "
					+ match.getRound() + " leaving startMatch");
	
			setInvitationInProgress(false);
			getOwnRating();
			getOppRating();
		} catch (NumberFormatException e) {
			server.systemPrinter.printLine(this.getClass().toString() + "(startMatch): " + e);
		}
	
	}

	private void stopMatch() {

		server.bgRunner.killGnubg();

        match.setFinished(true);
        match.purgeStampTimer();
		
		sleepFibs(200);
		server.fibsout.println("toggle r");
		
		if (server.getSavedMatches().containsValue(match.getPlayer1()))
			removeSavedMatch(match.getPlayer1());
		
		match.setWaitRateCalc(false);

		String star, score; // = (match.isDropping()) ? "*" : "";
		int rounds;	
		
		// UPDATE MATCH LOG FOR LAST GAME
		server.printDebug("UPDATING PLAYERSTATS for last game");		
		if (match.isDropped()) {
			
			if(!terminating) {
				server.printDebug("match DROPPED, waiting for: " + match.getPlayer1());
				match.setWaitfor(match.getPlayer1());
				match.waitfortimer.schedule(match.waitfortimertask, 45000L);
			}
			match.setDroppedat(Calendar.getInstance().getTime());
			
			star = "*";
			rounds = match.getRound();
			score = match.getScore()[0] + "-" + match.getScore()[1];
			server.printDebug("  > DROPPED match (round: " + rounds + " score: " + score + star + ")");
			
		} else {
			
			server.setLastopp(match.getPlayer1());
			server.setLastfinish(Calendar.getInstance().getTime());
			sleepFibs(250);
			server.fibsout.println("tell " + match.getPlayer1()
					+ " Thanks for playing.");
			
			star = " ";
			rounds = match.roundsave;
			score = match.finalscore;
			server.printDebug("  > NOT dropped match (round: " + rounds + " score: " + score + star + ")");
	
		}
		
		// --------> TODO move this code and instances to a method (x 3 occurences now)
	
		// update rpg
		match.roundspergames.put(match.getGameno(), rounds);
		match.roundsave = 0;
		
		// update timehistory
		//String tmp2 = match.getTime() / 1000 / 60 + ":"
		//+ (match.getTime() / 1000 - match.getTime() / 1000 / 60 * 60);
		match.timehistory.put(match.getGameno(), match.getGameTime());
		match.setGameTime(Calendar.getInstance().getTimeInMillis());
		
		// update scorehistory
		match.scorehistory.put(match.getGameno(), score + star);
		// <-----------
		
		
		// playerstat
		if(server.playerstats != null) {
			
			if(!server.playerstats.hasPlayer(match.getPlayer1())) {
				server.printDebug("CREATING PLAYER: " + match.getPlayer1());
				server.playerstats.cratePlayer(match.getPlayer1());
			} else {
				server.printDebug("PLAYER " + match.getPlayer1() + " exists");
			}
			
			
			server.printDebug("PUTTING match to PlayerStats");
			if(match.wasResumed()) {
				server.playerstats.getByName(match.getPlayer1())
				.appendMatch(match);
			} else {
			server.playerstats.getByName(match.getPlayer1())
				.putMatch(match);
			}
			
			
			server.printDebug("WRITING PlayerStats...");
			if(server.writePlayerStats(server.getPstatspath())) {
				server.printDebug("wrote: " + server.getPstatspath());
			} else {
				server.printDebug("COULDN'T WRITE PlayerStats! (" + server.getPstatspath() + ")");
			}
		}
		
	
		if(match.isDropped())  {
			lastmatch = match;
		}
		match = null;
		server.setFibsMode(0);
		wasResumed = false;		
		server.printDebug("*** match terminated ***");	
		sleepFibs(1200);
		server.fibsout.println("toggle r");
        server.fibsout.println("whois " + server.prefs.getUsername());

		sleepFibs(300);
		getSavedMatches();
	}

	private void getOwnRating() {
		getOwnRating = true;
		server.printDebug("Start getting own rating (who " + server.prefs.getUsername() + ")" );
		server.fibsout.println("who " + server.prefs.getUsername());
	
	}

	private void getOppRating() {
		getOppRating = true;
		server.printDebug("Start getting OPP rating (who " + match.getPlayer1() + ")" );
		server.fibsout.println("who " + match.getPlayer1());
	}

	private void greetOpponent(FibsBoard _board) {
		if (match.oppgreeted)
			return;

		if(match.getRound() == 1 && match.oppgreetphase == 0) {
			
			match.oppgreetphase = 1;
			sleepFibs(200);
			server.fibsout.println("kibitz Hi " + _board.getOppName() + ", gl!");
			sleepFibs(200);
			server.fibsout.println("kibitz I am a computer program, kibitz 'tell me more' if you want.");
			
		} else if(match.getRound() == 2 ) {
			
			if(match.oppgreetphase == 1) {
				match.oppgreetphase = 2;
				sleepFibs(200);
				server.fibsout.println("kibitz Kibitz 'move' or 'roll' if I stop responding.");
			
			} else if(match.oppgreetphase == 2) {
				match.oppgreetphase = 3;
				match.oppgreeted = true;
				int s;
				if(server.playerstats.hasPlayer(match.getPlayer1())) {
					if((s = server.playerstats.getByName(match.getPlayer1())
							.getMatchcount()) > 0) {
						sleepFibs(350);
						server.fibsout.println("kibitz or tell me any time "
							+ "'show stat' to get overall stats or 'show log' "
							+ "to get a game level log of the "
							+ s + " match" + (s > 1 ? "es" : "") + " we played (since "
							+ server.playerstats.getByName(match.getPlayer1()).getFirstMatchDate() + ").");
					}
				}				
			}
		}
	}

	private String getEquitiesBoard(FibsBoard board) {
		String[] split = board.getRawInput().split(":");
		split[32] = board.getDirection() == 1 ? "-1" : "1";

		return String.join(":", split);
	}

	public void getSavedMatches() {
		getSavedMatches = true;
		server.fibsout.println("show saved");
	}

	private void removeSavedMatch(String player) {
		int removethis = -1;
		for(int key : server.getSavedMatches().keySet()) {
			if(server.getSavedMatches().get(key).equals(player)) {
				removethis = key;
			}
		}
        synchronized (MatchDog.lock) {
            try {
                while (gettingSaved) {
                    MatchDog.lock.wait();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
		server.getSavedMatches().remove(removethis);
	}

	private void resetResumeBits() {
		resumeBits[0] = false;
		resumeBits[1] = false;
		resumeBits[2] = false;
		resumeBits[3] = false;
		wasResumed = false;
	}

	private void resetWaitFlags() {
		wOppMoveBoard = false;
		wOppDoubleBoard = false;
		// wDoubleBoard = false;
		wRollBoard = false;
		wResumeBoard = false;
	
		wScoreBoard = false;
		doubledInRound = 0;
	}

	private boolean isInvitationInProgress() {
		return invitationInProgress;
	}

	private void setInvitationInProgress(boolean invitationInProgress) {
		this.invitationInProgress = invitationInProgress;
	}

	protected void setShowMsgType(int index, boolean invert) {
		if (invert) {
			for (int i = 0; i < showMsgType.length; i++) {
				showMsgType[i] = (showMsgType[i] == 0) ? 1 : 0;
			}
		}
		else {
			showMsgType[index] = (showMsgType[index] == 0) ? 1 : 0;
		}
	}

	protected void login() {
		String cmd = "login " + server.programPrefs.getPlatform()
		+ " 1008 "
		+ server.prefs.getUsername() + " "
		+ server.prefs.getPassword();

		server.fibsout.println(cmd);
	}

	protected void printMatchInfo() {
		if(match == null) {
			return;
		}

		matchInfoPrinter.printMatchInfo();
	}

	protected void printFibsCommand(String fibscommand) {
		fibsCommandPrinter.printFibsCommand(fibscommand);
	}

	protected void terminate() {
		if(terminating) {
			return;
		}
		terminating = true;
		
		server.systemPrinter.printLine("Terminating FibsRunner - " + getName());
		if(match != null) {
			server.printDebug("Terminating FibsRunner: dropping match");
			match.setDropped(true);
			stopMatch();
			
			if(!s.isOutputShutdown() && s.isConnected() && !s.isClosed()) {
				server.fibsout.println("leave");
			}
		}

        if(server.fibsout != null && !s.isOutputShutdown() && loggedIn) {
            server.fibsout.println("logout");
            server.printDebug("Terminating FibsRunner: sent 'logout'");
        }
		
		try {
			s.shutdownInput();
			s.shutdownOutput();
			FibsRunner.sleep(800);
			
			input.close();
			sin.close();
			server.fibsout.close();
			server.fibsos.close();
			s.close();
	
		} catch(IOException e) {
			server.systemPrinter.printLine("Exception closing socket to fibs: " + e.getMessage());
		} catch(InterruptedException e) {
			server.systemPrinter.printLine("Exception sleeping fibs thread (in terminate()): " + e.getMessage());
		} catch(Exception e) {
			server.systemPrinter.printLine("Exception (in terminate()): " + e.getClass() + " > " + e.getMessage());
			e.printStackTrace();
		}
		server.systemPrinter.printLine("Terminating FibsRunner - " + getName() + " s.closed: " + s.isClosed() );
		
	}

	protected void sleepFibs(long millis) {
		try {
			FibsRunner.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected void keepAlive() {
		sleepFibs(250);
		server.fibsout.println("whois " + server.prefs.getUsername());
	}
	
	protected long getFibsLastLineSecondsAgo() {
		return (System.nanoTime() - lastLine) / 1000000000;
	}
	
	private void restart() {
		server.restartFibs();
	}
	
	protected 
	synchronized
	void 
	statUploaderFinished(Boolean ok, String filename) {
		
		server.printDebug( " > Looking for filename: " + filename);
		String player = null;
		for(String p : server.statrequests.keySet()) {
			server.printDebug(p + " : " + server.statrequests.get(p));
			if(server.statrequests.get(p).equals(filename)) {
				player = p;
				if(ok) {
					server.printDebug(" > SENDING URL to player: ");
					sleepFibs(200);
					server.fibsout.println("tell " + p + " "
							+ "http://dandre.hu/~marekful/fibs_stats/" 
							+ filename);
				} else {
					sleepFibs(200);
					server.fibsout.println("tell " + p + " "
							+ "There was en error. No statistics available");
				}
				break;
			}
		}
		if(player != null)
			server.statrequests.remove(player);
	}
}
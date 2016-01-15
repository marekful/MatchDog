package matchdog;

import java.util.HashMap;
import java.util.Map;
import java.net.*;
import java.io.*;

public class MatchDogServer {
	
	/* SET PROGRAM PARAMETERS HERE */
	/// SET PROGRAM PARAMETERS HERE
	// Fibs host
	public static final String 	fibshost 	= "fibs.com";
	// Fibs port
	public static final int 	fibsport 	= 4321;
	// Full path to gnubg executable and params (-t for no gui, -q for silent)
	public static final String 	gnubgcmd 	= "/Applications/gnubg.app/Contents/MacOS/local/bin/gnubg -t -q";
	// A small shell script that uploads match log files
	// comes with the package and should be installed
	// so it's executable by the server. Give its path here.
	public static final String 	ftpscriptpath 	= "/bin/./ftpupload.sh";	
	
	public static final String platform = "MatchDogServer";
	public static final String version = "0.5";
	/*** <-- ***/
	
	HashMap<Integer, String> globalblacklist = new HashMap<Integer, String>();
	
	int defaultplayer;
	int currentplayer;
	Map<Integer, PlayerPrefs> players;
	
	public static void main(String [] args) {
		new MatchDogServer(args);
	}

	
	MatchDogServer(String [] args) {

		
		players = new HashMap<Integer, PlayerPrefs>();
		
		// TODO
		// player prefs object should be reorganized
		// player preferences from file or db.
		
		/* PLAYER PREFERENCES */
		/**
		 * The <code>playerPrefs</code> constructor requires a one dimensional
		 * <code>Object</code> array in the following structure:
		 * 
		 * index  0: fibs user name -- String
		 * index  1: fibs password -- String
		 * index  2: autologin -- true|false
		 * index  3: autoinvite (preferred opps.) -- true|false
		 * index  4: autojoin (opp's invitation) -- true|false
		 * index  5: autoinvite for saved match -- true|false
		 * index  6: autojoin for saved match -- true|false
		 * index  7: maximum match length -- int
		 * index  8: opponent experience divider -- int
		 * index  9: reputation limit -- int
		 * index 10: gnubg checkquer play ply -- int
		 * index 11: gnubg cube decision ply -- int
		 * index 12: gnubg noise -- double
		 * index 13 - 17: gnubg 1 ply movefilter values 
		 *   (ply | ply level | enable | always accept | within)
		 *    -- int -- int -- 0|-1 -- int -- double 
		 * index 18 - 27: gnubg 2 ply movefilter values 
		 * index 28 - 42: gnubg 3 ply movefilter values
		 * index 43 - 62: gnubg 4 ply movefilter values 
		 * index 63 - 72: preffered player list ([player name, match length] x 5)
		 * 	  -- String -- int
		 */ 
		Object [] prefs;
		prefs = new Object [] {
				"tormonitor", "m0n1t0r", 			// user, pw
				true, false, false, 		// autologin, autoinvite, autojoin,
				false, false, 0,			// autoinvite saved, autojoin saved, max ml.
				0, 0,						// opp. exp. divider, reputation limit
				3, 3, 0.000,				// checquerplay ply, cubedec. ply, noise
				
				1,  0,   0, 16, 0.32,		// movefileters (x 10x5)
				2,  0,   0, 24, 0.42,
				2,  1,   0, 16, 0.28,
				3,  0,   0,  8, 0.32,
				3,  1,  -1,  0, 0,
				3,  2,   0,  6, 0.08,
				4,  0,   0, 16, 0.32,
				4,  1,  -1,  0, 0,
				4,  2,   0,  4, 0.08,
				4,  3,  -1,  0, 0,			
				
				"", 0,						// preferred player, ml (x 10x2)
				"", 0,
				"", 0,
				"", 0,
				"", 0,
				
				"", 0,
				"", 0,
				"", 0,
				"", 0,
				"", 0,
				
				0, 0					// gnubg external port port
										// if 0 (zero), gnubg is not started up
										// (tormonitor mode ;))
		};
		players.put(0, new PlayerPrefs(prefs));
		prefs = new Object [] {
				"marekful", "malako",
				false, false, true,
				true, true, 15,
				100, -22000,
				3, 3, 0.0,			
				
				1,  0,   0,  16, 0.32,		// movefileters (x 10x5)
				2,  0,   0,  24, 0.42,		
				2,  1,  -1,  0,  0.0,
				3,  0,   0,  8,  0.20,
				3,  1,   0,  8,  0.10,
				3,  2,   0,  2,  0.08,
				4,  0,   0,  16, 0.32,
				4,  1,  -1,  0, 0,
				4,  2,   0,  4, 0.08,
				4,  3,  -1,  0, 0,						
				
				"GammonBot_IX", 7,
				"GammonBot_VIII", 15,
				"GammonBot_VI", 7,
				"GammonBot_XIX", 7,
				"GammonBot_IV", 15,
				
				"", 0,
				"", 0,
				"", 0,
				"", 0,
				"", 0,
				
				4546, 0
		};		
		players.put(1, new PlayerPrefs(prefs));
		prefs = new Object [] {
				"MatchDogMini", "piko",
				true, true, true,
				true, true, 1,
				1, -16000,
				3, 3, 0.000,
				
				1,  0,   0,  24, 0.44,		// movefileters (x 10x5)
				2,  0,   0,  24, 0.32,		
				2,  1,  -1,  0,  0.0,
				3,  0,   0,  24,  0.32,
				3,  1,  -0,  4, 0.08,
				3,  2,   0,  8, 0.11,
				4,  0,   0,  20, 0.32,
				4,  1,  -1,  0, 0,
				4,  2,   0,  8, 0.11,
				4,  3,  -1,  0, 0,					
				
				"MonteCarlo", 1,
				"GammonBot_IX", 1,
				"GammonBot_IV", 1,
				"GammonBot_X", 1,
				"GammonBot_VI", 1,
				
				"GammonBot_XVIII", 1,
				"GammonBot_XVI", 1,
				"GammonBot_XIII", 1,
				"GammonBot_II", 1,
				"GammonBot_VIII", 1,
				
				4537, 55222
		};		
		players.put(2, new PlayerPrefs(prefs));
		prefs = new Object [] {
				"", "",
				false, false, true,
				true, true, 5,
				100, 0,
				0, 0, 0.700,
				
				1,  0,   0,  8, 0.16,		// movefileters (x 10x5)
				2,  0,   0,  8, 0.16,
				2,  1,  -1,  0, 0,
				3,  0,   0, 10, 0.18,
				3,  1,  -1,  0, 0,
				3,  2,   0,  4, 0.08,
				4,  0,   0,  8, 0.16,
				4,  1,  -1,  0, 0,
				4,  2,   0,  2, 0.04,
				4,  3,  -1,  0 ,0,		
				
				"", 0,
				"", 0,
				"", 0,
				"", 0,
				"", 0,
				
				"", 0,
				"", 0,
				"", 0,
				"", 0,
				"", 0,
				
				0, 0

		};		
		players.put(3, new PlayerPrefs(prefs));
		prefs = new Object [] {
				"MatchDog", "malako",
				true, false, true,
				false, false, 15,
				100, -22000,
				3, 3, 0.0,			
				
				1,  0,   0,  24, 0.44,		// movefileters (x 10x5)
				2,  0,   0,  24, 0.32,		
				2,  1,  -1,  0,  0.0,
				3,  0,   0,  24,  0.32,
				3,  1,  -0,  4, 0.08,
				3,  2,   0,  8, 0.11,
				4,  0,   0,  20, 0.32,
				4,  1,  -1,  0, 0,
				4,  2,   0,  8, 0.11,
				4,  3,  -1,  0, 0,	
				
				"GammonBot_XVIII", 11,
				"GammonBot_IX", 11,
				"GammonBot_IV", 11,
				"GammonBot_X", 11,
				"GammonBot_VI", 11,
				
				"GammonBot_XVIII", 11,
				"GammonBot_XVI", 11,
				"GammonBot_XIII", 11,
				"GammonBot_II", 11,
				"GammonBot_XII", 11,
				
				4548, 55221
		};		
		players.put(4, new PlayerPrefs(prefs));
		
		//globalblacklist.put(globalblacklist.size(), "roderick");
		//globalblacklist.put(globalblacklist.size(), "fimo");
		//globalblacklist.put(globalblacklist.size(), "BlindTal");
		globalblacklist.put(globalblacklist.size(), "sackofjaweea");
		
		defaultplayer = 0;
		
		if(args.length < 1) {
			currentplayer = defaultplayer;
		} else {
			try {
				currentplayer = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				currentplayer = defaultplayer;
			}
			if(currentplayer > players.size() - 1) {
				currentplayer = defaultplayer;
			}
		}		
		
		MatchDog g = new MatchDog(players.get(currentplayer), globalblacklist);
		g.start();
		
		if(g.prefs.getListenerPort() > 0) {
			startServer(g);
		}

	}
	
	private synchronized void startServer(MatchDog g) {
		ServerSocket ss;
		Socket sss;
		InputStream ssins;
		OutputStream ssouts;
		PrintWriter ssout;
		BufferedReader ssin;
		try {
			
			ss = new ServerSocket(g.prefs.getListenerPort());
			
			while(true) {
				g.printDebug(">>> waiting for connection on port " + g.prefs.getListenerPort());
				sss = ss.accept();
				g.printDebug(">>> connection from " + sss.getLocalSocketAddress()	);
				
				PrintWriter _p = new PrintWriter(sss.getOutputStream());
				_p.print("Hello");
				_p.println();
				_p.flush();
				
				g.listen(sss.getInputStream(), sss.getOutputStream());
			}

		} catch(Exception e) {
			e.printStackTrace();
		}
		g.printDebug(">>> listener stopped ");
		//g.setScanner(System.in);
	}
}

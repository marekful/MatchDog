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
	// Array of full paths to gnubg executable and params (-t for no gui, -q for silent)
    // Each will be tried, firsst succeeds used
    public static final String[] gnubgCmdArr = new String[] {
        "/Applications/gnubg.app/Contents/MacOS/local/bin/gnubg -t -q",
        "/usr/local/chroot/home/marekful/gnubg/gnubg-1.05.000/gnubg -t -q"
    };
    // A small shell script that uploads match log files
	// comes with the package and should be installed
	// so it's executable by the server. Give its path here.
	public static final String 	ftpscriptpath 	= "/bin/./ftpupload.sh";
	public static final String 	scpscriptpath 	= "../scripts/./scp.sh";


	public static final String platform = "MatchDogServer";
	public static final String version = "0.5";
	/*** <-- ***/
	
	HashMap<Integer, String> globalblacklist = new HashMap<Integer, String>();
	
	int defaultplayer;
	int currentplayer;
	Map<Integer, PlayerPrefs> players;
	boolean listenLocal = true;

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
				false, false, false,
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
				
				4546, 55444
		};		
		players.put(1, new PlayerPrefs(prefs));
		prefs = new Object [] {
				"MatchDogMini", "piko",
				true, true, true,
				true, true, 1,
				1, -16000,
				3, 3, 0.000,
				
				1,  0,   0,  20, 0.32,		// movefileters (x 10x5)
				2,  0,   0,  20, 0.32,		
				2,  1,  -1,  0,  0.0,
				3,  0,   0,  20,  0.32,
				3,  1,  -0,  4, 0.08,
				3,  2,   0,  6, 0.08,
				4,  0,   0,  20, 0.32,
				4,  1,  -1,  0, 0,
				4,  2,   0,  6, 0.08,
				4,  3,  -1,  0, 0,					
				
				"MonteCarlo", 1,
				"", 1,
				"", 1,
				"", 1,
				"", 1,
				
				"", 1,
				"", 1,
				"", 1,
				"", 1,
				"", 1,
				
				4537, 55272
		};		
		players.put(2, new PlayerPrefs(prefs));
		prefs = new Object [] {
				"", "",
				true, false, true,
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
				true, true, true,
				true, true, 15,
				100, -22000,
				3, 3, 0.0,			
				
				1,  0,   0,  20, 0.32,		// movefileters (x 10x5)
				2,  0,   0,  20, 0.32,		
				2,  1,  -1,  0,  0.0,
				3,  0,   0,  20,  0.32,
				3,  1,  -0,  4, 0.08,
				3,  2,   0,  6, 0.08,
				4,  0,   0,  20, 0.32,
				4,  1,  -1,  0, 0,
				4,  2,   0,  6, 0.08,
				4,  3,  -1,  0, 0,	
				
				"bonehead", 7,
				"", 11,
				"", 11,
				"", 11,
				"", 11,
				
				"", 11,
				"", 11,
				"", 11,
				"", 11,
				"", 11,
				
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
			
			if(args.length > 1) {
				try {
					listenLocal = Integer.parseInt(args[1]) != 0;
				} catch (NumberFormatException e) {
					listenLocal = true;
				}
			}
		}		
		
		MatchDog g = new MatchDog(
			players.get(currentplayer),
			globalblacklist,
			getLocalHostname(),
			listenLocal
		);
		g.start();
		
		if(g.prefs.getListenerPort() > 0) {
			SocketServer ss = new SocketServer(g);
            ss.acceptNewConnection();
		}
	}
	
	class SocketServer {

		ServerSocket serverSocket = null;
        MatchDog matchdog;
        int listenerPort;

        SocketServer(MatchDog matchdog) {
            this.matchdog = matchdog;
            listenerPort = matchdog.prefs.getListenerPort();
            createServer();
        }

        private void createServer() {
            while (true) {
                try {
                    serverSocket = new ServerSocket(listenerPort);
                    matchdog.systemPrinter.printDebugln(">>> Waiting for connection on port " + listenerPort);
                    System.out.println("Listening on port " + listenerPort); // print to stdout in case we started in background with listenLocal=0
                    break;
                } catch (BindException e) {
                    listenerPort++;
                } catch (Exception e) {
                    matchdog.systemPrinter.printDebugln(">>> Exception creating socket: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }


            if (serverSocket == null) {
                matchdog.systemPrinter.printDebugln(">>> Couldn't create socket, exiting listener");
            }
        }

        public void acceptNewConnection() {
            (new Thread(new ConnectionHandle())).start();
        }

        class ConnectionHandle implements Runnable {

            Socket clientSocket;

            ConnectionHandle() {}

            @Override
            public void run() {
                try {
                    clientSocket = serverSocket.accept();
                    matchdog.systemPrinter.printDebugln(
                            ">>> Connection from " + clientSocket.getRemoteSocketAddress()
                    );

                    acceptNewConnection();

                    PrintWriter _p = new PrintWriter(clientSocket.getOutputStream());
                    _p.print("Hello from " + clientSocket.getRemoteSocketAddress());
                    _p.print(" This is " + matchdog.prefs.getName() + "@" + matchdog.hostName);
                    _p.println();
                    _p.flush();

                    matchdog.listen(
                            clientSocket.getInputStream(),
                            new PrintStream(clientSocket.getOutputStream())
                    );
                    matchdog.systemPrinter.printDebugln(
                            ">>> Connection from " + clientSocket.getRemoteSocketAddress() + " closed"
                    );

                } catch (Exception e) {
                    System.out.println(" >> >> >> >> " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
	}

	private String getLocalHostname() {
		try {
			String result = InetAddress.getLocalHost().getHostName();
			if (result.trim() != "")
				return result;
		} catch (UnknownHostException e) {}

		String host = System.getenv("COMPUTERNAME");
		if (host != null)
			return host;
		host = System.getenv("HOSTNAME");
		if (host != null)
			return host;

		return "Unknown";
	}
}



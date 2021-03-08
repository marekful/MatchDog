package matchdog;

import java.util.HashMap;
import java.util.Map;
import java.net.*;
import java.io.*;
import java.util.Properties;

public class MatchDogServer {

	HashMap<Integer, String> globalblacklist = new HashMap<Integer, String>();
	
	int defaultplayer;
	int currentplayer;
    Map<String, String> prefs;
	Map<Integer, PlayerPrefs> players;
	boolean listenLocal = true;

	public static void main(String [] args) {
		new MatchDogServer(args);
	}
	
	MatchDogServer(String [] args) {

        prefs = new HashMap<String, String>();
		players = new HashMap<Integer, PlayerPrefs>();

		///globalblacklist.put(globalblacklist.size(), "sackofjaweea");

        initPlayers();

		defaultplayer = 0;
		
		if(args.length < 1) {
			currentplayer = defaultplayer;
		} else {
			try {
				currentplayer = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
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
            new ProgramPrefs(),
			players.get(currentplayer),
			globalblacklist,
			getLocalHostname(),
			listenLocal
		);
        (new Thread(g)).start();
		
		if(g.prefs.getListenerport() > 0) {
			SocketServer ss = new SocketServer(g);
            ss.acceptNewConnection();
		}
	}

    private void initPlayers() {
        for(int p = 1; p < 11; p++) {
            try {
                Properties playerPrefs = new Properties();
                FileInputStream in = new FileInputStream("../config/" + p + ".pprefs");
                playerPrefs.load(in);
                in.close();

                PlayerPrefs prefs = new PlayerPrefs(playerPrefs);
                players.put(p, prefs);


            } catch (IOException e) { }
        }
    }
	
	private class SocketServer {

		ServerSocket serverSocket = null;
        MatchDog matchdog;
        int listenerPort;

        SocketServer(MatchDog matchdog) {
            this.matchdog = matchdog;
            listenerPort = matchdog.prefs.getListenerport();
            createServer();
        }

        private void createServer() {
            while (true) {
                try {
                    serverSocket = new ServerSocket(listenerPort);
                    matchdog.socketServerPrinter
                            .printLine(">>> Waiting for connection on port " + listenerPort);

					if(!listenLocal) {
						System.out.println("Listening on port " + listenerPort); // print to stdout in case we started in background with listenLocal=0
					}
                    break;
                } catch (BindException e) {
                    listenerPort++;
                } catch (Exception e) {
                    matchdog.socketServerPrinter.printLine(">>> Exception creating socket: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }

            if (serverSocket == null) {
                matchdog.socketServerPrinter.printLine(">>> Couldn't create socket, exiting listener");
            }
        }

        public void acceptNewConnection() {
            (new Thread(new ConnectionHandle())).start();
        }

        private class ConnectionHandle implements Runnable {

            Socket clientSocket;

            ConnectionHandle() {}

            @Override
            public void run() {
                try {
                    clientSocket = serverSocket.accept();
                    matchdog.socketServerPrinter
                            .printLine(">>> Connection from " + clientSocket.getRemoteSocketAddress());

                    acceptNewConnection();

                    greet(clientSocket.getOutputStream());

                    matchdog.listen(
                            clientSocket.getInputStream(),
                            new PrintStream(clientSocket.getOutputStream())
                    );
                    matchdog.socketServerPrinter.printLine(
                            ">>> Connection from " + clientSocket.getRemoteSocketAddress() + " closed"
                    );

                } catch (Exception e) {
                    System.out.println(" >> >> >> >> " + e.getMessage());
                    e.printStackTrace();
                }
            }

            private void greet(OutputStream os) {
                PrintWriter _p = new PrintWriter(os);
                _p.print("Hello from " + clientSocket.getRemoteSocketAddress());
                _p.print(" This is " + matchdog.prefs.getUsername() + "@" + matchdog.hostName);
                _p.println();
                _p.flush();
            }
        }
	}

	private String getLocalHostname() {
		try {
			String result = InetAddress.getLocalHost().getHostName();
			if (!result.trim().equals(""))
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



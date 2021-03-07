package matchdog;
import jcons.src.com.meyling.console.UnixConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class BGRunner  {

    // gnubg process
	private Process p;
	private final String[] gnubgCommands;
	private final MatchDog server;
	private final BufferedConsolePrinter printer;

    private BufferedReader pIn;
    private PrintWriter pOut;

    // gnubg external
    private Socket s = null;

    private BufferedReader sIn;
    private PrintWriter sOut;

    protected BufferedConsolePrinter matchPrinter;
    protected BufferedConsolePrinter eqPrinter;
    private boolean connected, evalCmd;

	BGRunner(String[] command, MatchDog server) {

        // proc
		p = null;
		this.gnubgCommands = command;
		this.server = server;
		printer = new BufferedConsolePrinter(
			server, "gnubg:", UnixConsole.LIGHT_YELLOW, UnixConsole.BACKGROUND_BLUE
		);

        // sock
        connected = false;
        evalCmd = false;

        matchPrinter = new BufferedConsolePrinter(
            server, "gnubg:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_BLUE
        );
        eqPrinter = new BufferedConsolePrinter(
            server, "Equities:", UnixConsole.LIGHT_YELLOW, UnixConsole.BACKGROUND_BLACK
        );
	}

    // proc
	public boolean launch() {
        for (String cmd : gnubgCommands) {
            try {
                printer.printDebugln("Trying to launch gnubg binary");
                p = Runtime.getRuntime().exec(cmd);
                printer.printDebugln("gnubg running (" + cmd + ")");
                break;
            } catch (Exception e) {
                printer.printDebugln("gnubg not found at: " + cmd);
            }
        }

        if (p == null) {
            printer.printDebugln("Couldn't launch gnubg, exiting...");
            return false;
        }

        pIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
        pOut = new PrintWriter(p.getOutputStream(), true);

        return true;
    }

    public void setup() {

        int checkquerply = server.prefs.getCheckquerply();
        int cubedecply = server.prefs.getCubedecply();
        println("show version");

        println("set eval sameasanalysis off");

        println("set chequer evaluation prune on");
        if(server.prefs.getMaxml() == 1) {
            println("set evaluation chequer eval cubeful off");
        }
        println("set threads 4");

        println("set evaluation chequer eval plies " + checkquerply);
        println("set evaluation cubedecision eval plies " + cubedecply);

        for(int i = 0; i < 10; i++) {
            println("set evaluation movefilter " + server.prefs.getMoveFilter(i) );
            server.printDebug(server.prefs.getMoveFilter(i));
        }

        println("set rollout chequer plies " + checkquerply);
        println("set rollout cubedecision plies " + cubedecply);

        for(int i = 0; i < 10; i++) {
            println("set movefilter " + server.prefs.getMoveFilter(i) );
            server.printDebug(server.prefs.getMoveFilter(i));
        }

        //println("save settings");

        println("external localhost:" + server.prefs.getGnuBgPort());
	}

	public void terminate() {
        closeSocket();
		println("quit");
		p.destroy();
	}

	public void restartGnubg() {
        try {
            Thread.sleep(150);
            terminate();
            Thread.sleep(150);
            if (!launch()) {
                server.stopServer();
                return;
            }
            setup();
            connectSocket();
        } catch (InterruptedException ignored) {}
    }

    private void println(String str) {
        pOut.println(str);
        try {
            Thread.sleep(24);
        } catch (InterruptedException e) {
            return;
        }
        processInput();
    }

    protected void processInput() {
        try {
            while((pIn.ready())) {
                printer.printDebugln(pIn.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // sock
    public synchronized void execBoard(String board) {
        execCommand(board);
    }

    public synchronized void execEval(String board) {
        setEvalCmd(true);
        execCommand("evaluation fibsboard " + board + " PLIES 3 CUBE ON CUBEFUL");
        setEvalCmd(false);
    }

    private synchronized void execCommand(String lineIn) {

        if(lineIn.trim().equals("")) {
            matchPrinter.printDebugln("*** !! *** NOT SENDING empty line");
            return;
        }

        GnubgCommand r = new GnubgCommand(server, sIn, sOut, matchPrinter, eqPrinter, lineIn.trim(), isEvalCmd());

        // Process evalcmd synchronously (in the same thread) because these equities are used
        // to decide resignation later in the same invocation of FibsRunner.processGamePlay().
        // Process board state asynchronously (in separate thread) so FibsRunner can process
        // new input while waiting for gnubg's response.
        try {
            if(isEvalCmd()) {
                r.run();
            } else {
                (new Thread(r)).start();
            }
        } catch (RuntimeException e) {
            server.systemPrinter.printDebugln("Restarting gnubg now");
            restartGnubg();
        }
    }


    public void connectSocket() {

        while(!connected) {
            try {
                Thread.sleep(100);
                InetAddress sa = InetAddress.getByName("localhost");
                s = new Socket(sa, server.prefs.getGnuBgPort());
                if(s.isConnected()) {
                    server.systemPrinter.printDebugln("Successfully connected to bg socket");
                    connected = true;
                }
                sIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
                sOut = new PrintWriter(s.getOutputStream(), true);

            } catch(InterruptedException e) {
                return;
            } catch(Exception e) {
                server.systemPrinter.printDebugln(
                        "Exception in BGRunner.connectSocket(): " + e.getMessage() + ", retrying..."
                );
            }
        }
        processInput();
    }

    private void closeSocket() {
        try {
            sIn.close();
            sOut.close();
            s.close();
            connected = false;
        } catch (IOException e) {
            server.systemPrinter.printDebugln("Exception in BGRunner.closeSocket():" + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isEvalCmd() {
        return evalCmd;
    }

    private void setEvalCmd(boolean evalCmd) {
        this.evalCmd = evalCmd;
    }

    public PrintWriter getScokOut() {
        return sOut;
    }

    public PrintWriter getProcOut() {
        return pOut;
    }
}
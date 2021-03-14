package matchdog;
import jcons.src.com.meyling.console.UnixConsole;
import matchdog.console.printer.BufferedConsolePrinter;
import matchdog.console.printer.DefaultPrinter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class BGRunner  {

    // gnubg process
	private Process p;
	private long pid;
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
		printer = new DefaultPrinter(
			server, "gnubg:", UnixConsole.LIGHT_YELLOW, UnixConsole.BACKGROUND_BLUE
		);

        // sock
        connected = false;
        evalCmd = false;

        matchPrinter = new DefaultPrinter(
            server, "gnubg:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_BLUE
        );
        eqPrinter = new DefaultPrinter(
            server, "Equities:", UnixConsole.LIGHT_YELLOW, UnixConsole.BACKGROUND_BLACK
        );
	}

    public long getPid() {
        return pid;
    }

    // proc
	public boolean launch() {
        for (String cmd : gnubgCommands) {
            try {
                printer.printLine("Trying to launch gnubg binary");
                p = Runtime.getRuntime().exec(cmd);
                pid = p.pid();

                printer.setLabel("gnubg[pid=" + pid + "]:")
                       .printLine("gnubg running (" + cmd + ")");
                break;
            } catch (Exception e) {
                printer.printLine("gnubg not fouAnd at: " + cmd);
            }
        }

        if (p == null) {
            printer.printLine("Couldn't launch gnubg, exiting...");
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

        println("set evaluation chequer eval plies " + checkquerply);
        println("set evaluation cubedecision eval plies " + cubedecply);

        for(int i = 0; i < 10; i++) {
            server.printDebug("Setting movefilter: " + server.prefs.getMoveFilter(i));
            println("set evaluation movefilter " + server.prefs.getMoveFilter(i) );
            println("set rollout player 0 movefilter " + server.prefs.getMoveFilter(i));
            //-println("set rollout chequerplay movefilter " + server.prefs.getMoveFilter(i));
            //-println("set movefilter " + server.prefs.getMoveFilter(i));
        }

        println("set rollout chequer plies " + checkquerply);
        println("set rollout cubedecision plies " + cubedecply);
        if(server.prefs.getMaxml() == 1) {
            println("set evaluation chequer eval cubeful off");
            println("set evaluation cubedecision eval cubeful off");
            println("set rollout chequerplay cubeful off");
            println("set rollout cubedecision cubeful off");
        }


        printer.printLine("");
        println("show evaluation");
        printer.printLine("");
        println("show rollout");

        println("external localhost:" + server.prefs.getGnuBgPort());
	}

	public void killGnubg() {

        if (sIn != null) {
            closeSocket();
        }

        if (p != null && p.isAlive()) {
            if (!pOut.checkError()) {
                println("quit");
            }
            p.destroy();
        }

        p = null;
	}

	public void startGnubg() {
	    if (p != null && p.isAlive()) {
	        killGnubg();
        }

	    if (!launch()) {
	        throw new RuntimeException("Cannot launch gnubg binary");
        }

	    setup();
	    connectSocket();
    }

	public void restartGnubg() {
        try {
            Thread.sleep(150);
            killGnubg();
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
            server.systemPrinter.printLine("InterruptedException in BGRunner.println");
            return;
        }
        processInput();
    }

    protected void processInput() {
        try {
            while((pIn.ready())) {
                printer.printLine(pIn.readLine());
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
            matchPrinter.printLine("*** !! *** NOT SENDING empty line");
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
            server.systemPrinter.printLine("Restarting gnubg now");
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
                    server.systemPrinter.printLine("Successfully connected to bg socket");
                    matchPrinter.setLabel("gnubg[port=" + s.getPort() + "]");
                    connected = true;
                }
                sIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
                sOut = new PrintWriter(s.getOutputStream(), true);

            } catch(InterruptedException e) {
                return;
            } catch(Exception e) {
                server.systemPrinter.printLine(
                        "Exception in BGRunner.connectSocket(): " + e.getMessage() + ", retrying..."
                );
            }
        }
        processInput();
    }

    public void closeSocket() {
        try {
            sIn.close();
            sOut.close();
            s.close();
            connected = false;
        } catch (IOException e) {
            server.systemPrinter.printLine("Exception in BGRunner.closeSocket():" + e.getMessage());
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
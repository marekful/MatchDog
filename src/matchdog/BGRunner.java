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
	private String[] command;
	private MatchDog server;
	private BufferedDebugPrinter p_printer;

    private BufferedReader p_input;
    private PrintWriter p_output;

    // gnubg external
    private Socket s = null;

    private BufferedReader s_input;
    private PrintWriter s_output;

    protected BufferedDebugPrinter s_printer;
    private boolean busy, connected, evalcmd;

	BGRunner(String[] command, MatchDog server) {

        // proc
		p = null;
		this.command = command;
		this.server = server;
		p_printer = new BufferedDebugPrinter(
			server, "gnubg:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_BLUE
		);

        // sock
        busy = false;
        connected = false;
        evalcmd = false;

        s_printer = new BufferedDebugPrinter(
            server, "gnubg:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_BLUE
        );
	}

    // proc
	public boolean launch() {
        for (String cmd : command) {
            try {
                p_printer.printDebugln("Trying to launch gnubg binary");
                p = Runtime.getRuntime().exec(cmd);
                p_printer.printDebugln("gnubg running (" + cmd + ")");
                break;
            } catch (Exception e) {
                p_printer.printDebugln("gnubg not found at: " + cmd);
            }
        }

        if (p == null) {
            p_printer.printDebugln("Couldn't launch gnubg, exiting...");
            return false;
        }

        p_input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        p_output = new PrintWriter(p.getOutputStream(), true);

        return true;
    }

    public void setup() {

        int checkquerply = server.prefs.getCheckquerply();
        int cubedecply = server.prefs.getCubedecply();
        println("show version");

        println("set eval sameasanalysis off");

        if(server.prefs.getMaxml() == 1) {
            println("set evaluation chequer eval cubeful off");
        }
        println("set threads 8");
        println("set evaluation chequer eval plies " + checkquerply);
        println("set evaluation cubedecision eval plies " + cubedecply);

        for(int i = 0; i < 10; i++) {
            println("set evaluation movefilter " + server.prefs.getMoveFilter(i) );
            server.printDebug(server.prefs.getMoveFilter(i));
        }

        println("external localhost:" + server.prefs.getGnuBgPort());
	}

	public void terminate() {
        closeSocket();
		println("quit");
		p.destroy();
	}

    private void println(String str) {
        p_output.println(str);
        try {
            MatchDog.sleep(8);
        } catch (InterruptedException e) {
            return;
        }
        processInput();
    }

    protected void processInput() {
        try {
            //server.printDebug(" p.avail: " + p_input.ready());
            while((p_input.ready())) {
                p_printer.printDebugln(p_input.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // sock
    public synchronized void execBoard(String board) {
        s_printer.printDebugln("sending BOARD STATE... ");
        execCommand(board);
    }

    public synchronized void execEval(String board) {
        s_printer.printDebugln("sending EVALUATION CMD... ");
        setEvalcmd(true);
        execCommand("evaluation fibsboard " + board + " PLIES 3 CUBE ON CUBEFUL");
        setEvalcmd(false);
    }

    private synchronized void execCommand(String lineIn) {

        long sockettime;

        if(busy) {
            // FIXME
            // This is actually not a bug: can be caused by a 'kibitz move'
            // during gameplay or by the stamptimer.
            // Could be handled better.
            s_printer.printDebugln("*** !! *** NOT SENDING new line to socket - BUSY");
            return;
        }
        busy = true;

        if(lineIn.trim().equals("")) {
            s_printer.printDebugln("*** !! *** NOT SENDING empty line");
            return;
        }

        sockettime = System.nanoTime();
        s_output.printf("%s", lineIn.trim() + "\r\n");

        s_printer.printDebug("(" + ((System.nanoTime() - sockettime) / 1000000.0) + " ms) OK, waiting for reply", "");

        GnubgResponse r = new GnubgResponse(server, s_input, s_printer, isEvalcmd());

        // Process evalcmd synchronously becaue these equities are used
        // to decide resignation later in the same invocation of processGamePlay.
        // Process board state asynchronously so FibsRunner can process new input
        // while waiting for gnubg's response.
        if(isEvalcmd()) {
            r.run();
        } else {
            (new Thread(r)).start();
        }

        busy = false;
    }


    public void connectSocket() {

        while(!connected) {
            try {
                MatchDog.sleep(100);
                InetAddress sa = InetAddress.getByName("localhost");
                s = new Socket(sa, server.prefs.getGnuBgPort());
                if(s.isConnected()) {
                    server.systemPrinter.printDebugln("Successfully connected to bg socket");
                    connected = true;
                    server.gnubg.processInput();
                }
                s_input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                s_output = new PrintWriter(s.getOutputStream(), true);

            } catch(InterruptedException e) {
                return;
            } catch(Exception e) {
                server.systemPrinter.printDebugln(
                        "Exception in BGRunner.connectSocket(): " + e.getMessage() + ", retrying..."
                );
            }
        }
    }

    private void closeSocket() {
        try {
            s.close();
        } catch (IOException e) {
            server.systemPrinter.printDebugln("Exception in BGRunner.closeSocket():" + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isEvalcmd() {
        return evalcmd;
    }

    private void setEvalcmd(boolean evalcmd) {
        this.evalcmd = evalcmd;
    }

    public PrintWriter getScokOut() {
        return s_output;
    }

    public PrintWriter getProcOut() {
        return p_output;
    }
}
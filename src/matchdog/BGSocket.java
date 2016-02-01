package matchdog;

import java.io.*;
import java.net.*;

import jcons.src.com.meyling.console.UnixConsole;

public class BGSocket  {

    private Socket s = null;

    private BufferedReader input;
    private PrintWriter output;

    private MatchDog server;
    protected BufferedDebugPrinter printer;
	private boolean busy, connected, evalcmd;
	
	BGSocket(MatchDog server) {
			
		this.server = server;
		busy = false;
		connected = false;
		evalcmd = false;

		printer = new BufferedDebugPrinter(
			server, "gnubg-external:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_BLUE
		);
	}

    public void execBoard(String board) {
        printer.printDebugln("sending BOARD STATE... ");
        execCommand(board);
    }

    public void execEval(String board) {
        printer.printDebugln("sending EVALUATION CMD... ");
        setEvalcmd(true);
        execCommand("evaluation fibsboard " + board + " PLIES 3 CUBE ON CUBEFUL");
        setEvalcmd(false);
    }

    private void execCommand(String lineIn) {

        long sockettime;

		if(busy) {
			// FIXME
			// This is actually not a bug: can be caused by a 'kibitz move'
			// during gameplay or by the stamptimer.
			// Could be handled better.
			printer.printDebugln("*** !! *** NOT SENDING new line to socket");
			return;
		}
		busy = true;
		
		if(lineIn.trim().equals("")) {
            printer.printDebugln("*** !! *** NOT SENDING empty line");
            return;
        }

		sockettime = System.nanoTime();
		output.printf("%s", lineIn.trim() + "\r\n");

		printer.printDebug("(" + ((System.nanoTime() - sockettime) / 1000000.0) + " ms) OK, waiting for reply", "");

        GnubgResponse r = new GnubgResponse(server, input, printer, isEvalcmd());

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


	public void connect() {
		
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
				input = new BufferedReader(new InputStreamReader(s.getInputStream()));
				output = new PrintWriter(s.getOutputStream(), true);
				
			} catch(InterruptedException e) {
				return;
			} catch(Exception e) {
				server.systemPrinter.printDebugln(
                        "Exception in BGSocket.connect(): " + e.getMessage() + ", retrying..."
                );
			}
		}
	}
	
	public void terminate() {
        try {
            s.close();
        } catch (IOException e) {
            server.systemPrinter.printDebugln("Exception in BGSocket.terminate():" + e.getMessage());
            e.printStackTrace();
        }
    }

	private boolean isEvalcmd() {
		return evalcmd;
	}

	private void setEvalcmd(boolean evalcmd) {
		this.evalcmd = evalcmd;
	}

    public PrintWriter getOutput() {
        return output;
    }
}

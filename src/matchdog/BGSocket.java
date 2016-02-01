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

        long sockettime, replytime;
        double replydiff;
        String unit, rawReply, reply;

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
		replytime = System.nanoTime();

        try {
            rawReply = input.readLine();
        } catch (IOException e) {
            server.systemPrinter.printDebugln("Exception reading from gnubg external: " + e.getMessage());
            return;
        }

        if((replydiff = (System.nanoTime() - replytime) / 1000000000.0) < 0.001) {
            replydiff *= 1000;
            unit = " ms";
        } else {
            unit = " seconds";
        }

        if(rawReply.startsWith("Error:")) {
            server.printDebug(" --**--> GNUBG ERROR: ");
            server.printDebug(rawReply);
            busy = false;
            return;
        }

        if(isEvalcmd()) {
            parseEquities(rawReply);
            printer.printDebugln("gnubg EQUITIES (in "
                    + replydiff + unit
                    + "): " + rawReply);

        } else {
            reply = processReply(rawReply);
            printer.printDebugln("gnubg says (in "
                    + replydiff + unit
                    + "): " + rawReply);

            server.fibs.sleepFibs(100);
            server.fibsout.println(reply);
            printer.printDebugln("sent to fibs: ");
            server.fibs.printFibsCommand(reply);
            printer.printDebugln("");
            server.fibs.printMatchInfo();
        }

        busy = false;
	}
	
	private String processReply(String line) {

		if(isEvalcmd()) {
			return line;
		}

        return transformCommand(line);
	}

    private String transformCommand(String in) {

        String out = in.replace("25", "bar").
                replace("/0", "/off").
                replace("/", "-").
                replace("*", "").
                replace("take", "accept").
                replace("drop", "reject");

        if(out.contains("-")) {

            if(server.fibs.match.isShiftmove()) {
                out = shift(out);
            }
            out = "move " + out;
        }
       return out;
    }

	private void parseEquities(String in) {
		
		// FIXME
		//// this is bug... 
		// sometimes a 'not-to-roll' case is not detected
		// and a board: which results in a 'roll' or other
		// non expected reply is sent from gnubg.
		if(in.startsWith("roll") || in.startsWith("double") || in.contains("/") || in.equals("")) {
			printer.printDebugln("*** !! BUG parseEquities: " + in);
			return;
		}

		String [] split0 = in.split(" ");
		for(int i = 0; i < 6; i++) {
			if(!split0[i].equals("")) {
				server.fibs.match.equities[i] = Double.parseDouble(split0[i]);
			}
		}
	}

	private String shift(String in) {
		server.printDebug("SHIFTING");
		String [] arr0 = in.split(" ");
		String returnStr = "";
		for(int i = 0; i < arr0.length; i++) {
			String [] arr1 = arr0[i].split("-");
			
			try {
				int n = Integer.parseInt(arr1[0]);
				
				if(n < 25 && n > 0) {
					n = 25 - n;
					arr1[0] = Integer.toString(n);
				}
				
			} catch (Exception e) {
				//server.printDebug("not shifting " + arr1[0]);
			}
			try {
				int m = Integer.parseInt(arr1[1]);
				
				if(m < 25 && m > 0) {
					m = 25 - m;
					arr1[1] = Integer.toString(m);
				}
				
			} catch (Exception e) {
				//server.printDebug("not shifting " + arr1[1]);
			}
			arr0[i] = arr1[0] + "-" + arr1[1];
			returnStr += arr0[i] + " ";
		}
		//server.printDebug("shift result: " + returnStr);
		return returnStr;
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

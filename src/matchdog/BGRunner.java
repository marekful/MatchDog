package matchdog;
import jcons.src.com.meyling.console.UnixConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class BGRunner  {
	
	private Process p;
	private String[] command;
	private MatchDog server;
	private BufferedDebugPrinter printer;

    private BufferedReader input;
    private PrintWriter output;

	BGRunner(String[] command, MatchDog server) {
		p = null;
		this.command = command;
		this.server = server;
		printer = new BufferedDebugPrinter(
			server, "gnubg:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_BLUE
		);
	}

	public boolean connect() {
        for (String cmd : command) {
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

        input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        output = new PrintWriter(p.getOutputStream(), true);

        return true;
    }

    public void run() {

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
		println("quit");
		p.destroy();
	}

    private void println(String str) {
        output.println(str);
        try {
            MatchDog.sleep(8);
        } catch (InterruptedException e) {
            return;
        }
        processInput();
    }

    protected void processInput() {
        try {
            //server.printDebug(" p.avail: " + input.ready());
            while((input.ready())) {
                printer.printDebugln(input.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package matchdog;
import java.io.*;

import jcons.src.com.meyling.console.ConsoleBackgroundColor;
import jcons.src.com.meyling.console.ConsoleFactory;
import jcons.src.com.meyling.console.ConsoleForegroundColor;
import jcons.src.com.meyling.console.UnixConsole;
import jcons.src.com.meyling.console.Console;

public class BGRunner extends Thread {
	
	Process p;
	boolean init, isExternal, run, dead;
	String command;
	MatchDog server;
	BufferedDebugPrinter printer;
	String player0, player1;
	String lastMoveStr;
	int port;
	
	
	BGRunner(String command, MatchDog server, int port) {
		super("BGThred");
		p = null;
		init = false;
		isExternal = false;
		this.command = command;
		this.server = server;
		//player0 = server.gnubgp0;
		//player1 = server.gnubgp1;
		lastMoveStr = "";
		setName("BGRunner");
		dead = false;
		this.port = port;
		printer = new BufferedDebugPrinter(
			server, "gnubg:", UnixConsole.LIGHT_WHITE, UnixConsole.BACKGROUND_BLUE
		);
		printer.setBuff(server.outputBuffer);
	}

	@Override
	public void run() {
		run = true;
		try {
			
			String line;
			p = Runtime.getRuntime().exec(command);
			BufferedReader input = new BufferedReader(new InputStreamReader(p
					.getInputStream()));
			
			init = true;
			synchronized(server) {
				server.notify();
			}
			
			while ((line = input.readLine()) != null && run) {
				
				if(line.startsWith("Waiting for a connection")) {
					isExternal = true;
					//synchronized(server.bgsocket) {
					//	server.bgsocket.notify();
					//}
				}
				
				printer.printDebugln(line);
				//outbuffer.put(outbuffer.size(), line);
				
				//init = true;
				
			}
			input.close();
		} catch (Exception err) {
			server.printDebug("BGRunner(run): " + err);
			//err.printStackTrace();
		}

		server.printDebug("Exiting BGRunner thread");
		dead = true;
	}

	public void terminate() {
		server.gnubgout.println("quit");
		this.run = false;
		p.destroy();
	}

	public void setPort(int port) {
		this.port = port;
	}
}
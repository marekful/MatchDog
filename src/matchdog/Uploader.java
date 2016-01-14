package matchdog;

public class Uploader extends Thread {
	MatchDog server;
	Boolean run, dead, transferOK;
	String filename;
	Process p;
	Uploader(MatchDog server, int mode, String filename) {
		this.server = server;
		this.filename = filename;
		transferOK = false;
		server.printDebug("STARTING UPLOADER: " + filename);
		
	}

	@Override
	public void run() {
		run = true;
		dead = false;
		try {
			String in;
			String command = server.getFtpScriptPath() + " " + filename;
			p = Runtime.getRuntime().exec(command);
			server.printDebug(" > runtime command: " + command);
			
			p.waitFor();
			
			server.printDebug(" > exit value: " + p.exitValue());
			
			// FIXME
			// The supplied shell script will always
			// exit with value 0. That script should 
			// exit with 1 if anything goes wrong during
			// file upload to ftp server.
			if(p.exitValue() == 0) {
				server.printDebug(" > FTP UPLOAD finished");
				server.fibs.statUploaderFinished(true, filename);
			}  else {
				server.printDebug(" > FTP UPLOAD FAILED !");
				server.fibs.statUploaderFinished(false, filename);
			}
			server.printDebug("Exiting Uploader thread");
			dead = true;
		} catch (Exception err) {
			System.out.println("Uploader(run): " + err);
			err.printStackTrace();
		}
	}
}
package matchdog;

public class MatchEx extends Match {


    Runner r;


    MatchEx(MatchDog server, String oppname, int matchlength) {
        super(server, oppname, matchlength);

        r = new Runner();
        r.start();

    }

    public void sendCommand(String command) {
        r.sendCommand(command);
    }

    public void onMatchEnd() {
        super.onMatchEnd();
        r.bg.killGnubg(false);
        r.bg = null;
        r = null;
    }

    public void hint() {
        r.bg.printer.printLine("hint requested");
        r.bg.println("hint", false);

        r.bg.processInput();

        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignore) { }

        synchronized (FibsRunner.lockEx) {
            releaseLockEx = true;
            FibsRunner.lockEx.notify();
        }
    }


    private class Runner extends Thread {

        private BGRunner bg;

        @Override
        public void run() {
            String[] fixedArgs = {"-t", "-q", "-s", server.configDir + "gnubg", "-D", server.dataDir + "gnubg"};
            bg = new AnalysisRunner(server, server.programPrefs.getGnubgCmdArr(), fixedArgs);
            bg.printer.setLabel("gnubg-other");
            bg.startGnubg("", false);
        }

        synchronized void sendCommand(String command) {
            bg.println(command, false);

        }
    }
}

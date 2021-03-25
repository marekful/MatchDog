package matchdog;

import jcons.src.com.meyling.console.UnixConsole;

import java.io.File;
import java.io.IOException;

public class GnubgHint extends Gnubg {

    GnubgHint(MatchDog server, String[] command, String[] fixedArgs) {
        super(server, command, fixedArgs, false);

        printer.setColor(UnixConsole.LIGHT_YELLOW);
        printer.setBgColor(UnixConsole.BACKGROUND_MAGENTA);
    }

    protected Process createWithCommand(String command) throws IOException {
        File outFile = new File(server.getDataDir() + server.getPlayerName() + ".out.tmp");
        outFile.createNewFile();
        return new ProcessBuilder(command.split(" "))
                .redirectOutput(outFile)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
    }
}

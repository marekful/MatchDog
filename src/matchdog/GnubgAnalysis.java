package matchdog;

import java.io.File;
import java.io.IOException;

public class GnubgAnalysis extends Gnubg {

    private static final File DEV_NULL = new File(
            (System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null")
    );

    GnubgAnalysis(MatchDog server, String[] command, String[] fixedArgs, boolean verbose) {
        super(server, command, fixedArgs, verbose);
    }

    protected Process createWithCommand(String command) throws IOException {
        return new ProcessBuilder(command.split(" "))
                .redirectOutput(DEV_NULL)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
    }
}

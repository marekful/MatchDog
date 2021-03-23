package matchdog;

import jcons.src.com.meyling.console.UnixConsole;
import matchdog.console.printer.DefaultPrinter;

import java.io.IOException;
import java.util.ArrayList;

public class AnalysisRunner extends BGRunner {

    AnalysisRunner(MatchDog server, String[] command, String[] fixedArgs) {
        super(server, command, fixedArgs);

        printer.setColor(UnixConsole.RED);
        printer.setBgColor(UnixConsole.BACKGROUND_YELLOW);
    }

    public void processInput() {
        ArrayList<String> hints = new ArrayList<>();
        try {
            while((pIn.ready())) {
                 String line = pIn.readLine();
                 printer.printLine("read: " + line);
                 hints.add(pIn.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        printer.printLine("Hints size: " + hints.size());
        if (hints.size() > 0) {
            for (int i = 0; i <= 2; i++) {
                if (hints.size() - 1 < i) {
                    break;
                }
                String hint = hints.get(i);
                printer.printLine(hint);
            }
        }
    }

}

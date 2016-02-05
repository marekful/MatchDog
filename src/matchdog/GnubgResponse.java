package matchdog;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.Buffer;

/**
 * Created by marekful on 01/02/2016.
 */
public class GnubgResponse implements Runnable {

    MatchDog server;
    BufferedReader input;
    BufferedDebugPrinter printer;
    boolean isEvalcmd;

    GnubgResponse(MatchDog server,
                  BufferedReader input,
                  BufferedDebugPrinter printer,
                  boolean isEvalcmd)
    {
        this.server = server;
        this.input = input;
        this.printer = printer;
        this.isEvalcmd = isEvalcmd;
    }

    @Override
    public void run() {

        String rawReply, reply, unit;
        long replytime;
        double replydiff;

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
            return;
        }

        if(isEvalcmd) {
            parseEquities(rawReply);
            printer.printDebugln("gnubg EQUITIES (in "
                    + replydiff + unit
                    + "): " + rawReply);

        } else {
            reply = processReply(rawReply);

            if(server.fibs.match == null || server.fibs.match.isFinished()) {
                printer.printDebugln("*!* NOT SENDING to FIBS -> match finished");
                return;
            }

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

    }

    private String processReply(String line) {

        if(isEvalcmd) {
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

        if(server.fibs.match == null) {
            printer.printDebugln("*!* NOT PARSING EQUITIES -> match == null");
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
}

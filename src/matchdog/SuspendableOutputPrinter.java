package matchdog;

import matchdog.console.printer.BufferedConsolePrinter;

import java.io.PrintStream;
import java.util.ArrayList;

public interface SuspendableOutputPrinter {

    void addPrinter(BufferedConsolePrinter printer);

    void removePrinter(BufferedConsolePrinter printer);

    void suspendOutput(PrintStream output);

    void unSuspendOutput(PrintStream output);


    ArrayList<BufferedConsolePrinter> getPrinters();
}

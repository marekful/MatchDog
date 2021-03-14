package matchdog.console.printer;

import matchdog.MatchDog;
import matchdog.PrintableStreamSource;

public class FibsPrinter
		extends  BufferedConsolePrinter {

	int unused = 0;

	public FibsPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
		super(source, label, color, bgColor);
	}
	
	public String getLabel() {
		return "fibs[mode=" + ((MatchDog)super.getSource()).getFibsMode() + "]:";
	}
}

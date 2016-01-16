package matchdog;

public class FibsDebugPrinter extends DebugPrinter {

	FibsDebugPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
		super(source, label, color, bgColor);
	}
	
	public String getLabel() {
		return "fibs[" + ((MatchDog)source).getFibsmode() + "]:";
	}
}

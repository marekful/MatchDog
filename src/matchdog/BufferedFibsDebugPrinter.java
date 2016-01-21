package matchdog;

public class BufferedFibsDebugPrinter extends BufferedDebugPrinter {
	
	BufferedFibsDebugPrinter(PrintableStreamSource source, String label, String color, String bgColor) {
		super(source, label, color, bgColor);
	}
	
	public String getLabel() {
		return "fibs[" + ((MatchDog)source).getFibsmode() + "]:";
	}
}

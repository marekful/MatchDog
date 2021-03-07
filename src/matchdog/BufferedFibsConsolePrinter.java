package matchdog;

public class BufferedFibsConsolePrinter extends BufferedConsolePrinter {
	
	BufferedFibsConsolePrinter(PrintableStreamSource source, String label, String color, String bgColor) {
		super(source, label, color, bgColor);
	}
	
	public String getLabel() {
		return "fibs[" + ((MatchDog)super.getSource()).getFibsmode() + "]:";
	}
}

package matchdog;

import java.io.PrintStream;
import java.util.Collection;

public interface PrintableStreamSource {
	public Collection<PrintStream> getPrintStreams();
}

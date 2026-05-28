package dal.tool.trace.jmxer.tui;

import java.io.OutputStream;
import java.io.PrintStream;

import dal.tool.cli.Logger;

/**
 * While TUI owns the terminal, redirects {@link System#out} and {@link System#err} to {@link Logger}
 * so stray {@code printStackTrace()} or {@code System.out.println()} calls cannot corrupt the screen.
 */
final class TuiStdioRedirect {
	private final PrintStream prevOut;
	private final PrintStream prevErr;
	private final PrintStream wrappingOut;
	private final PrintStream wrappingErr;

	TuiStdioRedirect() {
		prevOut = System.out;
		prevErr = System.err;
		wrappingOut = createLoggerPrintStream(prevOut);
		wrappingErr = createLoggerPrintStream(prevErr);
	}

	void install() {
		System.setOut(wrappingOut);
		System.setErr(wrappingErr);
	}

	void restore() {
		System.setOut(prevOut);
		System.setErr(prevErr);
	}

	private static PrintStream createLoggerPrintStream(final PrintStream fallback) {
		return new PrintStream(new OutputStream() {
			private final StringBuilder line = new StringBuilder();

			@Override
			public synchronized void write(int b) {
				if(b == '\n') {
					emitLine();
				} else if(b != '\r') {
					line.append((char)(b & 0xff));
				}
			}

			@Override
			public synchronized void flush() {
				if(line.length() > 0) {
					emitLine();
				}
			}

			private void emitLine() {
				if(line.length() == 0) {
					return;
				}
				String s = line.toString();
				line.setLength(0);
				if(Logger.isTuiActive()) {
					Logger.logln(s);
				} else {
					fallback.println(s);
				}
			}
		}, true);
	}
}

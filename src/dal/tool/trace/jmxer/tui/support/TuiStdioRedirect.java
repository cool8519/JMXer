package dal.tool.trace.jmxer.tui.support;

import java.io.OutputStream;
import java.io.PrintStream;

import dal.tool.cli.Logger;

/**
 * TUI가 터미널을 점유하는 동안 System.out·System.err를 Logger로 넘겨 화면이 깨지지 않게 한다.
 *
 */
public final class TuiStdioRedirect {
	private final PrintStream prevOut;
	private final PrintStream prevErr;
	private final PrintStream wrappingOut;
	private final PrintStream wrappingErr;

	public TuiStdioRedirect() {
		prevOut = System.out;
		prevErr = System.err;
		wrappingOut = createLoggerPrintStream(prevOut);
		wrappingErr = createLoggerPrintStream(prevErr);
	}

	public void install() {
		System.setOut(wrappingOut);
		System.setErr(wrappingErr);
	}

	public void restore() {
		System.setOut(prevOut);
		System.setErr(prevErr);
	}

	private static PrintStream createLoggerPrintStream(final PrintStream fallback) {
		return new PrintStream(new OutputStream() {
			private final StringBuilder line = new StringBuilder();

			@Override
			public synchronized void write(int b) {
				if(b == '\n') {
					if(Logger.isTuiActive()) {
						line.setLength(0);
					} else {
						emitLine();
					}
				} else if(b != '\r') {
					char c = (char)(b & 0xff);
					line.append(c);
					if(Logger.isTuiActive()) {
						Logger.appendTuiInlineFragment(String.valueOf(c));
					}
				}
			}

			@Override
			public synchronized void flush() {
				if(!Logger.isTuiActive() && line.length() > 0) {
					emitLine();
				}
			}

			private void emitLine() {
				if(line.length() == 0) {
					return;
				}
				String s = line.toString();
				line.setLength(0);
				fallback.println(s);
			}
		}, true);
	}
}

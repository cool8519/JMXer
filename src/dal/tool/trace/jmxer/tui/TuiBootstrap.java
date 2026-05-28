package dal.tool.trace.jmxer.tui;

import java.io.IOException;
import java.util.Arrays;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.TerminalResizeListener;

import dal.tool.cli.ExternalUiHooks;
import dal.tool.cli.Logger;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxMutatingCallConfirmation;
import dal.tool.util.jmx.MBeanConnector;

public class TuiBootstrap {
	private TuiBootstrap() {}

	public static void launch(MBeanConnector connector, String prompt, String initialCommandLine) throws Exception {
		Screen screen = null;
		Terminal terminal = null;
		TuiStdioRedirect stdioRedirect = null;
		try {
			// Windows default console can start with a very small buffer/window.
			// Lanterna respects the initial terminal size as a "preferred" size.
			DefaultTerminalFactory factory = new DefaultTerminalFactory();
			// On Unix-like terminals, Ctrl+C might otherwise kill the JVM (SIGINT) before we can show a confirmation dialog.
			// Try to configure Lanterna to trap Ctrl+C and deliver it as a normal keystroke instead.
			try {
				try {
					Class<?> ctrlEnum = Class.forName("com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal$CtrlCBehaviour");
					@SuppressWarnings({ "rawtypes", "unchecked" })
					Object trap = Enum.valueOf((Class<? extends Enum>)ctrlEnum, "TRAP");
					java.lang.reflect.Method m = factory.getClass().getMethod("setUnixTerminalCtrlCBehaviour", ctrlEnum);
					m.invoke(factory, trap);
				} catch(Throwable t1) {
					Class<?> ctrlEnum = Class.forName("com.googlecode.lanterna.terminal.ansi.UnixLikeTTYTerminal$CtrlCBehaviour");
					@SuppressWarnings({ "rawtypes", "unchecked" })
					Object trap = Enum.valueOf((Class<? extends Enum>)ctrlEnum, "TRAP");
					java.lang.reflect.Method m = factory.getClass().getMethod("setUnixTerminalCtrlCBehaviour", ctrlEnum);
					m.invoke(factory, trap);
				}
			} catch(Throwable ignore) {}

			terminal = factory
				.setInitialTerminalSize(new TerminalSize(180, 50))
				.createTerminal();
			screen = new TerminalScreen(terminal);
			screen.startScreen();
			screen.setCursorPosition(null);

			final Screen finalScreen = screen;

			MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
			JmxCommandExecutor executor = new JmxCommandExecutor(connector);
			executor.setPrompt(prompt);

			final Logger.OutputSink prevSink = Logger.getOutputSink();

			final JmxTuiWindow window = new JmxTuiWindow();
			window.setTitle("");
			// Avoid outer window decorations (border) artifacts; keep inner boxed layout only.
			window.setHints(Arrays.asList(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));

			final Runnable closeAction = new Runnable() {
				@Override
				public void run() {
					try {
						window.close();
					} catch(Exception e) {}
				}
			};

			JmxTuiApp app = new JmxTuiApp(gui, executor, prompt, closeAction);
			window.setGlobalShortcutHandler(new JmxTuiWindow.GlobalShortcutHandler() {
				@Override
				public boolean consumeGlobalShortcut(com.googlecode.lanterna.input.KeyStroke keyStroke) {
					return app.consumeGlobalShortcut(keyStroke);
				}
			});
			Logger.setTuiActive(true);
			stdioRedirect = new TuiStdioRedirect();
			stdioRedirect.install();

			Logger.setOutputSink(app.getOutputSink());
			Logger.setClearScreenHandler(app.getClearHandler());
			Logger.setTuiAbortUserCaptureRunnable(app.getAbortUserCaptureRunnable());
			Logger.setTuiCommitCapturePreambleRunnable(app.getCommitUserCapturePreambleRunnable());

			final MultiWindowTextGUI guiForConfirm = gui;
			JmxMutatingCallConfirmation.setHandler(new JmxMutatingCallConfirmation.Handler() {
				@Override
				public boolean confirm(String title, String message) {
					MessageDialogButton b = TuiDialogs.showMessageDialog(
						guiForConfirm,
						title,
						message,
						MessageDialogButton.Yes,
						MessageDialogButton.No
					);
					return b != null && b == MessageDialogButton.Yes;
				}
			});

			ExternalUiHooks.beforeBlockingExternalProcess = new Runnable() {
				@Override
				public void run() {
					try {
						finalScreen.stopScreen();
					} catch(IOException e) {}
				}
			};
			ExternalUiHooks.afterBlockingExternalProcess = new Runnable() {
				@Override
				public void run() {
					try {
						finalScreen.startScreen();
						finalScreen.setCursorPosition(null);
					} catch(IOException e) {}
				}
			};

			if(initialCommandLine != null && initialCommandLine.trim().length() > 0) {
				executor.processCommandInNonPrompt(initialCommandLine.trim());
			}

			window.setComponent(app.getRoot());

			// Ensure Ctrl+C works reliably on Windows where it can be delivered as SIGINT instead of a key stroke.
			installSigIntHandler(gui, app);

			// Initial sizing (prefer output to expand, header fixed at 4 lines inside app)
			try {
				app.applyTerminalSize(screen.getTerminalSize());
			} catch(Exception e) {}

			// Resize handling: recompute preferred sizes and relayout
			if(terminal != null) {
				terminal.addResizeListener(new TerminalResizeListener() {
					@Override
					public void onResized(Terminal t, com.googlecode.lanterna.TerminalSize newSize) {
						gui.getGUIThread().invokeLater(new Runnable() {
							@Override
							public void run() {
								try {
									finalScreen.doResizeIfNecessary();
								} catch(Exception e) {}
								try {
									app.applyTerminalSize(newSize);
									window.invalidate();
								} catch(Exception e) {}
							}
						});
					}
				});
			}

			gui.addWindowAndWait(window);

			Logger.setTuiPanelCapture(false);
			Logger.setTuiActive(false);
			JmxMutatingCallConfirmation.clearHandler();
			if(stdioRedirect != null) {
				stdioRedirect.restore();
			}
			Logger.setOutputSink(prevSink);
			Logger.setClearScreenHandler(null);
			Logger.setTuiAbortUserCaptureRunnable(null);
			Logger.setTuiCommitCapturePreambleRunnable(null);
			ExternalUiHooks.beforeBlockingExternalProcess = null;
			ExternalUiHooks.afterBlockingExternalProcess = null;
		} finally {
			JmxMutatingCallConfirmation.clearHandler();
			Logger.setTuiAbortUserCaptureRunnable(null);
			Logger.setTuiCommitCapturePreambleRunnable(null);
			Logger.setTuiActive(false);
			if(stdioRedirect != null) {
				stdioRedirect.restore();
			}
			if(screen != null) {
				try { screen.stopScreen(); } catch(Exception e) {}
				try { screen.close(); } catch(Exception e) {}
			}
		}
	}

	/**
	 * Best-effort SIGINT handler (Ctrl+C). Uses reflection to avoid compile/runtime issues on
	 * environments where sun.misc.Signal is unavailable.
	 */
	private static void installSigIntHandler(final MultiWindowTextGUI gui, final JmxTuiApp app) {
		try {
			final Class<?> signalClass = Class.forName("sun.misc.Signal");
			final Class<?> handlerClass = Class.forName("sun.misc.SignalHandler");
			final java.lang.reflect.Method handleMethod = signalClass.getMethod("handle", signalClass, handlerClass);

			Object handler = java.lang.reflect.Proxy.newProxyInstance(
				TuiBootstrap.class.getClassLoader(),
				new Class<?>[] { handlerClass },
				new java.lang.reflect.InvocationHandler() {
					@Override
					public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
						// method name is usually "handle(Signal)".
						try {
							gui.getGUIThread().invokeLater(new Runnable() {
								@Override
								public void run() {
									app.requestExitWithConfirm();
								}
							});
						} catch(Exception e) {}
						return null;
					}
				}
			);

			handleMethod.invoke(null, signalClass.getConstructor(String.class).newInstance("INT"), handler);
		} catch(Throwable t) {
			// Ignore: Ctrl+C might still be handled as a key stroke by Lanterna.
		}
	}
}


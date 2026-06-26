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
import dal.tool.cli.ScriptUiHooks;
import dal.tool.cli.SleepUiHooks;
import dal.tool.trace.jmxer.JMXControl;
import dal.tool.trace.jmxer.cli.JmxCommandExecutor;
import dal.tool.trace.jmxer.cli.JmxMutatingCallConfirmation;
import dal.tool.trace.jmxer.cli.JmxTuiConnectionLossHandler;
import dal.tool.trace.jmxer.tui.app.JmxTuiApp;
import dal.tool.trace.jmxer.tui.app.JmxTuiWindow;
import dal.tool.trace.jmxer.tui.output.JmxTuiOutputSink;
import dal.tool.trace.jmxer.tui.dialog.TuiDialogs;
import dal.tool.trace.jmxer.tui.support.TuiCommandStdin;
import dal.tool.trace.jmxer.tui.support.TuiGuiThread;
import dal.tool.trace.jmxer.tui.support.TuiSleepProgress;
import dal.tool.trace.jmxer.tui.support.TuiScriptProgress;
import dal.tool.trace.jmxer.tui.support.TuiStdioRedirect;
import dal.tool.trace.jmxer.tui.support.TuiGuiThread.GuiCallable;
import dal.tool.util.jmx.MBeanConnector;

/**
 * TUI 세션 부트스트랩. Lanterna 터미널·GUI·JmxTuiApp을 초기화하고 종료 시 리소스를 정리한다.
 *
 */
public class TuiBootstrap {
	private TuiBootstrap() {}

	public static void launch(MBeanConnector connector, String prompt, String initialCommandLine) throws Exception {
		Screen screen = null;
		Terminal terminal = null;
		TuiStdioRedirect stdioRedirect = null;
		java.io.InputStream prevIn = null;
		TuiCommandStdin commandStdin = null;
		JmxTuiApp app = null;
		try {
			// Windows 콘솔은 초기 버퍼가 작을 수 있어 Lanterna 선호 크기를 명시한다.
			DefaultTerminalFactory factory = new DefaultTerminalFactory();
			// Unix 계열에서 Ctrl+C가 확인 대화상자 전에 SIGINT로 JVM을 종료하지 않도록 트랩한다.
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
			window.setHints(Arrays.asList(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));

			final Runnable closeAction = new Runnable() {
				@Override
				public void run() {
					try {
						window.close();
					} catch(Exception e) {}
				}
			};

			app = new JmxTuiApp(gui, executor, prompt, closeAction);
			final JmxTuiApp tuiApp = app;
			commandStdin = app.getCommandStdin();
			prevIn = System.in;
			System.setIn(commandStdin);
			window.setGlobalShortcutHandler(new JmxTuiWindow.GlobalShortcutHandler() {
				@Override
				public boolean consumeGlobalShortcut(com.googlecode.lanterna.input.KeyStroke keyStroke) {
					return tuiApp.consumeGlobalShortcut(keyStroke);
				}
			});
			Logger.setTuiActive(true);
			stdioRedirect = new TuiStdioRedirect();
			final TuiStdioRedirect stdioRedirectForExternal = stdioRedirect;
			stdioRedirect.install();

			Logger.setOutputSink(app.getOutputSink());
			Logger.setClearScreenHandler(app.getClearHandler());
			Logger.setTuiAbortUserCaptureRunnable(app.getAbortUserCaptureRunnable());
			Logger.setTuiCommitCapturePreambleRunnable(app.getCommitUserCapturePreambleRunnable());

			final MultiWindowTextGUI guiForConfirm = gui;
			JmxMutatingCallConfirmation.setHandler(new JmxMutatingCallConfirmation.Handler() {
				@Override
				public boolean confirm(final String title, final String message) {
					ScriptUiHooks.suspendForUserInput();
					try {
						MessageDialogButton b = TuiGuiThread.callOnGuiThread(guiForConfirm,
							new TuiGuiThread.GuiCallable<MessageDialogButton>() {
								@Override
								public MessageDialogButton call() {
									return TuiDialogs.showMessageDialog(
										guiForConfirm,
										title,
										message,
										MessageDialogButton.Yes,
										MessageDialogButton.No
									);
								}
							});
						return b != null && b == MessageDialogButton.Yes;
					} finally {
						ScriptUiHooks.resumeAfterUserInput();
					}
				}
			});

			final MultiWindowTextGUI guiForSleep = gui;
			SleepUiHooks.setHandler(new SleepUiHooks.Handler() {
				@Override
				public boolean waitForSleep(long sleepMs) {
					return TuiSleepProgress.waitForSleep(guiForSleep, sleepMs);
				}
			});

			final JmxTuiApp tuiAppForScript = app;
			ScriptUiHooks.setHandler(new ScriptUiHooks.Handler() {
				@Override
				public boolean confirmBeforeScript(String scriptFilesDisplay) {
					return TuiScriptProgress.confirmBeforeScript(guiForSleep, scriptFilesDisplay);
				}

				@Override
				public void beginScript(String rootCommandLine) {
					TuiScriptProgress.begin(guiForSleep, rootCommandLine);
				}

				@Override
				public void setCurrentCommand(String scriptFileName, String scriptCommandLine) {
					TuiScriptProgress.setCurrentCommand(guiForSleep, scriptFileName, scriptCommandLine);
				}

				@Override
				public void flushOutputCapture() {
					tuiAppForScript.refreshLiveScriptCaptureDisplay();
				}

				@Override
				public void suspendForUserInput() {
					TuiScriptProgress.suspend(guiForSleep);
				}

				@Override
				public void resumeAfterUserInput() {
					TuiScriptProgress.resume(guiForSleep);
				}

				@Override
				public void finishScript(boolean completedSuccessfully, boolean cancelled) {
					TuiScriptProgress.finish(guiForSleep, completedSuccessfully, cancelled);
				}
			});

			JmxTuiConnectionLossHandler.setHandler(new JmxTuiConnectionLossHandler.Handler() {
				@Override
				public void onConnectionLoss(String message) {
					tuiApp.handleConnectionLoss(message);
				}
			});

			final Logger.OutputSink[] savedOutputSinkForExternal = new Logger.OutputSink[1];
			final boolean[] savedPanelCaptureForExternal = new boolean[1];
			ExternalUiHooks.beforeBlockingExternalProcess = new Runnable() {
				@Override
				public void run() {
					TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
						@Override
						public Void call() {
							try {
								finalScreen.stopScreen();
							} catch(IOException e) {}
							return null;
						}
					});
					stdioRedirectForExternal.restore();
					savedOutputSinkForExternal[0] = Logger.getOutputSink();
					savedPanelCaptureForExternal[0] = Logger.isTuiPanelCapture();
					Logger.setTuiPanelCapture(false);
					Logger.setOutputSink(null);
				}
			};
			ExternalUiHooks.afterBlockingExternalProcess = new Runnable() {
				@Override
				public void run() {
					Logger.setOutputSink(savedOutputSinkForExternal[0]);
					Logger.setTuiPanelCapture(savedPanelCaptureForExternal[0]);
					stdioRedirectForExternal.install();
					TuiGuiThread.callOnGuiThread(gui, new GuiCallable<Void>() {
						@Override
						public Void call() {
							try {
								finalScreen.startScreen();
								finalScreen.setCursorPosition(null);
								finalScreen.doResizeIfNecessary();
							} catch(IOException e) {}
							try {
								tuiApp.applyTerminalSize(finalScreen.getTerminalSize());
							} catch(Exception e) {}
							window.invalidate();
							return null;
						}
					});
				}
			};

			if(initialCommandLine != null && initialCommandLine.trim().length() > 0) {
				executor.processCommandInNonPrompt(initialCommandLine.trim());
			}
			if(JMXControl.isOfflineSession) {
				app.finishOfflineSessionBootstrap();
			}

			window.setComponent(app.getRoot());

			// Windows에서 Ctrl+C가 키 입력 대신 SIGINT로 올 수 있어 별도 핸들러를 둔다.
			installSigIntHandler(gui, tuiApp);
			// Unix 계열에서 Ctrl+Z가 JVM을 일시 정지시키지 않도록 한다.
			installIgnoredSignal("TSTP");

			try {
				app.applyTerminalSize(screen.getTerminalSize());
			} catch(Exception e) {}

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
									tuiApp.applyTerminalSize(newSize);
									window.invalidate();
								} catch(Exception e) {}
							}
						});
					}
				});
			}

			gui.addWindowAndWait(window);

			if(app != null) {
				app.shutdownCommandWorker();
			}
			if(commandStdin != null) {
				commandStdin.closeBridge();
			}
			if(prevIn != null) {
				System.setIn(prevIn);
			}
			if(app != null) {
				((JmxTuiOutputSink) app.getOutputSink()).shutdown();
			}
			Logger.setTuiPanelCapture(false);
			Logger.setTuiActive(false);
			JmxMutatingCallConfirmation.clearHandler();
			SleepUiHooks.clearHandler();
			ScriptUiHooks.clearHandler();
			JmxTuiConnectionLossHandler.clearHandler();
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
			try {
				if(app != null) {
					app.shutdownCommandWorker();
				}
			} catch(Exception e) {}
			if(commandStdin != null) {
				try {
					commandStdin.closeBridge();
				} catch(Exception e) {}
			}
			if(prevIn != null) {
				try {
					System.setIn(prevIn);
				} catch(Exception e) {}
			}
			try {
				if(Logger.getOutputSink() instanceof JmxTuiOutputSink) {
					((JmxTuiOutputSink) Logger.getOutputSink()).shutdown();
				}
			} catch(Exception e) {}
			JmxMutatingCallConfirmation.clearHandler();
			SleepUiHooks.clearHandler();
			ScriptUiHooks.clearHandler();
			JmxTuiConnectionLossHandler.clearHandler();
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
	 * 지정 신호를 무시한다(예: Ctrl+Z의 SIGTSTP). sun.misc.Signal이 있을 때만 리플렉션으로 등록한다.
	 */
	private static void installIgnoredSignal(String signalName) {
		if(signalName == null || signalName.length() == 0) {
			return;
		}
		try {
			final Class<?> signalClass = Class.forName("sun.misc.Signal");
			final Class<?> handlerClass = Class.forName("sun.misc.SignalHandler");
			final java.lang.reflect.Method handleMethod = signalClass.getMethod("handle", signalClass, handlerClass);
			Object sigIgn = handlerClass.getField("SIG_IGN").get(null);
			handleMethod.invoke(null, signalClass.getConstructor(String.class).newInstance(signalName), sigIgn);
		} catch(Throwable t) {}
	}

	/**
	 * SIGINT(Ctrl+C) 핸들러를 최대한 등록한다. sun.misc.Signal이 없는 환경에서는 조용히 건너뛴다.
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
		} catch(Throwable t) {}
	}
}


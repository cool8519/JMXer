package dal.tool.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import dal.tool.util.StringUtil;


/**
 * 화면/파일 로깅을 담당하는 클래스
 * @author 권영달
 *
 */
public class Logger extends Object {

	/**
	 * 화면 출력 대상. TUI 모드에서 교체하여 결과를 터미널 GUI로 라우팅한다.
	 */
	public interface OutputSink {
		void print(Object msg, boolean newline);

		/** 마지막 출력 줄에 텍스트를 이어 붙인다(표준입력 에코, 진행 점 등). */
		default void appendInlineFragment(String fragment) {}
	}

	private static final OutputSink DEFAULT_OUTPUT_SINK = new OutputSink() {
		@Override
		public void print(Object msg, boolean newline) {
			try {
				if(newline) {
					System.out.println(msg);
				} else {
					System.out.print(msg);
				}
			} catch(Exception e) {}
		}
	};

	private static OutputSink outputSink = DEFAULT_OUTPUT_SINK;

	/**
	 * TUI Output 패널에 사용자 명시적 명령 결과만 붙일 때 true.
	 * false이면 outputSink는 화면에 쓰지 않는다(스풀 등 다른 기록은 유지).
	 */
	private static volatile boolean tuiPanelCapture = false;

	public static boolean isTuiPanelCapture() {
		return tuiPanelCapture;
	}

	public static void setTuiPanelCapture(boolean on) {
		tuiPanelCapture = on;
	}

	/**
	 * TUI 모드 활성 여부. true이면 System.out/err 직접 쓰기가 화면을 깨뜨릴 수 있으므로 Logger로 라우팅한다.
	 */
	private static volatile boolean tuiActive = false;

	public static boolean isTuiActive() {
		return tuiActive;
	}

	public static void setTuiActive(boolean on) {
		tuiActive = on;
	}

	/** TUI: 변경 CALL 등이 취소될 때 현재 사용자 명령 캡처를 롤백한다. */
	private static volatile Runnable tuiAbortUserCaptureRunnable;
	private static volatile Runnable tuiCommitCapturePreambleRunnable;

	public static void setTuiAbortUserCaptureRunnable(Runnable runnable) {
		tuiAbortUserCaptureRunnable = runnable;
	}

	public static void setTuiCommitCapturePreambleRunnable(Runnable runnable) {
		tuiCommitCapturePreambleRunnable = runnable;
	}

	public static void abortTuiUserCaptureSession() {
		Runnable r = tuiAbortUserCaptureRunnable;
		if(r != null) {
			try {
				r.run();
			} catch(Exception e) {}
		}
	}

	/** TUI: 확인 통과 후 Output에 > command 줄을 표시한다. */
	public static void commitTuiUserCapturePreamble() {
		Runnable r = tuiCommitCapturePreambleRunnable;
		if(r != null) {
			try {
				r.run();
			} catch(Exception e) {}
		}
	}

	/**
	 * 예외 스택 트레이스를 outputSink로 출력한다(System.err 사용 안 함).
	 */
	public static void logStackTrace(Throwable t) {
		if(t == null) {
			return;
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		String trace = sw.toString();
		if(trace.isEmpty()) {
			return;
		}
		String[] lines = trace.split("\\r?\\n", -1);
		for(int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if(i == lines.length - 1 && line.isEmpty()) {
				continue;
			}
			logln(line);
		}
	}

	/** 기본 동작: 빈 줄 여러 개 출력(IOUtil.clearScreen과 동일 개념). */
	private static Runnable clearScreenHandler = new Runnable() {
		@Override
		public void run() {
			for(int i = 0; i < 150; i++) {
				try {
					System.out.println("");
				} catch(Exception e) {}
			}
		}
	};

	/**
	 * 화면 출력 sink를 설정한다. null이면 기본(System.out)으로 복구한다.
	 */
	public static void setOutputSink(OutputSink sink) {
		outputSink = (sink != null) ? sink : DEFAULT_OUTPUT_SINK;
	}

	public static OutputSink getOutputSink() {
		return outputSink;
	}

	/**
	 * CLEAR 명령 등에서 호출. TUI에서는 출력 패널 비우기로 연결한다.
	 */
	public static void setClearScreenHandler(Runnable handler) {
		clearScreenHandler = (handler != null) ? handler : new Runnable() {
			@Override
			public void run() {
				for(int i = 0; i < 150; i++) {
					try {
						System.out.println("");
					} catch(Exception e) {}
				}
			}
		};
	}

	public static void clearScreenDisplay() {
		try {
			clearScreenHandler.run();
		} catch(Exception e) {}
	}

	/**
	 * 로그 레벨
	 * <xmp>
	 *  - RESULT : 결과 기록
	 *  - ERROR  : 결과,에러 기록
	 *  - INFO   : 결과,에러,정보 기록
	 *  - DEBUG  : 결과,에러,정보,상세 기록
	 * </xmp>
	 */
	public static enum Level { RESULT, ERROR, WARNING, INFO, DEBUG }
	
    public static final int BUFFER_SIZE = 8192;

    private static String spoolName = null;
    private static File f = null;
    private static FileWriter fw = null;
    private static BufferedWriter bw = null;
    private static boolean silent = false;
    private static Level showLevel = Level.WARNING;


    /**
     * 로그 기록 여부를 가져온다.
     * @return 기록하지 않으면 true
     */
    public static boolean getSilent() {
        return silent;
    }


    /**
     * 로그 기록 여부를 설정한다.
     * @param s 기록하지 않으려면 true
     */
    public static void setSilent(boolean s) {
        silent = s;
    }


    /**
     * 로그 레벨을 설정한다.<br/>
     * 설정된 레벨보다 낮은 로그 메시지는 무시된다.
     * @param level 로그 레벨
     */
    public static void setShowLevel(Level level) {
        showLevel = level;
    }

    
    /**
     * 로그 레벨을 가져온다.
     * @return 로그 레벨
     */
    public static Level getShowLevel() {
    	return showLevel;
    }
    
    
    /**
     * 입력받은 로그 레벨이 출력가능한 지 확인한다.<br/>
     * @param level 로그 레벨
     * @return 출력 가능하면 true
     */
    public static boolean isShowLevel(Level level) {
        if(!silent && level.ordinal() <= showLevel.ordinal()) {
        	return true;
        } else {
        	return false;
        }
    }

    
    /**
     * 스풀 파일명을 가져온다.
     * @return 파일명
     */
    public static String getSpoolName() {
        return spoolName;
    }

    
    /**
     * 스풀 파일명을 설정한다. 이후 로깅은 스풀 파일에 기록된다.
     * @param fname 파일명(경로 포함)
     * @param append append 모드이면 true
     */
    public static void setSpoolName(String fname, boolean append) {
        try {
            if(spoolName != null) {
                setSpoolOff();
            }
            if(fname != null) {
            	fname = StringUtil.stripOuterFilenameQuotes(fname);
            }
            spoolName = fname;
            f = new File(spoolName);
            fw = new FileWriter(f, append);
            bw = new BufferedWriter(fw, BUFFER_SIZE);
        } catch(Exception e) {}
    }


    /**
     * 스풀 파일 기록을 중지한다.
     */
    public static void setSpoolOff() {
        try {
            if(spoolName != null) {
                bw.flush();
                bw.close();
            }
            spoolName = null;
        } catch(Exception e) {}
    }


    /**
     * 로그를 기록한다.
     * @param msg 로그 메시지
     */
    public static void log(Object msg) {
        log(Level.RESULT, msg);
    }


    /**
     * 로그를 기록한다. 마지막에 newline 문자를 추가한다.
     * @param msg 로그 메시지
     */
    public static void logln(Object msg) {
        logln(Level.RESULT, msg);
    }

    /**
     * CLI·TUI 출력에 빈 줄 한 줄을 남긴다.
     * TUI는 빈 문자열을 건너뛰므로 줄을 확보하려고 no-break space를 쓴다.
     */
    public static void logBlankLine() {
        logln(tuiActive ? "\u00a0" : "");
    }

    
    /**
     * 로그를 기록한다.
     * @param level 로그 레벨
     * @param msg 로그 메시지
     */
    public static void log(Level level, Object msg) {
        if(isShowLevel(level)) {
            log(((level == Level.RESULT)?"":("["+level.name()+"] ")) + msg, false);
        }
    }


    /**
     * 로그를 기록한다. 마지막에 newline 문자를 추가한다.
     * @param level 로그 레벨
     * @param msg 로그 메시지
     */
    public static void logln(Level level, Object msg) {
        if(isShowLevel(level)) {
            log(((level == Level.RESULT)?"":("["+level.name()+"] ")) + msg, true);
        }
    }


    /**
     * 화면과 스풀에 로그를 기록한다.
     * @param msg 로그 메시지
     * @param newline newline 문자를 추가하려면 true
     */
    public static void log(Object msg, boolean newline) {
        try {
            outputSink.print(msg, newline);
            logOnlySpool(msg, newline);
        } catch(Exception e) {}
    }

    /**
     * TUI 출력의 마지막 줄에 인라인 텍스트를 이어 붙인다(진행 점, stdin 에코 등).
     */
    public static void appendTuiInlineFragment(String fragment) {
        if(!tuiActive || fragment == null || fragment.length() == 0) {
            return;
        }
        try {
            outputSink.appendInlineFragment(fragment);
        } catch(Exception e) {}
    }


    /**
     * 화면없이 스풀 로그만 기록한다.
     * @param msg 로그 메시지
     * @param newline newline 문자를 추가하려면 true
     */
    public static void logOnlySpool(Object msg, boolean newline) {
        String str = "";
        try {
            if(spoolName != null) {
                str = (msg instanceof String)?(String)msg:msg.toString();
                str = str.replaceAll("\r\n", "\n").replaceAll("\n", "\r\n");
                if(newline) {
                    bw.write(str);
                    bw.newLine();
                } else {
                    bw.write(str);
                }
            }
        } catch(Exception e) {}
    }

    
    /**
     * 스풀없이 화면 로그만 기록한다.
     * @param msg 로그 메시지
     * @param newline newline 문자를 추가하려면 true
     */
    public static void logWithoutSpool(Object msg, boolean newline) {
        try {
            outputSink.print(msg, newline);
        } catch(Exception e) {}
    }

}
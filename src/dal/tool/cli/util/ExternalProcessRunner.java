package dal.tool.cli.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 외부 셸 명령 실행: ProcessBuilder, stdin 종료, stdout/stderr 병렬 소비, 종료 코드.
 */
public final class ExternalProcessRunner {

	/** 한 줄 출력(일반 결과). */
	public interface Sink {
		void line(String s);

		void logError(String message);
	}

	private static final int JOIN_TIMEOUT_SEC = 120;

	private ExternalProcessRunner() {
	}

	/**
	 * 셸에 한 줄로 전달되는 사용자 명령을 실행하고 종료 코드를 반환한다.
	 *
	 * @param shellCommandLine 토큰을 공백으로 이은 명령 문자열(이미 파싱된 인자 경계 보존)
	 * @param unix {@code File.separatorChar == '/'}
	 * @param sink 출력 수신
	 * @return 프로세스 종료 코드(부모가 기다린 최상위 프로세스; {@code script} 래핑 시 그 코드)
	 * @throws IOException 프로세스 시작 실패
	 * @throws InterruptedException 대기 중 인터럽트
	 */
	public static int run(String shellCommandLine, boolean unix, Sink sink) throws IOException, InterruptedException {
		if (shellCommandLine == null) {
			shellCommandLine = "";
		}
		List<String> command = buildCommand(shellCommandLine, unix);
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(false);
		Process p = pb.start();
		try {
			p.getOutputStream().close();
		} catch (IOException e) {
			// ignore
		}
		Charset cs = Charset.defaultCharset();
		CountDownLatch latch = new CountDownLatch(2);
		IoHolder holder = new IoHolder();
		Thread outThread = new Thread(new StreamPump(p.getInputStream(), cs, sink, latch, holder), "jmxer-external-stdout");
		Thread errThread = new Thread(new StreamPump(p.getErrorStream(), cs, sink, latch, holder), "jmxer-external-stderr");
		outThread.setDaemon(true);
		errThread.setDaemon(true);
		outThread.start();
		errThread.start();
		try {
			if (!latch.await(JOIN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
				sink.logError("Timed out reading external process output.");
				p.destroyForcibly();
			}
		} catch (InterruptedException e) {
			p.destroyForcibly();
			Thread.currentThread().interrupt();
			throw e;
		}
		outThread.join(2000);
		errThread.join(2000);
		if (holder.ex != null) {
			sink.logError("I/O while reading external process: " + holder.ex.getMessage());
		}
		return p.waitFor();
	}

	private static List<String> buildCommand(String shellCommandLine, boolean unix) {
		List<String> cmd = new ArrayList<String>();
		if (!unix) {
			String comspec = System.getenv("COMSPEC");
			if (comspec != null && comspec.length() > 0) {
				cmd.add(comspec);
			} else {
				cmd.add("cmd.exe");
			}
			cmd.add("/C");
			cmd.add(shellCommandLine);
			return cmd;
		}
		cmd.add("/bin/sh");
		cmd.add("-c");
		cmd.add(shellCommandLine);
		return cmd;
	}

	private static final class IoHolder {
		volatile IOException ex;
	}

	private static final class StreamPump implements Runnable {
		private final InputStream in;
		private final Charset cs;
		private final Sink sink;
		private final CountDownLatch latch;
		private final IoHolder holder;

		StreamPump(InputStream in, Charset cs, Sink sink, CountDownLatch latch, IoHolder holder) {
			this.in = in;
			this.cs = cs;
			this.sink = sink;
			this.latch = latch;
			this.holder = holder;
		}

		@Override
		public void run() {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(in, cs));
				String line;
				while ((line = br.readLine()) != null) {
					sink.line(line);
				}
			} catch (IOException e) {
				holder.ex = e;
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						if (holder.ex == null) {
							holder.ex = e;
						}
					}
				}
				latch.countDown();
			}
		}
	}
}

package dal.tool.trace.jmxer.tui.support;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Thread UI 모드 주기적 새로고침 틱용 데몬 스케줄러.
 *
 */
public final class ThreadAutoRefreshScheduler {
	private static final String THREAD_NAME = "JmxTui-ThreadRefresh";

	private final ScheduledExecutorService executor;
	private final Runnable tickAction;
	private volatile ScheduledFuture<?> future;

	public ThreadAutoRefreshScheduler(Runnable tickAction) {
		this.tickAction = tickAction;
		this.executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, THREAD_NAME);
				t.setDaemon(true);
				return t;
			}
		});
	}

	public synchronized void start(int intervalSeconds) {
		stop();
		if(tickAction == null) {
			return;
		}
		int interval = intervalSeconds < 1 ? 1 : intervalSeconds;
		future = executor.scheduleAtFixedRate(tickAction, interval, interval, TimeUnit.SECONDS);
	}

	public synchronized void stop() {
		if(future != null) {
			future.cancel(false);
			future = null;
		}
	}

	public synchronized void reschedule(int intervalSeconds) {
		if(future != null) {
			start(intervalSeconds);
		}
	}

	public synchronized boolean isRunning() {
		return future != null;
	}

	public void shutdown() {
		stop();
		executor.shutdown();
	}
}

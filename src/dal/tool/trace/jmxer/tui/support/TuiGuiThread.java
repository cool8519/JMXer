package dal.tool.trace.jmxer.tui.support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

/**
 * 명령 워커 스레드에서 Lanterna GUI 스레드로 작업을 넘긴다(예: mutating CALL 확인).
 *
 */
public final class TuiGuiThread {

	private TuiGuiThread() {}

	public static boolean isGuiThread(WindowBasedTextGUI gui) {
		if(gui == null) {
			return false;
		}
		try {
			Thread guiThread = gui.getGUIThread().getThread();
			return guiThread != null && Thread.currentThread() == guiThread;
		} catch(Exception e) {
			return false;
		}
	}

	public static <T> T callOnGuiThread(WindowBasedTextGUI gui, final GuiCallable<T> callable) {
		if(gui == null || callable == null) {
			return null;
		}
		if(isGuiThread(gui)) {
			return callable.call();
		}
		final AtomicReference<T> result = new AtomicReference<T>();
		final AtomicReference<RuntimeException> error = new AtomicReference<RuntimeException>();
		final CountDownLatch latch = new CountDownLatch(1);
		gui.getGUIThread().invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					result.set(callable.call());
				} catch(RuntimeException e) {
					error.set(e);
				} finally {
					latch.countDown();
				}
			}
		});
		try {
			latch.await();
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		RuntimeException re = error.get();
		if(re != null) {
			throw re;
		}
		return result.get();
	}

	/**
	 * GUI 스레드에서 실행할 콜백.
	 *
	 */
	public interface GuiCallable<T> {
		T call();
	}
}

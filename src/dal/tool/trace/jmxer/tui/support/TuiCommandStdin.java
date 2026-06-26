package dal.tool.trace.jmxer.tui.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TUI 활성 중 System.in을 대체하여 feedLine으로 넣은 Command Pane 입력을 IOUtil.readLine이 읽을 수 있게 한다.
 *
 */
public final class TuiCommandStdin extends InputStream {

	/**
	 * 표준 입력 대기 상태가 바뀔 때 알리는 리스너.
	 *
	 */
	public interface AwaitingListener {
		void onAwaitingInput(boolean awaiting);
	}

	private final BlockingQueue<Byte> queue = new LinkedBlockingQueue<Byte>();
	private final AtomicBoolean awaiting = new AtomicBoolean(false);
	private volatile AwaitingListener awaitingListener;
	private volatile boolean closed;

	public void setAwaitingListener(AwaitingListener listener) {
		this.awaitingListener = listener;
	}

	public void feedLine(String line) {
		if(line == null) {
			line = "";
		}
		byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
		for(int i = 0; i < bytes.length; i++) {
			queue.offer(Byte.valueOf(bytes[i]));
		}
	}

	public boolean isAwaitingInput() {
		return awaiting.get();
	}

	public void wakeReaders() {
		feedLine("");
	}

	@Override
	public synchronized int read() throws IOException {
		if(closed) {
			return -1;
		}
		try {
			if(queue.isEmpty()) {
				setAwaiting(true);
			}
			Byte b = queue.take();
			setAwaiting(false);
			return b.byteValue() & 0xff;
		} catch(InterruptedException e) {
			setAwaiting(false);
			Thread.currentThread().interrupt();
			throw new IOException("stdin read interrupted", e);
		}
	}

	public void closeBridge() {
		closed = true;
		setAwaiting(false);
		wakeReaders();
	}

	private void setAwaiting(boolean on) {
		if(awaiting.compareAndSet(!on, on)) {
			AwaitingListener l = awaitingListener;
			if(l != null) {
				l.onAwaitingInput(on);
			}
		}
	}
}

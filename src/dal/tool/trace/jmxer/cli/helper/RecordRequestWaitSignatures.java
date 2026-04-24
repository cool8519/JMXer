package dal.tool.trace.jmxer.cli.helper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RecordRequestWaitSignatures {

	private static final Set<String> WAITING_SIGNATURES = new HashSet<String>(Arrays.asList(
		"org.apache.tomcat.util.threads.TaskQueue.take",
		"org.eclipse.jetty.util.thread.QueuedThreadPool.idleJobPoll",
		"org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.idleJobPoll",
		"io.netty.util.concurrent.SingleThreadEventExecutor.takeTask",
		"org.jboss.threads.EnhancedQueueExecutor.takeTask",
		"org.glassfish.grizzly.threadpool.SyncThreadPool$SyncThreadWorker.getTask",
		"org.glassfish.grizzly.threadpool.FixedThreadPool$BasicWorker.getTask",
		"com.sun.grizzly.util.SyncThreadPool$SyncThreadWorker.getTask",
		"weblogic.work.ExecuteThread.waitForRequest",
		"com.ibm.ws.util.ThreadPool.getTask",
		"com.ibm.ws.util.ThreadPool$Worker.getTask",
		"com.ibm.ws.util.BoundedBuffer.waitGet_",
		"com.ibm.ws.util.BoundedBuffer.take",
		"com.ibm.ws.threading.internal.Worker.getWork",
		"jeus.util.pool.auto.LinkedBlockingQueue.take",
		"jeus.util.ThreadPoolExecutor.getTask",
		"jeus.servlet.engine.WebThreadPoolExecutor.getTask",
		"jeus.util.threadpool.ThreadPool$Worker.getTask",
		"com.caucho.env.thread.ResinThread.waitForTask"
	));

	private RecordRequestWaitSignatures() {}

	public static boolean isWaitingSignature(String className, String methodName) {
		return WAITING_SIGNATURES.contains(className + "." + methodName);
	}
}

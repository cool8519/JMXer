package dal.tool.trace.jmxer.cli.data;

import java.io.Serializable;

/**
 * RECORD RESOURCE 샘플의 스레드별 CPU·할당 메모리 사용량
 *
 */
public class ResourceUsage implements Serializable {

	private static final long serialVersionUID = 8269957265134857655L;
	
	public String threadName;
	public long startCpu;
	public long startMem;
	public long currCpu;
	public long currMem;
	
	public ResourceUsage(String threadName, long cpu, long mem) {
		this.threadName = threadName;
		this.startCpu = cpu;
		this.startMem = mem;
	}
	
	public void update(long cpu, long mem) {
		this.currCpu = cpu;
		this.currMem = mem;
	}

}

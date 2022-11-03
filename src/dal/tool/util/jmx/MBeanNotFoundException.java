package dal.tool.util.jmx;


/**
 * JMX 호출시 MBean을 찾을 수 없을 때 발생하는 예외클래스
 * @author 권영달
 *
 */
public class MBeanNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private Throwable ex;


    public MBeanNotFoundException() {
		super((Throwable)null);
    }

    public MBeanNotFoundException(String s) {
		super(s, null);
    }

    public MBeanNotFoundException(String s, Throwable ex) {
		super(s, null);
		this.ex = ex;
    }

    public Throwable getException() {
        return ex;
    }

    public Throwable getCause() {
    	if(ex != null)
    		return ex.getCause();
    	else
    		return null;
    }

}
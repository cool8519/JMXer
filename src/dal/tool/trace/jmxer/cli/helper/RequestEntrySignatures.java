package dal.tool.trace.jmxer.cli.helper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RequestEntrySignatures {

	private static final Set<String> ENTRY_SIGNATURES = new HashSet<String>(Arrays.asList(
		"javax.servlet.http.HttpServlet.service",
		"jakarta.servlet.http.HttpServlet.service",
		"org.springframework.web.servlet.FrameworkServlet.service",
		"org.springframework.web.servlet.DispatcherServlet.doDispatch",
		"org.apache.catalina.core.StandardWrapperValve.invoke",
		"org.apache.catalina.connector.CoyoteAdapter.service",
		"org.eclipse.jetty.server.handler.HandlerWrapper.handle",
		"org.eclipse.jetty.servlet.ServletHandler.doHandle",
		"weblogic.servlet.internal.ServletRequestImpl.run",
		"weblogic.servlet.internal.WebAppServletContext.execute",
		"com.ibm.ws.webcontainer.servlet.ServletWrapper.service",
		"com.ibm.ws.webcontainer.srt.SRTServletRequest.dispatch",
		"jeus.servlet.engine.ServletWrapper.service",
		"jeus.servlet.engine.WebContainer.service"
	));

	private RequestEntrySignatures() {}

	public static boolean isEntrySignature(String className, String methodName) {
		return isEntrySignature(className, methodName, null);
	}

	public static boolean isEntrySignature(String className, String methodName, Set<String> userSignatures) {
		if(className == null || methodName == null) {
			return false;
		}
		String key = className + "." + methodName;
		if(ENTRY_SIGNATURES.contains(key)) {
			return true;
		}
		if(userSignatures != null && !userSignatures.isEmpty() && userSignatures.contains(key)) {
			return true;
		}
		return "doFilter".equals(methodName);
	}
}

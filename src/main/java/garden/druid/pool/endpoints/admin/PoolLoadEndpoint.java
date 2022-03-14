package garden.druid.pool.endpoints.admin;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.logging.Level;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.servlet.annotation.WebServlet;

import garden.druid.base.http.auth.api.UserLevel;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.RequireAuthenticator;
import garden.druid.base.http.rest.annotation.RequireUserLevel;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.base.logging.Logger;

@WebServlet(value = { "/api/admin/v1/poolLoad" })
@RestPattern( uri = "/api/admin/v1/poolLoad")
@RequireAuthenticator()
@RequireUserLevel( level = UserLevel.ADMIN)
public class PoolLoadEndpoint extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;
	
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public HashMap<String, Object> runUpdateBalance() {
		HashMap<String, Object> rtn = new HashMap<String, Object>();
		rtn.put("max_memory", getMaxMemory());
		rtn.put("used_memory", getUsedMemory() );
		rtn.put("total_memory", getTotalMemory());
		rtn.put("free_memory", getFreeMemory());
		rtn.put("load", load());
		return rtn;
	}

	private double load() {
		try {
		    return (Double)ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("java.lang:type=OperatingSystem"), "ProcessCpuLoad");
		} catch (JMException ex) {
			Logger.getInstance().log(Level.WARNING, "Error in PoolLoadEndpoint.load", ex);
		    return -1;
		}
	}
	
	private static long getMaxMemory() {
	    return Runtime.getRuntime().maxMemory();
	}

	private static long getUsedMemory() {
	    return getMaxMemory() - getFreeMemory();
	}

	private static long getTotalMemory() {
	    return Runtime.getRuntime().totalMemory();
	}

	private static long getFreeMemory() {
	    return Runtime.getRuntime().freeMemory();
	}
}

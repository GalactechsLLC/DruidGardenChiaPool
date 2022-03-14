package garden.druid.pool.endpoints.admin;

import java.util.HashMap;
import javax.servlet.annotation.WebServlet;

import garden.druid.pool.Pool;
import garden.druid.base.http.auth.api.UserLevel;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.DELETE;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.RequireAuthenticator;
import garden.druid.base.http.rest.annotation.RequireUserLevel;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.annotation.UriVariable;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.base.http.rest.exceptions.BadRequestException;

@WebServlet(value = { "/api/admin/v1/poolpayments", "/api/admin/v1/poolpayments/*" })
@RestPattern( uri = "/api/admin/v1/poolpayments/?{name}?")
@RequireAuthenticator()
@RequireUserLevel( level = UserLevel.ADMIN)
public class PoolPaymentsEndpoint extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;
	
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public HashMap<String, Object> getPoolPayments() {
		HashMap<String, Object> rtnMap = new HashMap<String, Object>();
		rtnMap.put("collection", Pool.getInstance().getPending_Collections().toArray());
		rtnMap.put("farmer", Pool.getInstance().getPending_farmer_payments().toArray());
		return rtnMap;
	}
	
	@DELETE
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public Object deletePendingData( @UriVariable(name="name") String name) throws BadRequestException {
		if("collection".equalsIgnoreCase(name)) {
			Pool.getInstance().getPending_Collections().clear();
			return Pool.getInstance().getPending_Collections();
		} else if("farmer".equalsIgnoreCase(name)) {
			Pool.getInstance().getPending_farmer_payments().clear();
			return Pool.getInstance().getPending_farmer_payments();
		} else {
			throw new BadRequestException("Invalid name: " + name);
		}
	}
}

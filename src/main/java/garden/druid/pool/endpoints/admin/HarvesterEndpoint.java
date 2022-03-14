package garden.druid.pool.endpoints.admin;

import java.util.ArrayList;
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
import garden.druid.base.http.rest.exceptions.NotFoundException;
import garden.druid.chia.rpc.FarmerRpcClient;
import garden.druid.chia.types.farmer.Harvester;

@WebServlet(value = { "/api/admin/v1/harvesters" })
@RestPattern( uri = "/api/admin/v1/harvesters")
@RequireAuthenticator()
@RequireUserLevel( level = UserLevel.ADMIN)
public class HarvesterEndpoint extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;
	
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public ArrayList<Harvester> getHarvesters() throws NotFoundException {
		FarmerRpcClient client = new FarmerRpcClient("FARMER_HOST", 8559);
		ArrayList<Harvester> harvesters = client.get_harvesters();
		if(harvesters != null) {
			return harvesters;
		} else {
			throw new NotFoundException("No harvesters Found");
		}
	}
}
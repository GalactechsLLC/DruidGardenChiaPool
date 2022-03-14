package garden.druid.pool.endpoints.poolAPI;

import javax.servlet.annotation.WebServlet;

import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.annotation.UriVariable;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.pool.database.PoolFeaturesDAO;
import garden.druid.pool.types.PublicFarmerData;

@WebServlet( value = "/api/rest/v1/farmer/public/*")
@RestPattern( uri = "/api/rest/v1/farmer/public/{launcherId}")
public class PoolFarmer extends RestEndpoint{

	private static final long serialVersionUID = 1L;

	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	protected PublicFarmerData loadFarmerRecord(@UriVariable(name="launcherId") String launcherId) {
		return PoolFeaturesDAO.getPublicFarmerData(new Bytes32(launcherId));
	}

}

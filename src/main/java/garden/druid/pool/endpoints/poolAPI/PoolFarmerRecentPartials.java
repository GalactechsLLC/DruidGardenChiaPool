package garden.druid.pool.endpoints.poolAPI;

import java.util.ArrayList;

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
import garden.druid.pool.database.PoolPartialsDAO;
import garden.druid.pool.types.RecentPartial;

@WebServlet( value = "/api/rest/v1/partials/*")
@RestPattern( uri = "/api/rest/v1/partials/{launcherId}/{limit}")
public class PoolFarmerRecentPartials extends RestEndpoint{

	private static final long serialVersionUID = 3718112580807732768L;

	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	protected ArrayList<RecentPartial> loadRecentpartials(@UriVariable(name="launcherId") String launcherId, @UriVariable(name="limit") int limit) {
		return PoolPartialsDAO.getRecentPartials(new Bytes32(launcherId), Math.min(Math.max(limit,  1), 50));
	}
}


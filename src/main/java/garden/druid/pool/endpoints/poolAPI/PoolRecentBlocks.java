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
import garden.druid.pool.database.PoolFeaturesDAO;
import garden.druid.pool.types.RecentBlock;

@WebServlet( value = { "/api/rest/v1/recentblocks/*", "/api/rest/v1/recentblocks" })
@RestPattern( uri = "/api/rest/v1/recentblocks/{offset}/{limit}")
public class PoolRecentBlocks extends RestEndpoint {

	private static final long serialVersionUID = 3424459535393532446L;
	
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public ArrayList<RecentBlock> doGet(@UriVariable(name="limit") int offset, @UriVariable(name="limit") int limit) {
		return PoolFeaturesDAO.getBlockHistory(Math.max(offset, 0), limit > 0 ? Math.min(50, limit) : 20 );
	}
}

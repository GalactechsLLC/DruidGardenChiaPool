package garden.druid.pool.endpoints.poolAPI;

import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;

import garden.druid.base.cache.DataCache;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.pool.database.PoolFeaturesDAO;
import garden.druid.pool.types.LeaderBoardRecord;

@WebServlet(value = { "/api/rest/v1/leaderboard" })
@RestPattern( uri = "/api/rest/v1/leaderboard")
public class PoolLeaderBoard extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;
	
	private static DataCache<ArrayList<LeaderBoardRecord>> cachedState = new DataCache<ArrayList<LeaderBoardRecord>>(new ArrayList<LeaderBoardRecord>(), 0);	
	
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public ArrayList<LeaderBoardRecord> getLeaderboard() {
		if(cachedState == null || cachedState.isExpired()) {
			cachedState = new DataCache<ArrayList<LeaderBoardRecord>>(PoolFeaturesDAO.getLeaderBoard(), 30000);
		}
		return cachedState.getData();
	}
}

package garden.druid.pool.endpoints.poolAPI;

import java.time.Instant;
import java.util.ArrayList;

import javax.servlet.annotation.WebServlet;

import garden.druid.base.cache.DataCache;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.annotation.UriVariable;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.pool.database.PoolPartialsDAO;
import garden.druid.pool.types.Dataset;

@WebServlet( value = { "/api/rest/v1/partialtimeline/*", "/api/rest/v1/partialtimeline" })
@RestPattern( uri = "/api/rest/v1/partialtimeline/{start}/{end}")
public class PoolPartialTimeline extends RestEndpoint {
		
	private static final long serialVersionUID = 1L;
	
	private static DataCache<ArrayList<Dataset<Long,Integer>>> cachedState = null;	
	
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.URL_PARAMS)
	public ArrayList<Dataset<Long,Integer>> getPartialTimeline(@UriVariable(name="start") String start, @UriVariable(name="end") String end) {
		long st, ed;
		try {
			st = start != null ? Long.parseLong(start) : -1;
		} catch(Exception e) {
			st = -1;
		}
		try {
			ed = end != null ? Long.parseLong(end) : -1;
		} catch(Exception e) {
			ed = -1;
		}
		if(st <= 0) {
			ed = Instant.now().getEpochSecond();
			st = Instant.now().getEpochSecond() - (60 * 60 * 24 * 7);
		} else if(ed <= 0 || ed <= st) {
			ed = Math.max(Instant.now().getEpochSecond(), st + (60 * 60 * 24 * 7));
		}
		if(cachedState == null || cachedState.isExpired()) {
			cachedState = new DataCache<ArrayList<Dataset<Long,Integer>>>(PoolPartialsDAO.getPartialsOverTime(st, ed), 300000);
		}
		return cachedState.getData();
	}
}
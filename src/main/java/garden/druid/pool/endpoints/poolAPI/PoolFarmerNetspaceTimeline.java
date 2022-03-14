package garden.druid.pool.endpoints.poolAPI;

import java.time.Instant;
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
import garden.druid.pool.database.PoolFeaturesDAO;
import garden.druid.pool.types.Dataset;

@WebServlet( value = "/api/rest/v1/farmer/netspacetimeline/*")
@RestPattern( uri = "/api/rest/v1/farmer/netspacetimeline/{launcherId}/?{start}?/?{end}?")
public class PoolFarmerNetspaceTimeline extends RestEndpoint{

	private static final long serialVersionUID = 1L;
	
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.URL_PARAMS)
	protected ArrayList<Dataset<Long, Long>> loadFarmerNetspaceTimeline(@UriVariable(name="launcherId") Bytes32 launcherId, @UriVariable(name="start") String start, @UriVariable(name="end") String end) {
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
		return PoolFeaturesDAO.getFarmerNetspaceTimeline(launcherId, st, ed);
	}
}


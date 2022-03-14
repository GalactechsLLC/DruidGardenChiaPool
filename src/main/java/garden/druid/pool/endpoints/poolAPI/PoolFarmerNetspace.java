package garden.druid.pool.endpoints.poolAPI;

import javax.servlet.annotation.WebServlet;

import garden.druid.base.cache.Cache;
import garden.druid.base.cache.DataCache;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.annotation.UriVariable;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.database.PoolFeaturesDAO;

@WebServlet( value = "/api/rest/v1/farmer/netspace/*")
@RestPattern( uri = "/api/rest/v1/farmer/netspace/{launcherId}")
public class PoolFarmerNetspace extends RestEndpoint{

	private static final long serialVersionUID = 1L;
	
	private Cache<Bytes32, DataCache<NativeUInt64>> cache = new Cache<Bytes32, DataCache<NativeUInt64>>(1000);
		
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	protected NativeUInt64 loadFarmerNetspace(@UriVariable(name="launcherId") Bytes32 launcherId) {
		DataCache<NativeUInt64> data = cache.get(launcherId);
		if(data != null && !data.isExpired()) {
			return data.getData();
		} else {
			NativeUInt64 netspace = PoolFeaturesDAO.getNetspace(launcherId);
			if(netspace != null) {
				data = new DataCache<NativeUInt64>(netspace, 1800000/*30 mins in milliseconds*/);
				cache.put(launcherId, data);
			}
			return netspace;
		}
	}
}

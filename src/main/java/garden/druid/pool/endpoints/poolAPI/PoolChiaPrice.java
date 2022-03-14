package garden.druid.pool.endpoints.poolAPI;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.servlet.annotation.WebServlet;

import garden.druid.base.cache.DataCache;
import garden.druid.base.http.HttpClient;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;

@WebServlet(value = { "/api/rest/v1/chia/price" })
@RestPattern( uri = "/api/rest/v1/chia/price")
public class PoolChiaPrice extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;

	private static DataCache<String> cachedState = null;	

	@GET
	@ReturnType(type=ReturnTypes.STRING)
	@Consumes(consumer=ConsumerTypes.NONE)
	public String fetchChiaPrice() throws IOException, URISyntaxException {
		if(cachedState == null || cachedState.isExpired()) {
			byte[] resp = HttpClient.get("https://www.coinbase.com/api/v2/assets/prices/cf1f1de6-0a6e-5e15-a68c-e4678a67e60e?base=USD", new HashMap<String,String>() {
				private static final long serialVersionUID = 1L;
				{put("Content-Type", "application/json");}}
			);
			String tmp = new String(resp, StandardCharsets.UTF_8);
			if(cachedState == null || tmp.contains("\"base\":\"XCH\"")) {
				cachedState = new DataCache<String>(tmp, 30000);
			}
		}
		return cachedState.getData();
	}
}

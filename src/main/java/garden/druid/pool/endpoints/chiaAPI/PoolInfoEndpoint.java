package garden.druid.pool.endpoints.chiaAPI;

import java.util.HashMap;

import javax.servlet.annotation.WebServlet;

import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.chia.crypt.bech32.Bech32;
import garden.druid.pool.Pool;
import garden.druid.pool.types.PoolError;
import garden.druid.pool.types.PoolInfo;

@WebServlet(value = { "/pool_info" })
@RestPattern( uri = "/pool_info")
public class PoolInfoEndpoint extends RestEndpoint {

	private static final long serialVersionUID = 1L;

	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public Object getPoolInfo() {
		try {
			PoolInfo info = Pool.getInstance().getPoolInfo();
			HashMap<String, Object> rtn = new HashMap<String, Object>();
			rtn.put("description", info.getDescription());
			rtn.put("fee", info.getFee());
			rtn.put("logo_url", info.getLogoURL());
			rtn.put("minimum_difficulty", info.getMin_difficulty());
			rtn.put("name", info.getName());
			rtn.put("protocol_version", info.getProtocolVersion());
			rtn.put("relative_lock_height", info.getRelative_lock_height());
			rtn.put("target_puzzle_hash", Bech32.decodePuzzleHash(info.getPool_contract_puzzle_hash()));
			rtn.put("authentication_token_timeout", info.getAuthentication_token_timeout());
			return rtn;
		} catch (Exception e) {
			return PoolError.REQUEST_FAILED;
		}
	}
}
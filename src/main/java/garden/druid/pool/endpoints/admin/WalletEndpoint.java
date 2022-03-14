package garden.druid.pool.endpoints.admin;

import java.util.ArrayList;
import java.util.HashMap;
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
import garden.druid.chia.rpc.WalletRpcClient;
import garden.druid.chia.types.blockchain.WalletInfo;
import garden.druid.chia.types.ints.NativeUInt32;

@WebServlet(value = { "/api/admin/v1/wallets" })
@RestPattern( uri = "/api/admin/v1/wallets")
@RequireAuthenticator()
@RequireUserLevel( level = UserLevel.ADMIN)
public class WalletEndpoint extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;
	
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public HashMap<String, Object> getHarvesters() {
		WalletRpcClient client = new WalletRpcClient("FARMER_HOST", 9256);
		client.log_in(new NativeUInt32().fromString("PUT WALLET FINGERPRINT HERE"));
		HashMap<String, Object> rtn = new HashMap<>();
		rtn.put("sync", client.get_sync_status());
		ArrayList<HashMap<String, Object>> wallets = new ArrayList<>();
		for(WalletInfo info : client.get_wallets()) {
			HashMap<String, Object> wallet = new HashMap<>();
			wallet.put("info", info);
			wallet.put("balance", client.get_wallet_balance(info.getId()));
			wallets.add(wallet);
		}
		rtn.put("wallets", wallets);
		return rtn;
	}
}

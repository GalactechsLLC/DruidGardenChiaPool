package garden.druid.pool.endpoints.admin;

import java.util.ArrayList;
import java.util.Collection;
import javax.servlet.annotation.WebServlet;

import garden.druid.base.http.auth.api.UserLevel;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.POST;
import garden.druid.base.http.rest.annotation.RequireAuthenticator;
import garden.druid.base.http.rest.annotation.RequireUserLevel;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.pool.Pool;
import garden.druid.chia.types.blockchain.PendingPayment;

@WebServlet(value = { "/api/admin/v1/processcollections" })
@RestPattern( uri = "/api/admin/v1/processcollections")
@RequireAuthenticator()
@RequireUserLevel( level = UserLevel.ADMIN)
public class ProcessCollectionEndpoint extends RestEndpoint{

	private static final long serialVersionUID = 1L;

	@POST
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.URL_PARAMS)
	protected Collection<ArrayList<PendingPayment>> processCollections() {
		Pool.getInstance().runProcessCollections();
		return Pool.getInstance().getPending_Collections();
	}
}

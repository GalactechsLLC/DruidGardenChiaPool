package garden.druid.pool.endpoints.chiaAPI;

import java.io.*;
import java.time.Instant;

import javax.servlet.http.*;

import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.servlet.annotation.WebServlet;

import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Body;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.POST;
import garden.druid.base.http.rest.annotation.Request;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.base.logging.Logger;
import garden.druid.pool.Pool;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.endpoints.chiaAPI.types.PostPartialRequest;
import garden.druid.pool.endpoints.chiaAPI.types.PostPartialResponse;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PoolError;

@WebServlet(value = { "/partial" })
@RestPattern( uri = "/partial")
public class PoolPartialEndpoint extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;

	@POST
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.BODY)
	public Object processpartial(@Request HttpServletRequest request, @Body String body) throws IOException {
		try {
			Instant start_time = Instant.now();
			PostPartialRequest partialRequest = gson.fromJson(body, PostPartialRequest.class);
			if (!Pool.getInstance().validateAuthenticationToken(partialRequest.getPayload().getAuthenticationToken())) {
				Logger.getInstance().log(Level.WARNING, "Invalid Auth Token in post partial: " + partialRequest.getPayload().getAuthenticationToken());
				return PoolError.INVALID_AUTHENTICATION_TOKEN;
			}

			FarmerRecord farmer_record = PoolDAO.getFarmerRecord(partialRequest.getPayload().getLauncherId());
			if (farmer_record == null) {
				Logger.getInstance().log(Level.WARNING, "Farmer with launcher_id " + partialRequest.getPayload().getLauncherId() + " not known.");
				return PoolError.FARMER_NOT_KNOWN;
			}
			Future<PostPartialResponse> fPartialResponse = Pool.getInstance().submitPartial(partialRequest, farmer_record, start_time);
			PostPartialResponse pResponse = fPartialResponse.get();
			if (pResponse == null) {
				Logger.getInstance().log(Level.WARNING, "Farmer with launcher_id " + partialRequest.getPayload().getLauncherId() + " failed.");
				return PoolError.REQUEST_FAILED;
			} else {
				Logger.getInstance().log(Level.INFO, "Processed partial for (" + partialRequest.getPayload().getLauncherId() + "), start time: " + start_time.toString() + ", end time " + Instant.now().toString());
				return pResponse;
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.WARNING, "Error in PoolPartialEndpoint.doPost", e);
			return PoolError.REQUEST_FAILED;
		}
	}
}

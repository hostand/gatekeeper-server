package jhi.gatekeeper.server.resource;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jooq.DSLContext;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.*;

import static jhi.gatekeeper.server.database.tables.AccessRequests.*;
import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
@Path("request/existing/{requestId}/decision")
@Secured(UserType.ADMIN)
public class ExistingRequestDecisionResource extends ContextResource
{
	public static boolean decide(Integer id, RequestDecision request, HttpServletResponse resp)
		throws SQLException, IOException
	{
		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			// Get the request with the given id
			AccessRequestsRecord accessRequest = context.selectFrom(ACCESS_REQUESTS)
														.where(ACCESS_REQUESTS.ID.eq(id))
														.fetchAnyInto(ACCESS_REQUESTS);

			if (accessRequest != null)
			{
				// Get user and database
				Users user = context.selectFrom(USERS)
									.where(USERS.ID.eq(accessRequest.getUserId()))
									.fetchAnyInto(Users.class);
				DatabaseSystems database = context.selectFrom(DATABASE_SYSTEMS)
												  .where(DATABASE_SYSTEMS.ID.eq(accessRequest.getDatabaseSystemId()))
												  .fetchAnyInto(DatabaseSystems.class);

				// If it's been rejected, update database,
				switch (request.getDecision())
				{
					case REJECT:
						// Set rejected flag
						accessRequest.setHasBeenRejected((byte) 1);
						accessRequest.store();

						// Send an email letting the user know
						Email.sendAccessRequestRejected(Locale.ENGLISH, user, request.getFeedback());
						break;
					case APPROVE:
						// Else, it has been approved -> set permission
						UserHasAccessToDatabasesRecord record = context.newRecord(USER_HAS_ACCESS_TO_DATABASES);
						record.setDatabaseId(accessRequest.getDatabaseSystemId());
						record.setUserId(accessRequest.getUserId());
						record.setUserTypeId(2);
						record.store();

						// Delete the request
						accessRequest.delete();

						// Email the user
						Email.sendAccessRequestApproved(Locale.ENGLISH, user, database);
						break;
				}
				return true;
			}
			else
			{
				resp.sendError(Response.Status.NOT_FOUND.getStatusCode());
				return false;
			}
		}
		catch (EmailException e)
		{
			e.printStackTrace();
			resp.sendError(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), StatusMessage.UNAVAILABLE_EMAIL.name());
			return false;
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean postDecision(RequestDecision request, @PathParam("requestId") Integer requestId)
		throws IOException, SQLException
	{
		if (requestId == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID.name());
			return false;
		}
		if (request == null || !Objects.equals(request.getRequestId(), requestId))
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
			return false;
		}

		return decide(requestId, request, resp);
	}
}

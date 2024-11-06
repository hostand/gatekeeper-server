package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.AccessRequestsRecord;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.*;
import org.jooq.*;
import org.jooq.Record;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static jhi.gatekeeper.server.database.tables.AccessRequests.*;
import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.Users.*;
import static jhi.gatekeeper.server.database.tables.ViewAccessRequestUserDetails.*;

/**
 * @author Sebastian Raubach
 */
@Path("request/existing")
@Secured(UserType.ADMIN)
public class ExistingRequestResource extends PaginatedServerResource
{
	@DELETE
	@Path("/{requestId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean deleteExistingRequest(@PathParam("requestId") Integer requestId)
		throws IOException, SQLException
	{
		if (requestId == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID.name());
			return false;
		}

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			return context.deleteFrom(ACCESS_REQUESTS)
						  .where(ACCESS_REQUESTS.ID.eq(requestId))
						  .execute() > 0;
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean postExistingRequest(NewAccessRequest request)
		throws IOException, SQLException
	{
		if (request == null || request.getUserId() == null || request.getDatabaseSystemId() == null)
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode(), StatusMessage.NOT_FOUND_PAYLOAD.name());
			return false;
		}

		Locale locale = request.getJavaLocale();

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			// Get the user and the database
			Users user = context.selectFrom(USERS)
								.where(USERS.ID.eq(request.getUserId()))
								.fetchAnyInto(Users.class);
			DatabaseSystems database = context.selectFrom(DATABASE_SYSTEMS)
											  .where(DATABASE_SYSTEMS.ID.eq(request.getDatabaseSystemId()))
											  .fetchAnyInto(DatabaseSystems.class);

			// If either are null, fail
			if (user == null || database == null)
			{
				resp.sendError(Response.Status.BAD_REQUEST.getStatusCode(), StatusMessage.NOT_FOUND_USER.name());
				return false;
			}

			boolean alreadyHasAccess = context.fetchExists(USER_HAS_ACCESS_TO_DATABASES, USER_HAS_ACCESS_TO_DATABASES.USER_ID.eq(user.getId())
																															 .and(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID.eq(database.getId())));
			boolean alreadyRequested = context.fetchExists(ACCESS_REQUESTS, ACCESS_REQUESTS.USER_ID.eq(user.getId())
																								   .and(ACCESS_REQUESTS.DATABASE_SYSTEM_ID.eq(database.getId()))
																								   .and(ACCESS_REQUESTS.HAS_BEEN_REJECTED.eq((byte) 0)));

			if (alreadyHasAccess)
			{
				resp.sendError(Response.Status.CONFLICT.getStatusCode(), StatusMessage.CONFLICT_USER_ALREADY_HAS_ACCESS.name());
				return false;
			}
			if (alreadyRequested)
			{
				resp.sendError(Response.Status.CONFLICT.getStatusCode(), StatusMessage.CONFLICT_USER_ALREADY_REQUESTED_ACCESS.name());
				return false;
			}

			if (request.getNeedsApproval() == 1)
			{
				AccessRequestsRecord record = context.newRecord(ACCESS_REQUESTS, request);
				record.setCreatedOn(new Timestamp(System.currentTimeMillis()));
				// If it needs approval, store it in the database for now, then send emails
				int result = record.store();
				Email.sendAwaitingApproval(locale, user);
				Email.sendAdministratorNotification(locale, database, true);
				return result > 0;
			}
			else
			{
				// If it doesn't need approval, make the decision straightaway.
				AccessRequestsRecord record = context.newRecord(ACCESS_REQUESTS, request.getAccessRequest());
				record.setCreatedOn(new Timestamp(System.currentTimeMillis()));
				record.store();
				RequestDecision decision = new RequestDecision(record.getId(), Decision.APPROVE, null);
				decision.setJavaLocale(locale);
				ExistingRequestDecisionResource.decide(record.getId(), decision, resp);
				Email.sendAdministratorNotification(locale, database, false);
				return true;
			}
		}
		catch (EmailException e)
		{
			e.printStackTrace();
			resp.sendError(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), StatusMessage.UNAVAILABLE_EMAIL.name());
			return false;
		}
	}

	@GET
	@Path("/{requestId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<ViewAccessRequestUserDetails>> getExistingRequestById(@PathParam("requestId") Integer requestId)
		throws SQLException
	{
		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			SelectConditionStep<Record> step = context.select().hint("SQL_CALC_FOUND_ROWS").from(VIEW_ACCESS_REQUEST_USER_DETAILS)
													  .where(VIEW_ACCESS_REQUEST_USER_DETAILS.HAS_BEEN_REJECTED.eq((byte) 0));

			if (requestId != null)
				step.and(VIEW_ACCESS_REQUEST_USER_DETAILS.ID.eq(requestId));

			if (query != null)
				step.and(VIEW_ACCESS_REQUEST_USER_DETAILS.USERNAME.contains(query)
																  .or(VIEW_ACCESS_REQUEST_USER_DETAILS.DATABASE_SERVER_NAME.contains(query))
																  .or(VIEW_ACCESS_REQUEST_USER_DETAILS.DATABASE_SYSTEM_NAME.contains(query))
																  .or(VIEW_ACCESS_REQUEST_USER_DETAILS.EMAIL_ADDRESS.contains(query))
																  .or(VIEW_ACCESS_REQUEST_USER_DETAILS.NAME.contains(query))
																  .or(VIEW_ACCESS_REQUEST_USER_DETAILS.ACRONYM.contains(query))
																  .or(VIEW_ACCESS_REQUEST_USER_DETAILS.ADDRESS.contains(query))
																  .or(VIEW_ACCESS_REQUEST_USER_DETAILS.FULL_NAME.contains(query)));

			List<ViewAccessRequestUserDetails> result = setPaginationAndOrderBy(step)
				.fetch()
				.into(ViewAccessRequestUserDetails.class);

			Integer count = context.fetchOne("SELECT FOUND_ROWS()").into(Integer.class);

			return new PaginatedResult<>(result, count);
		}
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<ViewAccessRequestUserDetails>> getExistingRequests()
		throws SQLException
	{
		return this.getExistingRequestById(null);
	}
}

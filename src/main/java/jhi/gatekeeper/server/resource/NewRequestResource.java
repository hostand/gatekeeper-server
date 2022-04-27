package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.auth.BCrypt;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.UnapprovedUsersRecord;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.*;
import jhi.gatekeeper.server.util.watcher.PropertyWatcher;
import org.jooq.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static jhi.gatekeeper.server.database.tables.UnapprovedUsers.*;
import static jhi.gatekeeper.server.database.tables.Users.*;
import static jhi.gatekeeper.server.database.tables.ViewUnapprovedUserDetails.*;

/**
 * @author Sebastian Raubach
 */
@Path("request/new")
@Secured(UserType.ADMIN)
public class NewRequestResource extends PaginatedServerResource
{
	@DELETE
	@Path("/{requestId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean deleteNewRequest(@PathParam("requestId") Integer requestId)
		throws IOException, SQLException
	{
		if (requestId == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID.name());
			return false;
		}

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			return context.deleteFrom(UNAPPROVED_USERS)
						  .where(UNAPPROVED_USERS.ID.eq(requestId))
						  .execute() > 0;
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean postNewRequest(NewUnapprovedUser request)
		throws IOException, SQLException
	{
		if (request == null || request.getDatabaseSystemId() == null
			|| StringUtils.isEmpty(request.getUserUsername(), request.getUserPassword(), request.getUserEmailAddress(), request.getUserFullName()))
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode(), StatusMessage.BAD_REQUEST_MISSING_FIELDS.name());
			return false;
		}

		Locale locale = request.getJavaLocale();

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			boolean userExists = context.fetchExists(USERS, USERS.USERNAME.eq(request.getUserUsername())
																		  .or(USERS.EMAIL_ADDRESS.eq(request.getUserEmailAddress())));
			boolean requestExists = context.fetchExists(UNAPPROVED_USERS, UNAPPROVED_USERS.HAS_BEEN_REJECTED.eq((byte) 0)
																											.and(UNAPPROVED_USERS.USER_USERNAME.eq(request.getUserUsername())
																																			   .or(UNAPPROVED_USERS.USER_EMAIL_ADDRESS.eq(request.getUserEmailAddress()))));

			if (userExists || requestExists)
			{
				resp.sendError(Response.Status.CONFLICT.getStatusCode(), StatusMessage.CONFLICT_USERNAME_EMAIL_ALREADY_IN_USE.name());
				return false;
			}

			request.setUserPassword(BCrypt.hashpw(request.getUserPassword(), BCrypt.gensalt(TokenResource.SALT)));
			UnapprovedUsers newUser = request.getUnapprovedUser();

			UnapprovedUsersRecord record = context.newRecord(UNAPPROVED_USERS, newUser);

			String url = PropertyWatcher.get(ServerProperty.WEB_BASE);
			String uuid = UUID.randomUUID().toString();

			record.setActivationKey(uuid);
			record.setCreatedOn(new Timestamp(System.currentTimeMillis()));

			if (!url.endsWith("/"))
				url += "/";

			url += "#/gk/activation?activationKey=" + uuid;

			record.store();

			Email.sendActivationPrompt(locale, record, url);

			return true;
		}
		catch (EmailException e)
		{
			e.printStackTrace();
			resp.sendError(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), StatusMessage.UNAVAILABLE_EMAIL.name());
			return false;
		}
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<ViewUnapprovedUserDetails>> getNewRequests()
		throws IOException, SQLException
	{
		return this.getNewRequestById(null);
	}

	@GET
	@Path("requestId")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<ViewUnapprovedUserDetails>> getNewRequestById(@PathParam("requestId") Integer requestId)
		throws IOException, SQLException
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			SelectConditionStep<Record> step = context.select().hint("SQL_CALC_FOUND_ROWS").from(VIEW_UNAPPROVED_USER_DETAILS)
													  .where(VIEW_UNAPPROVED_USER_DETAILS.HAS_BEEN_REJECTED.eq((byte) 0));

			if (requestId != null)
				step.and(VIEW_UNAPPROVED_USER_DETAILS.ID.eq(requestId));

			if (query != null)
				step.and(VIEW_UNAPPROVED_USER_DETAILS.USERNAME.contains(query)
															  .or(VIEW_UNAPPROVED_USER_DETAILS.DATABASE_SERVER_NAME.contains(query))
															  .or(VIEW_UNAPPROVED_USER_DETAILS.DATABASE_SYSTEM_NAME.contains(query))
															  .or(VIEW_UNAPPROVED_USER_DETAILS.EMAIL_ADDRESS.contains(query))
															  .or(VIEW_UNAPPROVED_USER_DETAILS.NAME.contains(query))
															  .or(VIEW_UNAPPROVED_USER_DETAILS.ACRONYM.contains(query))
															  .or(VIEW_UNAPPROVED_USER_DETAILS.ADDRESS.contains(query))
															  .or(VIEW_UNAPPROVED_USER_DETAILS.FULL_NAME.contains(query)));

			List<ViewUnapprovedUserDetails> result = setPaginationAndOrderBy(step)
				.fetch()
				.into(ViewUnapprovedUserDetails.class);

			Integer count = context.fetchOne("SELECT FOUND_ROWS()").into(Integer.class);

			return new PaginatedResult<>(result, count);
		}
	}
}

package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.*;

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
import static jhi.gatekeeper.server.database.tables.ViewAccessRequestUserDetails.*;

/**
 * @author Sebastian Raubach
 */
public class ExistingRequestResource extends ServerResource
{
	private Integer id;

	@Override
	public void doInit()
	{
		super.doInit();

		try
		{
			this.id = Integer.parseInt(getRequestAttributes().get("requestId").toString());
		}
		catch (NullPointerException | NumberFormatException e)
		{
		}
	}

	@OnlyAdmin
	@Delete("json")
	public boolean deleteJson()
	{
		if (id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID.name());

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			return context.deleteFrom(ACCESS_REQUESTS)
						  .where(ACCESS_REQUESTS.ID.eq(id))
						  .execute() > 0;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@OnlyAdmin
	@Post("json")
	public boolean postJson(NewAccessRequest request)
	{
		if (request == null || request.getUserId() == null || request.getDatabaseSystemId() == null)
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, StatusMessage.NOT_FOUND_PAYLOAD.name());

		Locale locale = request.getJavaLocale();

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			// Get the user and the database
			Users user = context.selectFrom(USERS)
								.where(USERS.ID.eq(request.getUserId()))
								.fetchOneInto(Users.class);
			DatabaseSystems database = context.selectFrom(DATABASE_SYSTEMS)
											  .where(DATABASE_SYSTEMS.ID.eq(request.getDatabaseSystemId()))
											  .fetchOneInto(DatabaseSystems.class);

			// If either are null, fail
			if (user == null || database == null)
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, StatusMessage.NOT_FOUND_USER.name());

			boolean alreadyHasAccess = context.fetchExists(USER_HAS_ACCESS_TO_DATABASES, USER_HAS_ACCESS_TO_DATABASES.USER_ID.eq(user.getId())
																															 .and(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID.eq(database.getId())));
			boolean alreadyRequested = context.fetchExists(ACCESS_REQUESTS, ACCESS_REQUESTS.USER_ID.eq(user.getId())
																								   .and(ACCESS_REQUESTS.DATABASE_SYSTEM_ID.eq(database.getId()))
																								   .and(ACCESS_REQUESTS.HAS_BEEN_REJECTED.eq((byte) 0)));

			if (alreadyHasAccess)
				throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, StatusMessage.CONFLICT_USER_ALREADY_HAS_ACCESS.name());
			if (alreadyRequested)
				throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, StatusMessage.CONFLICT_USER_ALREADY_REQUESTED_ACCESS.name());

			if (request.getNeedsApproval() == 1)
			{
				// If it needs approval, store it in the database for now, then send emails
				int result = context.newRecord(ACCESS_REQUESTS, request).store();
				Email.sendAwaitingApproval(locale, user);
				Email.sendAdministratorNotification(locale, database, true);
				return result > 0;
			}
			else
			{
				// If it doesn't need approval, make the decision straightaway.
				AccessRequestsRecord record = context.newRecord(ACCESS_REQUESTS, request.getAccessRequest());
				record.store();
				RequestDecision decision = new RequestDecision(record.getId(), Decision.APPROVE, null);
				decision.setJavaLocale(locale);
				ExistingRequestDecisionResource.decide(record.getId(), decision);
				Email.sendAdministratorNotification(locale, database, false);
				return true;
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
		catch (EmailException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, StatusMessage.UNAVAILABLE_EMAIL.name());
		}
	}

	@OnlyAdmin
	@Get("json")
	public List<ViewAccessRequestUserDetails> getJson()
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			SelectWhereStep<ViewAccessRequestUserDetailsRecord> step = context.selectFrom(VIEW_ACCESS_REQUEST_USER_DETAILS);

			if (id != null)
				step.where(VIEW_ACCESS_REQUEST_USER_DETAILS.ID.eq(id));

			return step.orderBy(VIEW_ACCESS_REQUEST_USER_DETAILS.CREATED_ON)
					   .fetch()
					   .into(ViewAccessRequestUserDetails.class);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

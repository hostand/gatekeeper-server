package jhi.gatekeeper.server.resource;

import org.jooq.DSLContext;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.Objects;

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
public class ExistingRequestDecisionResource extends ServerResource
{
	private Integer id;

	public static boolean decide(Integer id, RequestDecision request)
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			// Get the request with the given id
			AccessRequestsRecord accessRequest = context.selectFrom(ACCESS_REQUESTS)
														.where(ACCESS_REQUESTS.ID.eq(id))
														.fetchOneInto(ACCESS_REQUESTS);

			if (accessRequest != null)
			{
				// Get user and database
				Users user = context.selectFrom(USERS)
									.where(USERS.ID.eq(accessRequest.getUserId()))
									.fetchOneInto(Users.class);
				DatabaseSystems database = context.selectFrom(DATABASE_SYSTEMS)
												  .where(DATABASE_SYSTEMS.ID.eq(accessRequest.getDatabaseSystemId()))
												  .fetchOneInto(DatabaseSystems.class);

				// If it's been rejected, update database,
				switch (request.getDecision())
				{
					case REJECT:
						// Set rejected flag
						accessRequest.setHasBeenRejected((byte) 1);
						accessRequest.store();

						// Send an email letting the user know
						Email.sendAccessRequestRejected(request.getJavaLocale(), user, request.getFeedback());
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
						Email.sendAccessRequestApproved(request.getJavaLocale(), user, database);
						break;
				}
				return true;
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
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
	@Post("json")
	public boolean postJson(RequestDecision request)
	{
		if (id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID.name());
		if (request == null || !Objects.equals(request.getRequestId(), id))
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);

		return decide(id, request);
	}
}

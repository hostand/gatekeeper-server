package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.exception.*;
import jhi.gatekeeper.server.util.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UnapprovedUsers.*;
import static jhi.gatekeeper.server.database.tables.ViewUnapprovedUserDetails.*;

/**
 * @author Sebastian Raubach
 */
public class NewRequestResource extends ServerResource
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

	@Delete("json")
	public boolean deleteJson()
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);
		if (id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			return context.deleteFrom(UNAPPROVED_USERS)
						  .where(UNAPPROVED_USERS.ID.eq(id))
						  .execute() > 0;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@Post("json")
	public boolean postJson(NewUnapprovedUser request)
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);
		if (request == null || request.getDatabaseSystemId() == null
			|| StringUtils.isEmpty(request.getUserUsername(), request.getUserPassword(), request.getUserEmailAddress(), request.getUserFullName()))
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);

		Locale locale = request.getJavaLocale();

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			DatabaseSystems database = context.selectFrom(DATABASE_SYSTEMS)
											  .where(DATABASE_SYSTEMS.ID.eq(request.getDatabaseSystemId()))
											  .fetchOneInto(DatabaseSystems.class);

			request.setUserPassword(BCrypt.hashpw(request.getUserPassword(), BCrypt.gensalt(TokenResource.SALT)));
			if (request.getNeedsApproval() == 1)
			{
				// If it needs approval, add to database, notify everyone
				UnapprovedUsers newUser = request.getUnapprovedUser();
				Email.sendAwaitingApproval(locale, newUser);
				Email.sendAdministratorNotification(locale, database, true);
				return context.newRecord(UNAPPROVED_USERS, newUser).store() > 0;
			}
			else
			{
				// If it doesn't need approval, make the decision straightaway.
				UnapprovedUsersRecord record = context.newRecord(UNAPPROVED_USERS, request.getUnapprovedUser());
				record.store();
				RequestDecision decision = new RequestDecision(record.getId(), Decision.APPROVE, null);
				decision.setJavaLocale(locale);
				NewRequestDecisionResource.decide(record.getId(), decision);
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
			throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
		}
	}

	@Get("json")
	public List<ViewUnapprovedUserDetails> getJson()
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			SelectWhereStep<ViewUnapprovedUserDetailsRecord> step = context.selectFrom(VIEW_UNAPPROVED_USER_DETAILS);

			if (id != null)
				step.where(VIEW_UNAPPROVED_USER_DETAILS.ID.eq(id));

			return step.orderBy(VIEW_UNAPPROVED_USER_DETAILS.CREATED_ON)
					   .fetch()
					   .into(ViewUnapprovedUserDetails.class);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

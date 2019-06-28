package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;

import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.*;

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
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
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
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

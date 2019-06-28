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

import static jhi.gatekeeper.server.database.tables.AccessRequests.*;
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
			return context.deleteFrom(ACCESS_REQUESTS)
				.where(ACCESS_REQUESTS.ID.eq(id))
				.execute() > 0;
		}
		catch (SQLException e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@Get("json")
	public List<ViewAccessRequestUserDetails> getJson()
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
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
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

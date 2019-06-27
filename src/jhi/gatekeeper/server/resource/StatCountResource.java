package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;

import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;

import static jhi.gatekeeper.server.database.tables.ViewCounts.*;

/**
 * @author Sebastian Raubach
 */
public class StatCountResource extends PaginatedServerResource
{
	@Get("json")
	public ViewCounts getJson()
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);
		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			return context.selectFrom(VIEW_COUNTS)
				   .fetchOne()
				   .into(ViewCounts.class);
		}
		catch (SQLException e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

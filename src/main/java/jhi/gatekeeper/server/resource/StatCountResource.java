package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;

import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.ViewCounts;
import jhi.gatekeeper.server.util.OnlyAdmin;

import static jhi.gatekeeper.server.database.tables.ViewCounts.*;

/**
 * @author Sebastian Raubach
 */
public class StatCountResource extends PaginatedServerResource
{
	@OnlyAdmin
	@Get("json")
	public ViewCounts getJson()
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			return context.selectFrom(VIEW_COUNTS)
						  .fetchOne()
						  .into(ViewCounts.class);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

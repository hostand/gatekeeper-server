package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jhi.gatekeeper.resource.UserType;
import jhi.gatekeeper.server.util.*;
import org.jooq.DSLContext;

import java.sql.*;

import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.ViewCounts;

import static jhi.gatekeeper.server.database.tables.ViewCounts.*;

/**
 * @author Sebastian Raubach
 */
@Path("stat/count")
@Secured(UserType.ADMIN)
public class StatCountResource extends PaginatedServerResource
{
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ViewCounts getStatsCount()
		throws SQLException
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			return context.selectFrom(VIEW_COUNTS)
						  .fetchAnyInto(ViewCounts.class);
		}
	}
}

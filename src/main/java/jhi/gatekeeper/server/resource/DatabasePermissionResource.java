package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.ViewUserPermissions;
import jhi.gatekeeper.server.util.*;
import org.jooq.*;
import org.jooq.Record;

import java.io.IOException;
import java.sql.*;
import java.util.List;

import static jhi.gatekeeper.server.database.tables.ViewUserPermissions.*;

/**
 * @author Sebastian Raubach
 */
@Path("database/{databaseId}/permission")
@Secured(UserType.ADMIN)
public class DatabasePermissionResource extends PaginatedServerResource
{
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<ViewUserPermissions>> getDatabaseUserPermissions(@PathParam("databaseId") Integer databaseId, @QueryParam("username") String username)
		throws IOException, SQLException
	{
		if (databaseId == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID.name());
			return null;
		}

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			SelectWhereStep<Record> step = context.select()
												  .hint("SQL_CALC_FOUND_ROWS")
												  .from(VIEW_USER_PERMISSIONS);

			step.where(VIEW_USER_PERMISSIONS.DATABASE_ID.eq(databaseId));

			if (query != null && !"".equals(query))
			{
				query = "%" + query + "%";
				step.where(VIEW_USER_PERMISSIONS.USERNAME.like(query)
														 .or(VIEW_USER_PERMISSIONS.USER_TYPE.like(query)));
			}
			else if (username != null)
			{
				if (!StringUtils.isEmpty(username))
					step.where(VIEW_USER_PERMISSIONS.USERNAME.eq(username));
			}

			List<ViewUserPermissions> result = setPaginationAndOrderBy(step)
				.fetch()
				.into(ViewUserPermissions.class);

			Integer count = context.fetchOne("SELECT FOUND_ROWS()").into(Integer.class);

			return new PaginatedResult<>(result, count);
		}
	}
}

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
import java.util.*;

import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.ViewUserPermissions.*;

/**
 * @author Sebastian Raubach
 */
@Path("user/{userId}/permission")
@Secured(UserType.ADMIN)
public class UserPermissionResource extends PaginatedServerResource
{
	@PathParam("userId")
	Integer userId;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean pstUserPermission(ViewUserPermissions permission)
		throws IOException, SQLException
	{
		if (permission == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_PAYLOAD.name());
			return false;
		}

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			int result = context.insertInto(USER_HAS_ACCESS_TO_DATABASES)
								.set(USER_HAS_ACCESS_TO_DATABASES.USER_ID, permission.getUserId())
								.set(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID, permission.getDatabaseId())
								.set(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID, permission.getUserTypeId())
								.set(USER_HAS_ACCESS_TO_DATABASES.PRIMARY_CONTACT, permission.getUserIsPrimaryContact() == ((byte)1) ? permission.getUserIsPrimaryContact() : (byte) 0)
								.execute();

			Email.initPrimaryContactMap(Collections.singletonList(permission.getDatabaseId()));

			return result > 0;
		}
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<ViewUserPermissions>> getUserPermissions(@QueryParam("databaseName") String databaseName, @QueryParam("databaseServer") String databaseServer)
		throws IOException, SQLException
	{
		if (userId == null)
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

			step.where(VIEW_USER_PERMISSIONS.USER_ID.eq(userId));

			if (query != null && !"".equals(query))
			{
				query = "%" + query + "%";
				step.where(VIEW_USER_PERMISSIONS.SERVER_NAME.like(query)
															.or(VIEW_USER_PERMISSIONS.SYSTEM_NAME.like(query))
															.or(VIEW_USER_PERMISSIONS.USER_TYPE.like(query)));
			}
			else if (databaseName != null || databaseServer != null)
			{
				if (!StringUtils.isEmpty(databaseServer))
					step.where(VIEW_USER_PERMISSIONS.SERVER_NAME.eq(databaseServer));
				if (!StringUtils.isEmpty(databaseName))
					step.where(VIEW_USER_PERMISSIONS.SYSTEM_NAME.eq(databaseName));
			}

			List<ViewUserPermissions> result = setPaginationAndOrderBy(step)
				.fetch()
				.into(ViewUserPermissions.class);

			Integer count = context.fetchOne("SELECT FOUND_ROWS()").into(Integer.class);

			return new PaginatedResult<>(result, count);
		}
	}

	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean deleteUserPermission(ViewUserPermissions permission)
		throws IOException, SQLException
	{
		if (permission == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_PAYLOAD.name());
			return false;
		}

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			int result = context.deleteFrom(USER_HAS_ACCESS_TO_DATABASES)
								.where(USER_HAS_ACCESS_TO_DATABASES.USER_ID.eq(permission.getUserId()))
								.and(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID.eq(permission.getDatabaseId()))
								.and(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID.eq(permission.getUserTypeId()))
								.execute();

			Email.initPrimaryContactMap(Collections.singletonList(permission.getDatabaseId()));

			return result > 0;
		}
	}

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean patchUserPermission(ViewUserPermissions permission)
		throws IOException, SQLException
	{
		if (permission == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_PAYLOAD.name());
			return false;
		}

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			int result = context.update(USER_HAS_ACCESS_TO_DATABASES)
								.set(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID, permission.getUserTypeId())
								.set(USER_HAS_ACCESS_TO_DATABASES.PRIMARY_CONTACT, permission.getUserIsPrimaryContact())
								.where(USER_HAS_ACCESS_TO_DATABASES.USER_ID.eq(permission.getUserId())
																		   .and(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID.eq(permission.getDatabaseId())))
								.execute();

			Email.initPrimaryContactMap(Collections.singletonList(permission.getDatabaseId()));

			return result > 0;
		}
	}
}

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
import jhi.gatekeeper.server.util.*;

import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.ViewUserPermissions.*;

/**
 * @author Sebastian Raubach
 */
public class UserPermissionResource extends PaginatedServerResource
{
	public static final String PARAM_DATABASE_SERVER = "databaseServer";
	public static final String PARAM_DATABASE_NAME   = "databaseName";

	private Integer id = null;
	private String  databaseServer;
	private String  databaseName;

	@Override
	public void doInit()
	{
		super.doInit();

		try
		{
			this.id = Integer.parseInt(getRequestAttributes().get("userId").toString());
		}
		catch (NullPointerException | NumberFormatException e)
		{
		}
		this.databaseServer = getQueryValue(PARAM_DATABASE_SERVER);
		this.databaseName = getQueryValue(PARAM_DATABASE_NAME);
	}

	@Post("json")
	public boolean postJson(ViewUserPermissions permission)
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS);
		if (permission == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_PAYLOAD);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			int result = context.insertInto(USER_HAS_ACCESS_TO_DATABASES)
								.set(USER_HAS_ACCESS_TO_DATABASES.USER_ID, permission.getUserId())
								.set(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID, permission.getDatabaseId())
								.set(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID, permission.getUserTypeId())
								.execute();

			return result > 0;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@OnlyAdmin
	@Get("json")
	public PaginatedResult<List<ViewUserPermissions>> getJson()
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS);
		if (id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			SelectWhereStep<Record> step = context.select()
												  .hint("SQL_CALC_FOUND_ROWS")
												  .from(VIEW_USER_PERMISSIONS);

			step.where(VIEW_USER_PERMISSIONS.USER_ID.eq(id));

			if (query != null && !"".equals(query))
			{
				query = "%" + query + "%";
				step.where(VIEW_USER_PERMISSIONS.SERVER_NAME.like(query))
					.or(VIEW_USER_PERMISSIONS.SYSTEM_NAME.like(query))
					.or(VIEW_USER_PERMISSIONS.USER_TYPE.like(query));
			}
			else if (databaseName != null || databaseServer != null)
			{
				if (!StringUtils.isEmpty(databaseServer))
					step.where(VIEW_USER_PERMISSIONS.SERVER_NAME.eq(databaseServer));
				if (!StringUtils.isEmpty(databaseName))
					step.where(VIEW_USER_PERMISSIONS.SYSTEM_NAME.eq(databaseName));
			}

			if (ascending != null && orderBy != null)
			{
				// Camelcase to underscore
				orderBy = orderBy.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();

				if (ascending)
					step.orderBy(DSL.field(orderBy).asc());
				else
					step.orderBy(DSL.field(orderBy).desc());
			}

			List<ViewUserPermissions> result = step.limit(pageSize)
												   .offset(pageSize * currentPage)
												   .fetch()
												   .into(ViewUserPermissions.class);

			Integer count = context.fetchOne("SELECT FOUND_ROWS()").into(Integer.class);

			return new PaginatedResult<>(result, count);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@Delete("json")
	public boolean deleteJson(ViewUserPermissions permission)
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS);
		if (permission == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_PAYLOAD);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			int result = context.deleteFrom(USER_HAS_ACCESS_TO_DATABASES)
								.where(USER_HAS_ACCESS_TO_DATABASES.USER_ID.eq(permission.getUserId()))
								.and(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID.eq(permission.getDatabaseId()))
								.and(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID.eq(permission.getUserTypeId()))
								.execute();

			return result > 0;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@Patch("json")
	public boolean patchJson(ViewUserPermissions permission)
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS);
		if (permission == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_PAYLOAD);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			int result = context.update(USER_HAS_ACCESS_TO_DATABASES)
								.set(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID, permission.getUserTypeId())
								.where(USER_HAS_ACCESS_TO_DATABASES.USER_ID.eq(permission.getUserId())
																		   .and(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID.eq(permission.getDatabaseId())))
								.execute();

			return result > 0;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

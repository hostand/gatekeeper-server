package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.List;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.ViewUserPermissions;
import jhi.gatekeeper.server.util.*;

import static jhi.gatekeeper.server.database.tables.ViewUserPermissions.*;

/**
 * @author Sebastian Raubach
 */
public class DatabasePermissionResource extends PaginatedServerResource
{
	public static final String PARAM_DATABASE_USERNAME = "username";

	private Integer id = null;
	private String  username;

	@Override
	public void doInit()
	{
		super.doInit();

		try
		{
			this.id = Integer.parseInt(getRequestAttributes().get("databaseId").toString());
		}
		catch (NullPointerException | NumberFormatException e)
		{
		}
		this.username = getQueryValue(PARAM_DATABASE_USERNAME);
	}

	@OnlyAdmin
	@Get("json")
	public PaginatedResult<List<ViewUserPermissions>> getJson()
	{
		if (id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID.name());

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			SelectWhereStep<Record> step = context.select()
												  .hint("SQL_CALC_FOUND_ROWS")
												  .from(VIEW_USER_PERMISSIONS);

			step.where(VIEW_USER_PERMISSIONS.DATABASE_ID.eq(id));

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

			if (ascending != null && orderBy != null)
			{
				// Camelcase to underscore
				orderBy = orderBy.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();

				if (ascending)
					step.orderBy(DSL.field(getSafeColumn(orderBy)).asc());
				else
					step.orderBy(DSL.field(getSafeColumn(orderBy)).desc());
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
}

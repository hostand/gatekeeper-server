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

import static jhi.gatekeeper.server.database.tables.Users.*;
import static jhi.gatekeeper.server.database.tables.ViewUserDetails.*;

/**
 * @author Sebastian Raubach
 */
public class UserResource extends PaginatedServerResource
{
	private Integer id = null;

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
			int result = context.deleteFrom(USERS)
								.where(USERS.ID.eq(id))
								.execute();

			return result > 0;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@Get("json")
	public PaginatedResult<List<ViewUserDetails>> getJson()
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			CustomVerifier.UserDetails sessionUser = CustomVerifier.getFromSession(getRequest());

			SelectWhereStep<Record> step = context.select()
												  .hint("SQL_CALC_FOUND_ROWS")
												  .from(VIEW_USER_DETAILS);

			if (id != null)
			{
				if (!Objects.equals(id, sessionUser.getId()))
					throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
				else
					step.where(VIEW_USER_DETAILS.ID.eq(id));
			}
			else
			{
				if (!CustomVerifier.isAdmin(getRequest()))
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);

				if (query != null && !"".equals(query))
				{
					query = "%" + query + "%";
					step.where(VIEW_USER_DETAILS.USERNAME.like(query)
														 .or(VIEW_USER_DETAILS.ADDRESS.like(query))
														 .or(VIEW_USER_DETAILS.EMAIL_ADDRESS.like(query))
														 .or(VIEW_USER_DETAILS.NAME.like(query))
														 .or(VIEW_USER_DETAILS.FULL_NAME.like(query))
														 .or(VIEW_USER_DETAILS.ACRONYM.like(query)));
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
			}

			List<ViewUserDetails> result = step.limit(pageSize)
											   .offset(pageSize * currentPage)
											   .fetch()
											   .into(ViewUserDetails.class);

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

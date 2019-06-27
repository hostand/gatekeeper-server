package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;

import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.records.*;

import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
public class UserGatekeeperResource extends PaginatedServerResource
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

	@Patch("json")
	public boolean postJson(Byte update)
	{
		if (update == null || id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			Optional<UsersRecord> optional = context.selectFrom(USERS)
													.where(USERS.ID.eq(id))
													.fetchOptional();

			if (optional.isPresent())
			{
				UsersRecord user = optional.get();

				context.update(USERS)
					   .set(USERS.HAS_ACCESS_TO_GATEKEEPER, update)
					   .where(USERS.ID.eq(user.getId()))
					   .execute();

				return true;
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
			}
		}
		catch (SQLException e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

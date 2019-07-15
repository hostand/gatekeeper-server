package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;

import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.util.*;

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

	@OnlyAdmin
	@Patch("json")
	public boolean postJson(Byte update)
	{
		if (update == null || id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID_OR_PAYLOAD);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			Users user = context.selectFrom(USERS)
								.where(USERS.ID.eq(id))
								.fetchOneInto(Users.class);

			if (user == null)
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_USER);

			context.update(USERS)
				   .set(USERS.HAS_ACCESS_TO_GATEKEEPER, update)
				   .where(USERS.ID.eq(user.getId()))
				   .execute();

			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

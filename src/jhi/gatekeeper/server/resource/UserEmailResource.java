package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;

import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
public class UserEmailResource extends PaginatedServerResource
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
	public boolean postJson(EmailUpdate update)
	{
		if (update == null || id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);

		CustomVerifier.UserDetails sessionUser = CustomVerifier.getFromSession(getRequest());

		if (sessionUser == null || !Objects.equals(sessionUser.getId(), id))
			throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			Users user = context.selectFrom(USERS)
								.where(USERS.ID.eq(sessionUser.getId()))
								.fetchOneInto(Users.class);

			if (user != null)
			{
				if (Objects.equals(user.getEmailAddress(), update.getOldEmail()))
				{
					context.update(USERS)
						   .set(USERS.EMAIL_ADDRESS, update.getNewEmail())
						   .where(USERS.ID.eq(user.getId()))
						   .execute();

					return true;
				}
				else
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
				}
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.Objects;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.auth.CustomVerifier;
import jhi.gatekeeper.server.database.tables.pojos.Users;

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
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID_OR_PAYLOAD.name());

		CustomVerifier.UserDetails sessionUser = CustomVerifier.getFromSession(getRequest(), getResponse());

		if (sessionUser == null || !Objects.equals(sessionUser.getId(), id))
			throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			Users user = context.selectFrom(USERS)
								.where(USERS.ID.eq(sessionUser.getId()))
								.fetchOneInto(Users.class);

			if (user == null)
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_USER.name());

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
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

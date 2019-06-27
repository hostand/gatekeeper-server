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
import jhi.gatekeeper.server.database.tables.records.*;

import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
public class UserPasswordResource extends PaginatedServerResource
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
	public boolean postJson(PasswordUpdate update)
	{
		if (update == null || id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);

		CustomVerifier.UserDetails sessionUser = CustomVerifier.getFromSession(getRequest());

		if (sessionUser == null || !Objects.equals(sessionUser.getId(), id))
			throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			Optional<UsersRecord> optional = context.selectFrom(USERS)
													.where(USERS.ID.eq(sessionUser.getId()))
													.fetchOptional();

			if (optional.isPresent())
			{
				UsersRecord user = optional.get();

				// Check if they are the same
				boolean same = BCrypt.checkpw(update.getOldPassword(), user.getPassword());

				if (same)
				{
					// Update the password
					String saltedPassword = BCrypt.hashpw(update.getNewPassword(), BCrypt.gensalt(TokenResource.SALT));
					context.update(USERS)
						   .set(USERS.PASSWORD, saltedPassword)
						   .where(USERS.ID.eq(user.getId()))
						   .execute();

					// Terminate this "session".
					CustomVerifier.removeToken(getRequest());

					return true;
				}
				else
				{
					throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
				}
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

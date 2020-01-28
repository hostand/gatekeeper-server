package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.Objects;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.Users;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.Email;

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
				CustomVerifier.removeToken(CustomVerifier.getFromSession(getRequest(), getResponse()).getToken(), getRequest(), getResponse());

				if (!user.getUsername().equals("admin"))
					Email.sendPasswordChangeInfo(update.getJavaLocale(), user);

				return true;
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_ACCESS_TO_OTHER_USER.name());
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
		catch (EmailException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, StatusMessage.UNAVAILABLE_EMAIL.name());
		}
	}
}

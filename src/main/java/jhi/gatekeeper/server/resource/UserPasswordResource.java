package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.BCrypt;
import jhi.gatekeeper.server.database.tables.pojos.Users;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.*;
import jhi.gatekeeper.server.util.watcher.PropertyWatcher;
import org.jooq.DSLContext;

import java.io.IOException;
import java.sql.*;
import java.util.Objects;

import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
@Path("user/{userId}/password")
@Secured
public class UserPasswordResource extends PaginatedServerResource
{
	@PathParam("userId")
	Integer userId;

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean patchPassword(PasswordUpdate update)
		throws IOException, SQLException
	{
		if (update == null || userId == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID_OR_PAYLOAD.name());
			return false;
		}

		AuthenticationFilter.UserDetails sessionUser = (AuthenticationFilter.UserDetails) securityContext.getUserPrincipal();

		if (sessionUser == null || !Objects.equals(sessionUser.getId(), userId))
		{
			resp.sendError(Response.Status.UNAUTHORIZED.getStatusCode());
			return false;
		}

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			Users user = context.selectFrom(USERS)
								.where(USERS.ID.eq(sessionUser.getId()))
								.fetchAnyInto(Users.class);

			if (user == null)
			{
				resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_USER.name());
				return false;
			}

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
				AuthenticationFilter.removeToken(sessionUser.getToken(), req, resp);

				if (!user.getUsername().equals("admin"))
				{
					Email.sendPasswordChangeInfo(update.getJavaLocale(), user);
				} else {
					// Remember the changed admin password in the config file for easier lookup if required.
					PropertyWatcher.set(ServerProperty.GENERAL_ADMIN_PASSWORD, update.getNewPassword());
					PropertyWatcher.storeProperties();
				}

				return true;
			}
			else
			{
				resp.sendError(Response.Status.FORBIDDEN.getStatusCode(), StatusMessage.FORBIDDEN_ACCESS_TO_OTHER_USER.name());
				return false;
			}
		}
		catch (EmailException e)
		{
			e.printStackTrace();
			resp.sendError(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
			return false;
		}
	}
}

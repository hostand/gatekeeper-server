package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.database.tables.pojos.Users;
import jhi.gatekeeper.server.util.Secured;
import org.jooq.DSLContext;

import java.io.IOException;
import java.sql.*;
import java.util.Objects;

import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
@Path("user/{userId}/email")
@Secured
public class UserEmailResource extends PaginatedServerResource
{
	@PathParam("userId")
	Integer userId;

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean patchEmail(EmailUpdate update)
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

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			Users user = context.selectFrom(USERS)
								.where(USERS.ID.eq(sessionUser.getId()))
								.fetchAnyInto(Users.class);

			if (user == null)
			{
				resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_USER.name());
				return false;
			}

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
				resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
				return false;
			}
		}
	}
}

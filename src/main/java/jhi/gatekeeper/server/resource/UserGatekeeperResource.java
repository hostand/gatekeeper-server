package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.util.*;
import org.jooq.DSLContext;

import java.io.IOException;
import java.sql.*;

import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.Users;

import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
@Path("user/{userId}/gatekeeper")
@Secured(UserType.ADMIN)
public class UserGatekeeperResource extends PaginatedServerResource
{
	@PathParam("userId")
	Integer userId;

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean patchGatekeeperAccess(Byte update)
		throws IOException, SQLException
	{
		if (update == null || userId == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID_OR_PAYLOAD.name());
			return false;
		}

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			Users user = context.selectFrom(USERS)
								.where(USERS.ID.eq(userId))
								.fetchAnyInto(Users.class);

			if (user == null)
			{
				resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_USER.name());
				return false;
			}

			context.update(USERS)
				   .set(USERS.HAS_ACCESS_TO_GATEKEEPER, update)
				   .where(USERS.ID.eq(user.getId()))
				   .execute();

			return true;
		}
	}
}

/*
 * Copyright 2018 Information & Computational Sciences, The James Hutton Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.database.tables.records.UsersRecord;
import jhi.gatekeeper.server.util.Secured;
import org.jooq.DSLContext;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.UserTypes.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * {@link ContextResource} handling {@link TokenResource} requests.
 *
 * @author Sebastian Raubach
 */
@Path("token")
public class TokenResource extends ContextResource
{
	public static Integer SALT = 10;

	@DELETE
	@Secured
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean deleteToken(Users user)
		throws IOException
	{
		if (user == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_TOKEN.name());
			return false;
		}

		AuthenticationFilter.UserDetails sessionUser = (AuthenticationFilter.UserDetails) securityContext.getUserPrincipal();

		if (sessionUser == null || !Objects.equals(sessionUser.getToken(), user.getPassword()))
		{
			resp.sendError(Response.Status.FORBIDDEN.getStatusCode(), StatusMessage.FORBIDDEN_ACCESS_TO_OTHER_USER.name());
			return false;
		}

		try
		{
			// Try and see if it's a valid UUID
			UUID.fromString(user.getPassword());
			AuthenticationFilter.removeToken(user.getPassword(), req, resp);
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Token postToken(Users request)
		throws IOException, SQLException
	{
		boolean canAccess;
		String token;
		UsersRecord user;
		UserTypes type;
		String userType = "Unknown";

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			user = context.selectFrom(USERS)
						  .where(USERS.USERNAME.eq(request.getUsername()))
						  .fetchAny();

			if (user != null)
			{
				canAccess = BCrypt.checkpw(request.getPassword(), user.getPassword());

				type = context.select(USER_TYPES.ID, USER_TYPES.DESCRIPTION)
							  .from(USER_TYPES)
							  .leftJoin(USER_HAS_ACCESS_TO_DATABASES).on(USER_TYPES.ID.eq(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID))
							  .leftJoin(DATABASE_SYSTEMS).on(DATABASE_SYSTEMS.ID.eq(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID))
							  .leftJoin(USERS).on(USERS.ID.eq(USER_HAS_ACCESS_TO_DATABASES.USER_ID))
							  .where(USERS.ID.eq(user.getId()))
							  .and(DATABASE_SYSTEMS.SYSTEM_NAME.eq("gatekeeper"))
							  .and(DATABASE_SYSTEMS.SERVER_NAME.eq("--"))
							  .and(USERS.HAS_ACCESS_TO_GATEKEEPER.eq((byte) 1))
							  .fetchOptionalInto(UserTypes.class)
							  .orElse(null);

				if (type != null)
					userType = type.getDescription();

				if (canAccess)
				{
					// Keep track of this last login event
					user.setLastLogin(new Timestamp(System.currentTimeMillis()));
					user.store(USERS.LAST_LOGIN);
				}
			}
			else
			{
				Logger.getLogger("").log(Level.SEVERE, "User not found: " + request.getUsername());
				resp.sendError(Response.Status.BAD_REQUEST.getStatusCode(), StatusMessage.FORBIDDEN_INVALID_CREDENTIALS.name());
				return null;
			}
		}

		if (canAccess)
		{
			token = UUID.randomUUID().toString();
			AuthenticationFilter.addToken(this.req, this.resp, token, userType, user.getId());

			// The salt may have changed since the last time, so update the password in the database with the new salt.
			String saltedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt(SALT));

			try (Connection conn = Database.getConnection();
				 DSLContext context = Database.getContext(conn))
			{
				context.update(USERS)
					   .set(USERS.PASSWORD, saltedPassword)
					   .where(USERS.ID.eq(user.getId()))
					   .execute();
			}
			catch (SQLException e)
			{
				Logger.getLogger("").info(e.getMessage());
				e.printStackTrace();
			}
		}
		else
		{
			resp.sendError(Response.Status.FORBIDDEN.getStatusCode(), StatusMessage.FORBIDDEN_INVALID_CREDENTIALS.name());
			return null;
		}

		return new Token(token, user.getId(), user.getUsername(), user.getFullName(), user.getEmailAddress(), type, AuthenticationFilter.AGE, System.currentTimeMillis());
	}
}

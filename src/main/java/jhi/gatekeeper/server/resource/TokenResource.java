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

import jhi.gatekeeper.server.database.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;
import java.util.logging.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.UserTypes.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * {@link ServerResource} handling {@link TokenResource} requests.
 *
 * @author Sebastian Raubach
 */
public class TokenResource extends ServerResource
{
	public static Integer SALT = 10;

	@Delete("json")
	public boolean deleteJson(Users user)
	{
		if (user == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_TOKEN.name());

		CustomVerifier.UserDetails sessionUser = CustomVerifier.getFromSession(getRequest(), getResponse());

		if (sessionUser == null || !Objects.equals(sessionUser.getToken(), user.getPassword()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_ACCESS_TO_OTHER_USER.name());

		try
		{
			// Try and see if it's a valid UUID
			UUID.fromString(user.getPassword());
			return CustomVerifier.removeToken(user.getPassword(), getRequest(), getResponse());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Post("json")
	public Token postJson(Users request)
	{
		boolean canAccess;
		String token;
		UsersRecord user;
		UserTypes type;

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
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, StatusMessage.FORBIDDEN_INVALID_CREDENTIALS.name());
			}
		}
		catch (SQLException e)
		{
			Logger.getLogger("").info(e.getMessage());
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}

		if (canAccess)
		{
			token = UUID.randomUUID().toString();
			CustomVerifier.addToken(getRequest(), getResponse(), token, user.getId());

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
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INVALID_CREDENTIALS.name());
		}

		return new Token(token, user.getId(), user.getUsername(), user.getFullName(), user.getEmailAddress(), type, CustomVerifier.AGE, System.currentTimeMillis());
	}
}

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

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;

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
	public boolean deleteJson(Users request)
	{
		if (request.getPassword() != null)
		{
			try
			{
				// Try and see if it's a valid UUID
				UUID.fromString(request.getPassword());
				return CustomVerifier.removeToken(request.getPassword());
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}

		return false;
	}

	@Post("json")
	public Token postJson(Users request)
	{
		boolean canAccess;
		String token;
		Users user;

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			user = context.selectFrom(USERS)
								.where(USERS.USERNAME.eq(request.getUsername()))
								.fetchOneInto(Users.class);

			if (user != null)
				canAccess = BCrypt.checkpw(request.getPassword(), user.getPassword());
			else
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
		catch (SQLException e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}

		if (canAccess)
		{
			token = UUID.randomUUID().toString();
			CustomVerifier.addToken(getResponse(), token, user.getId());

			// The salt may have changed since the last time, so update the password in the database with the new salt.
			String saltedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt(SALT));

			try (Connection conn = Database.getConnection();
				 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
			{
				context.update(USERS)
					   .set(USERS.PASSWORD, saltedPassword)
					   .where(USERS.ID.eq(user.getId()))
					   .execute();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INVALID_CREDENTIALS);
		}

		return new Token(token, user.getId(), user.getUsername(), user.getFullName(), user.getEmailAddress(), CustomVerifier.AGE, System.currentTimeMillis());
	}
}

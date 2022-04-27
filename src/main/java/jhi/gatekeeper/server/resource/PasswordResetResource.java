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
import org.jooq.DSLContext;

import java.io.IOException;
import java.sql.*;
import java.util.UUID;
import java.util.logging.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.auth.BCrypt;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.Email;

import static jhi.gatekeeper.server.database.tables.PasswordResetLog.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * {@link ContextResource} handling {@link PasswordResetResource} requests.
 *
 * @author Sebastian Raubach
 */
@Path("passwordreset")
public class PasswordResetResource extends ContextResource
{
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean postPasswordReset(PasswordResetRequest request)
		throws IOException, SQLException
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			UsersRecord user = context.selectFrom(USERS)
									  .where(USERS.USERNAME.eq(request.getUsername())
														   .and(USERS.EMAIL_ADDRESS.eq(request.getEmail())))
									  .fetchAnyInto(UsersRecord.class);

			if (user == null)
			{
				resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_USER.name());
				return false;
			}

			String newPassword = UUID.randomUUID().toString();

			Email.sendNewPassword(request.getJavaLocale(), user, newPassword);

			PasswordResetLogRecord record = context.newRecord(PASSWORD_RESET_LOG);
			record.setUserId(user.getId());
			record.setIpAddress(req.getRemoteAddr());
			record.setTimestamp(new Timestamp(System.currentTimeMillis()));
			record.store();

			// The salt may have changed since the last time, so update the password in the database with the new salt.
			String saltedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(TokenResource.SALT));

			// Update the password in the database
			user.setPassword(saltedPassword);
			user.store(USERS.PASSWORD);

			return true;
		}
		catch (EmailException e)
		{
			Logger.getLogger("").log(Level.SEVERE, "EmailException", e);
			resp.sendError(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), StatusMessage.UNAVAILABLE_EMAIL.name());
			return false;
		}
	}
}

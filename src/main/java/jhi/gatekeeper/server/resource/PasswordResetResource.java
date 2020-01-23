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
import org.jooq.impl.DSL;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.UUID;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.auth.BCrypt;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.Email;

import static jhi.gatekeeper.server.database.tables.PasswordResetLog.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * {@link ServerResource} handling {@link PasswordResetResource} requests.
 *
 * @author Sebastian Raubach
 */
public class PasswordResetResource extends ServerResource
{
	@Post("json")
	public boolean postJson(PasswordResetRequest request)
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			UsersRecord user = context.selectFrom(USERS)
									  .where(USERS.USERNAME.eq(request.getUsername())
														   .and(USERS.EMAIL_ADDRESS.eq(request.getEmail())))
									  .fetchOneInto(UsersRecord.class);

			if (user == null)
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_USER.name());

			String newPassword = UUID.randomUUID().toString();

			Email.sendNewPassword(request.getJavaLocale(), user, newPassword);

			PasswordResetLogRecord record = context.newRecord(PASSWORD_RESET_LOG);
			record.setUserId(user.getId());
			record.setIpAddress(getRequest().getClientInfo().getUpstreamAddress());
			record.setTimestamp(new Timestamp(System.currentTimeMillis()));
			record.store();

			// The salt may have changed since the last time, so update the password in the database with the new salt.
			String saltedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(TokenResource.SALT));

			// Update the password in the database
			user.setPassword(saltedPassword);
			user.store(USERS.PASSWORD);

			return true;
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

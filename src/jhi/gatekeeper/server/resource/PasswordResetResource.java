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
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.records.*;

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
			Optional<UsersRecord> optional = context.selectFrom(USERS)
													.where(USERS.USERNAME.eq(request.getUsername())
																		 .and(USERS.EMAIL_ADDRESS.eq(request.getEmail())))
													.fetchOptional();

			if (optional.isPresent())
			{
				UsersRecord user = optional.get();

				String newPassword = UUID.randomUUID().toString();

				// TODO: Email
				PasswordResetLogRecord record = new PasswordResetLogRecord();
				record.setUserId(user.getId());
				record.setIpAddress(getRequest().getClientInfo().getUpstreamAddress());
				record.setTimestamp(new Timestamp(System.currentTimeMillis()));
				record.store();

				// The salt may have changed since the last time, so update the password in the database with the new salt.
				String saltedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(TokenResource.SALT));

				context.update(USERS)
					   .set(USERS.PASSWORD, saltedPassword)
					   .where(USERS.ID.eq(user.getId()))
					   .execute();

				return true;
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
			}
		}
		catch (SQLException e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

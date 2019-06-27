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

package jhi.gatekeeper.server.auth;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.*;
import org.restlet.data.*;
import org.restlet.security.*;
import org.restlet.util.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import jhi.gatekeeper.server.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.UserTypes.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
public class CustomVerifier implements Verifier
{
	public static final long AGE = 18000000;

	private static Map<String, UserDetails> tokenToTimestamp = new ConcurrentHashMap<>();

	public CustomVerifier()
	{
		Timer timer = new Timer();
		timer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				tokenToTimestamp.entrySet().removeIf(token -> token.getValue().timestamp < (System.currentTimeMillis() - AGE));
			}
		}, 0, AGE);
	}

	public static boolean removeToken(Request request)
	{
		return tokenToTimestamp.remove(getToken(request)) != null;
	}

	public static boolean removeToken(String password)
	{
		return tokenToTimestamp.remove(password) != null;
	}

	public static Integer getUserId(Request request)
	{
		Series<Header> headers = request.getHeaders();
		String token = null;

		for (Header header : headers)
		{
			if (header.getName().equals("authorization"))
			{
				token = header.getValue();

				if (token != null)
					token = token.replace("Bearer ", "");

				break;
			}
		}

		if (token != null && tokenToTimestamp.containsKey(token))
			return tokenToTimestamp.get(token).id;
		else
			return null;
	}

	private static String getToken(Request request)
	{
		ChallengeResponse cr = request.getChallengeResponse();
		if (cr != null)
		{
			String token = cr.getRawValue();

			// If we do, validate it against the cookie
			List<Cookie> cookies = request.getCookies()
										  .stream()
										  .filter(c -> c.getName().equals("token"))
										  .collect(Collectors.toList());

			if (cookies.size() > 0)
			{
				return Objects.equals(token, cookies.get(0).getValue()) ? token : null;
			}
			else
			{
				return null;
			}
		}

		return null;
	}

	public static boolean isAdmin(Request request)
	{
		return isAdmin(getToken(request));
	}

	public static UserDetails getFromSession(Request request)
	{
		return tokenToTimestamp.get(getToken(request));
	}

	public static boolean isAdmin(String token)
	{
		UserDetails details = tokenToTimestamp.get(token);

		if (details == null)
			return false;

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			Optional<Record> optional = context.selectFrom(USERS.leftJoin(USER_HAS_ACCESS_TO_DATABASES).on(USER_HAS_ACCESS_TO_DATABASES.USER_ID.eq(USERS.ID))
																.leftJoin(DATABASE_SYSTEMS).on(DATABASE_SYSTEMS.ID.eq(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID))
																.leftJoin(USER_TYPES).on(USER_TYPES.ID.eq(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID)))
											   .where(DATABASE_SYSTEMS.SYSTEM_NAME.eq("gatekeeper"))
											   .and(USER_TYPES.DESCRIPTION.eq("Administrator"))
											   .and(USERS.ID.eq(details.id))
											   .fetchOptional();

			return optional.isPresent();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static void addToken(Response response, String token, Integer userId)
	{
		setCookie(response, token);
		UserDetails details = new UserDetails();
		details.timestamp = System.currentTimeMillis();
		details.id = userId;
		tokenToTimestamp.put(token, details);
	}

	private static void setCookie(Response response, String token)
	{
		CookieSetting cookie = new CookieSetting(0, "token", token);
		cookie.setAccessRestricted(true);
		cookie.setMaxAge((int) (AGE / 1000));
		cookie.setPath("/");
		response.getCookieSettings().add(cookie);
	}

	@Override
	public int verify(Request request, Response response)
	{
		ChallengeResponse cr = request.getChallengeResponse();
		if (cr != null)
		{
			String token = getToken(request);

			if (token != null)
			{
				boolean canAccess = false;

				// Check if it's a valid token
				UserDetails details = tokenToTimestamp.get(token);

				if (details != null)
				{
					// First, check the bearer token and see if we have it in the cache
					if ((System.currentTimeMillis() - AGE) < details.timestamp)
					{
						canAccess = true;
						// Extend the cookie
						details.timestamp = System.currentTimeMillis();
						tokenToTimestamp.put(token, details);
						setCookie(response, token);
					}
					else
					{
						return RESULT_STALE;
					}
				}

				return canAccess ? RESULT_VALID : RESULT_INVALID;
			}
			else
			{
				return RESULT_INVALID;
			}
		}
		else
		{
			return RESULT_MISSING;
		}
	}

	public static class UserDetails
	{
		private Integer id;
		private Long    timestamp;

		public Integer getId()
		{
			return id;
		}

		public Long getTimestamp()
		{
			return timestamp;
		}

		@Override
		public String toString()
		{
			return "UserDetails{" +
				"id=" + id +
				", timestamp=" + timestamp +
				'}';
		}
	}
}

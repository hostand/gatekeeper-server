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
import org.jooq.impl.DSL;
import org.restlet.*;
import org.restlet.data.Status;
import org.restlet.data.*;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.*;
import org.restlet.routing.Route;
import org.restlet.security.Verifier;
import org.restlet.util.Series;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.util.stream.Collectors;

import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.util.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.UserTypes.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
public class CustomVerifier implements Verifier
{
	public static final long AGE = 43200000; // 12 hours

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

	public static boolean removeToken(String token, Request request, Response response)
	{
		if (token != null)
		{
			UserDetails exists = tokenToTimestamp.remove(token);

			if (exists != null)
			{
				setCookie(request, response, null);
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
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

	private static TokenResult getToken(Request request, Response response)
	{
		ChallengeResponse cr = request.getChallengeResponse();
		if (cr != null)
		{
			TokenResult result = new TokenResult();
			result.token = cr.getRawValue();

			if (StringUtils.isEmpty(result.token) || result.token.equalsIgnoreCase("null"))
				result.token = null;

			// If we do, validate it against the cookie
			List<Cookie> cookies = request.getCookies()
										  .stream()
										  .filter(c -> Objects.equals(c.getName(), "token"))
										  .collect(Collectors.toList());

			if (cookies.size() > 0)
			{
				result.match = Objects.equals(result.token, cookies.get(0).getValue());

				if (!result.match)
					setCookie(request, response, null);
			}
			else
			{
				if (result.token == null)
					return null;
				else
					result.match = true;
			}

			return result;
		}

		return null;
	}

	public static boolean isAdmin(Request request, Response response)
	{
		return isAdmin(getToken(request, response).token);
	}

	public static UserDetails getFromSession(Request request, Response response)
	{
		TokenResult token = getToken(request, response);

		// If there is no token or token and cookie don't match, remove the cookie
		if (token == null || !token.match)
			setCookie(request, response, null);

		if (token == null)
		{
			// We get here if no token is found at all
			return new UserDetails(-1000, null, AGE);
		}
		else if (!StringUtils.isEmpty(token.token) && !token.match)
		{
			// We get here if a token is provided, but no matching cookie is found.
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);
		}
		else
		{
			// We get here if the token is present and it matches the cookie
			UserDetails details = token.token != null ? tokenToTimestamp.get(token.token) : null;

			if (details == null)
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);

			return details;
		}
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
											   .and(DATABASE_SYSTEMS.SERVER_NAME.eq("--"))
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

	public static void addToken(Request request, Response response, String token, Integer userId)
	{
		setCookie(request, response, token);
		UserDetails details = new UserDetails();
		details.timestamp = System.currentTimeMillis();
		details.token = token;
		details.id = userId;
		tokenToTimestamp.put(token, details);
	}

	private static void setCookie(Request request, Response response, String token)
	{
		boolean delete = StringUtils.isEmpty(token);

		CookieSetting cookie = new CookieSetting(0, "token", token);
		cookie.setAccessRestricted(true);
		cookie.setMaxAge(delete ? 0 : (int) (AGE / 1000));
		cookie.setPath(ServletUtils.getRequest(request).getContextPath());

		Logger.getLogger("").log(Level.INFO, "SETTING COOKIE: " + cookie.toString());

		response.getCookieSettings().add(cookie);
	}

	@Override
	public int verify(Request request, Response response)
	{
		Route route = Gatekeeper.INSTANCE.routerAuth.getRoutes().getBest(request, response, 0);

		Finder finder = (Finder) route.getNext();

		Class<?> clazz = finder.getTargetClass();
		String method = request.getMethod().getName();
		long count = Arrays.stream(clazz.getDeclaredMethods())
						   // Only get the ones that require admin permissions
						   .filter(m -> m.isAnnotationPresent(OnlyAdmin.class))
						   // Then filter for all Java methods that have a matching HTTP method annotation
						   .filter(m -> Arrays.stream(m.getDeclaredAnnotations()) // Stream over all annotations
											  .map(a -> a.annotationType().getSimpleName()) // Map them to their simple name
											  .anyMatch(method::equalsIgnoreCase)) // Compare them to the request method
						   .count();

		// If there is a method of the requested type and it has the {@link OnlyAdmin} annotation, but the user isn't an admin, return INVALID.
		if (count > 0 && !isAdmin(request, response))
			return RESULT_INVALID;

		ChallengeResponse cr = request.getChallengeResponse();
		if (cr != null)
		{
			TokenResult token = getToken(request, response);

			Logger.getLogger("").log(Level.INFO, "CHECKING: " + token);

			if (token != null && token.token != null)
			{
				boolean canAccess = false;

				// Check if it's a valid token
				UserDetails details = tokenToTimestamp.get(token.token);

				if (details != null)
				{
					// First, check the bearer token and see if we have it in the cache
					if ((System.currentTimeMillis() - AGE) < details.timestamp)
					{
						canAccess = true;
						// Extend the cookie
						details.timestamp = System.currentTimeMillis();
						tokenToTimestamp.put(token.token, details);
					}
					else
					{
						return RESULT_STALE;
					}
				}

				if (canAccess)
				{
					// Extend the cookie here
					setCookie(request, response, token.token);
					return RESULT_VALID;
				}
				else
				{
					removeToken(token.token, request, response);
					return RESULT_INVALID;
				}
			}
			else
			{
				removeToken(null, request, response);
				return RESULT_INVALID;
			}
		}
		else
		{
			return RESULT_MISSING;
		}
	}

	private static class TokenResult
	{
		private String  token;
		private boolean match;

		@Override
		public String toString()
		{
			return "TokenResult{" +
				"token='" + token + '\'' +
				", match=" + match +
				'}';
		}
	}

	public static class UserDetails
	{
		private Integer id;
		private String  token;
		private Long    timestamp;

		public UserDetails()
		{
		}

		public UserDetails(Integer id, String token, Long timestamp)
		{
			this.id = id;
			this.token = token;
			this.timestamp = timestamp;
		}

		public Integer getId()
		{
			return id;
		}

		public String getToken()
		{
			return token;
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

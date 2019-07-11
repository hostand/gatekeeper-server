package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.ext.servlet.*;
import org.restlet.resource.Delete;
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;

import javax.servlet.http.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.exception.*;
import jhi.gatekeeper.server.util.*;

import static jhi.gatekeeper.server.database.tables.UnapprovedUsers.*;
import static jhi.gatekeeper.server.database.tables.Users.*;
import static jhi.gatekeeper.server.database.tables.ViewUnapprovedUserDetails.*;

/**
 * @author Sebastian Raubach
 */
public class NewRequestResource extends ServerResource
{
	private Integer id;

	@Override
	public void doInit()
	{
		super.doInit();

		try
		{
			this.id = Integer.parseInt(getRequestAttributes().get("requestId").toString());
		}
		catch (NullPointerException | NumberFormatException e)
		{
		}
	}

	@Delete("json")
	public boolean deleteJson()
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS);
		if (id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			return context.deleteFrom(UNAPPROVED_USERS)
						  .where(UNAPPROVED_USERS.ID.eq(id))
						  .execute() > 0;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@Post("json")
	public boolean postJson(NewUnapprovedUser request)
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS);
		if (request == null || request.getDatabaseSystemId() == null
			|| StringUtils.isEmpty(request.getUserUsername(), request.getUserPassword(), request.getUserEmailAddress(), request.getUserFullName()))
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, StatusMessage.BAD_REQUEST_MISSING_FIELDS);

		Locale locale = request.getJavaLocale();

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			boolean userExists = context.fetchExists(USERS, USERS.USERNAME.eq(request.getUserUsername()).or(USERS.EMAIL_ADDRESS.eq(request.getUserEmailAddress())));
			boolean requestExists = context.fetchExists(UNAPPROVED_USERS, UNAPPROVED_USERS.USER_USERNAME.eq(request.getUserUsername()).or(UNAPPROVED_USERS.USER_EMAIL_ADDRESS.eq(request.getUserEmailAddress())));

			if (userExists || requestExists)
				throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, StatusMessage.CONFLICT_USERNAME_EMAIL_ALREADY_IN_USE);

			request.setUserPassword(BCrypt.hashpw(request.getUserPassword(), BCrypt.gensalt(TokenResource.SALT)));
			UnapprovedUsers newUser = request.getUnapprovedUser();
			UnapprovedUsersRecord record = context.newRecord(UNAPPROVED_USERS, newUser);

			String url = Gatekeeper.WEB_BASE;
			String uuid = UUID.randomUUID().toString();

			record.setActivationKey(uuid);

			if (!url.endsWith("/"))
				url += "/";

			url += "gk/activation?activationKey=" + uuid;

			record.store();

			Email.sendActivationPrompt(locale, record, url);

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
			throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, StatusMessage.UNAVAILABLE_EMAIL);
		}
	}

	private String getUrl()
	{
		HttpServletRequest req = ServletUtils.getRequest(getRequest());
		String scheme = req.getScheme(); // http
		String serverName = req.getServerName(); // ics.hutton.ac.uk
		int serverPort = req.getServerPort(); // 80
		String contextPath = req.getContextPath(); // /germinate-baz

		return scheme + "://" + serverName + ":" + serverPort + contextPath; // http://ics.hutton.ac.uk:80/germinate-baz
	}

	@Get("json")
	public List<ViewUnapprovedUserDetails> getJson()
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			SelectWhereStep<ViewUnapprovedUserDetailsRecord> step = context.selectFrom(VIEW_UNAPPROVED_USER_DETAILS);

			if (id != null)
				step.where(VIEW_UNAPPROVED_USER_DETAILS.ID.eq(id));

			return step.where(VIEW_UNAPPROVED_USER_DETAILS.ACTIVATION_KEY.isNull())
					   .orderBy(VIEW_UNAPPROVED_USER_DETAILS.CREATED_ON)
					   .fetch()
					   .into(ViewUnapprovedUserDetails.class);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

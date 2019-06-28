package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;

import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;

import static jhi.gatekeeper.server.database.tables.ViewAccessRequestUserDetails.*;

/**
 * @author Sebastian Raubach
 */
public class ExistingRequestResource extends ServerResource
{
	@Get("json")
	public List<ViewAccessRequestUserDetails> getJson()
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			return context.selectFrom(VIEW_ACCESS_REQUEST_USER_DETAILS)
						  .orderBy(VIEW_ACCESS_REQUEST_USER_DETAILS.CREATED_ON)
						  .fetch()
						  .into(ViewAccessRequestUserDetails.class);
		}
		catch (SQLException e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

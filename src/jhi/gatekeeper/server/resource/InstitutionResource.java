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
import jhi.gatekeeper.server.database.tables.records.*;

import static jhi.gatekeeper.server.database.tables.Institutions.*;

/**
 * @author Sebastian Raubach
 */
public class InstitutionResource extends ServerResource
{
	private Integer id = null;

	@Override
	public void doInit()
	{
		super.doInit();

		try
		{
			this.id = Integer.parseInt(getRequestAttributes().get("institutionId").toString());
		}
		catch (NullPointerException | NumberFormatException e)
		{
		}
	}

	@Get("json")
	public List<Institutions> getJson()
	{
		if (!CustomVerifier.isAdmin(getRequest()))
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS);

		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			SelectWhereStep<InstitutionsRecord> step = context.selectFrom(INSTITUTIONS);

			if (id != null)
				step.where(INSTITUTIONS.ID.eq(id));

			return step.fetchInto(Institutions.class);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

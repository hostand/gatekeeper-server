package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.List;

import jhi.gatekeeper.resource.PaginatedResult;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.Institutions;
import jhi.gatekeeper.server.util.OnlyAdmin;

import static jhi.gatekeeper.server.database.tables.Institutions.*;

/**
 * @author Sebastian Raubach
 */
public class InstitutionResource extends PaginatedServerResource
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

	@OnlyAdmin
	@Post("json")
	public boolean postJson(Institutions newInstitution)
	{
		if (newInstitution == null || newInstitution.getId() != null || id != null)
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			return context.newRecord(INSTITUTIONS, newInstitution).store() > 0;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@OnlyAdmin
	@Get("json")
	public PaginatedResult<List<Institutions>> getJson()
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			SelectWhereStep<Record> step = context.select()
												  .hint("SQL_CALC_FOUND_ROWS")
												  .from(INSTITUTIONS);

			if (id != null)
				step.where(INSTITUTIONS.ID.eq(id));

			List<Institutions> result = step.limit(pageSize)
											.offset(pageSize * currentPage)
											.fetchInto(Institutions.class);

			Integer count = context.fetchOne("SELECT FOUND_ROWS()").into(Integer.class);

			return new PaginatedResult<>(result, count);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

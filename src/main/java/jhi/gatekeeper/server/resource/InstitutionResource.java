package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.Institutions;
import jhi.gatekeeper.server.util.Secured;
import org.jooq.*;

import java.io.IOException;
import java.sql.*;
import java.util.List;

import static jhi.gatekeeper.server.database.tables.Institutions.*;

/**
 * @author Sebastian Raubach
 */
@Path("institution")
@Secured(UserType.ADMIN)
public class InstitutionResource extends PaginatedServerResource
{
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean postInstitution(Institutions newInstitution)
		throws IOException, SQLException
	{
		if (newInstitution == null || newInstitution.getId() != null)
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
			return false;
		}

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			return context.newRecord(INSTITUTIONS, newInstitution).store() > 0;
		}
	}

	@GET
	@Path("/{institutionId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<Institutions>> getInstitutionById(@PathParam("institutionId") Integer institutionId)
		throws SQLException
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			SelectWhereStep<Record> step = context.select()
												  .hint("SQL_CALC_FOUND_ROWS")
												  .from(INSTITUTIONS);

			if (institutionId != null)
				step.where(INSTITUTIONS.ID.eq(institutionId));

			List<Institutions> result = step.limit(pageSize)
											.offset(pageSize * currentPage)
											.fetchInto(Institutions.class);

			Integer count = context.fetchOne("SELECT FOUND_ROWS()").into(Integer.class);

			return new PaginatedResult<>(result, count);
		}
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<Institutions>> getInstitutions()
		throws SQLException
	{
		return this.getInstitutionById(null);
	}
}

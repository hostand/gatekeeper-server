package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.DatabaseSystems;
import jhi.gatekeeper.server.database.tables.records.DatabaseSystemsRecord;
import jhi.gatekeeper.server.util.Secured;
import org.jooq.*;
import org.jooq.Record;

import java.io.IOException;
import java.sql.*;
import java.util.List;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;

/**
 * @author Sebastian Raubach
 */
@Path("database")
@Secured(UserType.ADMIN)
public class DatabaseResource extends PaginatedServerResource
{
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Integer postDatabase(DatabaseSystems database)
		throws IOException, SQLException
	{
		if (database.getId() != null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID_OR_PAYLOAD.name());
			return null;
		}

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			DatabaseSystemsRecord record = context.newRecord(DATABASE_SYSTEMS, database);
			record.store();

			return record.getId();
		}
	}

	@DELETE
	@Path("/{databaseId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean deleteJson(@PathParam("databaseId") Integer databaseId)
		throws IOException, SQLException
	{
		if (databaseId == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID.name());
			return false;
		}

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			int result = context.deleteFrom(DATABASE_SYSTEMS)
								.where(DATABASE_SYSTEMS.ID.eq(databaseId))
								.execute();

			return result > 0;
		}
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<DatabaseSystems>> getDatabases(@QueryParam("server") String queryServer, @QueryParam("database") String queryDatabase)
		throws IOException, SQLException
	{
		return this.getDatabaseById(null, queryServer, queryDatabase);
	}

	@GET
	@Path("/{databaseId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<DatabaseSystems>> getDatabaseById(@PathParam("databaseId") Integer databaseId, @QueryParam("server") String queryServer, @QueryParam("database") String queryDatabase)
		throws IOException, SQLException
	{
		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			SelectWhereStep<Record> step = context.select()
												  .hint("SQL_CALC_FOUND_ROWS")
												  .from(DATABASE_SYSTEMS);

			if (databaseId != null)
			{
				step.where(DATABASE_SYSTEMS.ID.eq(databaseId));
			}
			else
			{
				if (query != null && !"".equals(query))
				{
					query = "%" + query + "%";
					step.where(DATABASE_SYSTEMS.SERVER_NAME.like(query)
														   .or(DATABASE_SYSTEMS.SYSTEM_NAME.like(query))
														   .or(DATABASE_SYSTEMS.DESCRIPTION.like(query)));
				}
				else if (queryServer != null && queryDatabase != null)
				{
					step.where(DATABASE_SYSTEMS.SERVER_NAME.eq(queryServer)
														   .and(DATABASE_SYSTEMS.SYSTEM_NAME.eq(queryDatabase)));
				}
			}

			List<DatabaseSystems> result = setPaginationAndOrderBy(step)
				.fetch()
				.into(DatabaseSystems.class);

			Integer count = context.fetchOne("SELECT FOUND_ROWS()").into(Integer.class);

			return new PaginatedResult<>(result, count);
		}
	}
}

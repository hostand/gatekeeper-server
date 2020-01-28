package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.*;

import java.sql.*;
import java.util.List;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.DatabaseSystems;
import jhi.gatekeeper.server.database.tables.records.DatabaseSystemsRecord;
import jhi.gatekeeper.server.util.OnlyAdmin;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;

/**
 * @author Sebastian Raubach
 */
public class DatabaseResource extends PaginatedServerResource
{
	public static final String PARAM_DATABASE = "database";
	public static final String PARAM_SERVER   = "server";

	private Integer id = null;
	private String  queryDatabase;
	private String  queryServer;

	@Override
	public void doInit()
	{
		super.doInit();

		try
		{
			this.id = Integer.parseInt(getRequestAttributes().get("databaseId").toString());
		}
		catch (NullPointerException | NumberFormatException e)
		{
		}

		this.queryDatabase = getQueryValue(PARAM_DATABASE);
		this.queryServer = getQueryValue(PARAM_SERVER);
	}

	@OnlyAdmin
	@Post("json")
	public Integer postJson(DatabaseSystems database)
	{
		if (id != null || database.getId() != null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID_OR_PAYLOAD.name());

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			DatabaseSystemsRecord record = context.newRecord(DATABASE_SYSTEMS, database);
			record.store();

			return record.getId();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@OnlyAdmin
	@Delete("json")
	public boolean deleteJson()
	{
		if (id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID.name());

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			int result = context.deleteFrom(DATABASE_SYSTEMS)
								.where(DATABASE_SYSTEMS.ID.eq(id))
								.execute();

			return result > 0;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@OnlyAdmin
	@Get("json")
	public PaginatedResult<List<DatabaseSystems>> getJson()
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			SelectWhereStep<Record> step = context.select()
												  .hint("SQL_CALC_FOUND_ROWS")
												  .from(DATABASE_SYSTEMS);

			if (id != null)
			{
				step.where(DATABASE_SYSTEMS.ID.eq(id));
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

				if (ascending != null && orderBy != null)
				{
					// Camelcase to underscore
					orderBy = orderBy.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();

					if (ascending)
						step.orderBy(DSL.field(orderBy).asc());
					else
						step.orderBy(DSL.field(orderBy).desc());
				}
			}

			List<DatabaseSystems> result = step.limit(pageSize)
											   .offset(pageSize * currentPage)
											   .fetch()
											   .into(DatabaseSystems.class);

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

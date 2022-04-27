package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.util.Secured;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.Users.*;
import static jhi.gatekeeper.server.database.tables.ViewUserDetails.*;

/**
 * @author Sebastian Raubach
 */
@Path("user")
public class UserResource extends PaginatedServerResource
{
	@DELETE
	@Path("/{userId}")
	@Secured(UserType.ADMIN)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean deleteUser(@PathParam("userId") Integer userId)
		throws IOException, SQLException
	{
		if (userId == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID.name());
			return false;
		}

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			int result = context.deleteFrom(USERS)
								.where(USERS.ID.eq(userId))
								.execute();

			return result > 0;
		}
	}

	@POST
	@Secured(UserType.ADMIN)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean postUser(Users newUser)
		throws IOException, SQLException
	{
		if (newUser == null || newUser.getId() != null)
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
			return false;
		}

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			newUser.setPassword(BCrypt.hashpw(newUser.getPassword(), BCrypt.gensalt(TokenResource.SALT)));
			return context.newRecord(USERS, newUser).store() > 0;
		}
	}

	@GET
	@Secured
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<ViewUserDetails>> getUsers(@QueryParam("username") String username, @QueryParam("database") String database, @QueryParam("server") String server)
		throws IOException, SQLException
	{
		return this.getUserById(null, username, database, server);
	}

	@GET
	@Path("/{userId}")
	@Secured
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PaginatedResult<List<ViewUserDetails>> getUserById(@PathParam("userId") Integer userId, @QueryParam("username") String username, @QueryParam("database") String database, @QueryParam("server") String server)
		throws IOException, SQLException
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			AuthenticationFilter.UserDetails userDetails = (AuthenticationFilter.UserDetails) securityContext.getUserPrincipal();

			SelectWhereStep<Record> step = context.select()
												  .hint("SQL_CALC_FOUND_ROWS")
												  .from(VIEW_USER_DETAILS);

			if (userId != null)
			{
				// A user must be allowed to request their own details, but nothing else
				if (!Objects.equals(userId, userDetails.getId()))
				{
					resp.sendError(Response.Status.FORBIDDEN.getStatusCode(), StatusMessage.FORBIDDEN_ACCESS_TO_OTHER_USER.name());
					return null;
				}
				else
					step.where(VIEW_USER_DETAILS.ID.eq(userId));
			}
			else if (username != null)
			{
				if (userDetails.getUserType() != UserType.ADMIN)
				{
					resp.sendError(Response.Status.FORBIDDEN.getStatusCode(), StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS.name());
					return null;
				}
				else
					step.where(VIEW_USER_DETAILS.USERNAME.eq(username));
			}
			else if (database != null && server != null)
			{
				if (userDetails.getUserType() != UserType.ADMIN)
				{
					resp.sendError(Response.Status.FORBIDDEN.getStatusCode(), StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS.name());
					return null;
				}
				else
					step.where(DSL.exists(DSL.selectOne()
											 .from(DATABASE_SYSTEMS)
											 .leftJoin(USER_HAS_ACCESS_TO_DATABASES).on(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID.eq(DATABASE_SYSTEMS.ID))
											 .where(USER_HAS_ACCESS_TO_DATABASES.USER_ID.eq(VIEW_USER_DETAILS.ID))
											 .and(DATABASE_SYSTEMS.SERVER_NAME.eq(server))
											 .and(DATABASE_SYSTEMS.SYSTEM_NAME.eq(database))));
			}
			else
			{
				if (userDetails.getUserType() != UserType.ADMIN)
				{
					resp.sendError(Response.Status.FORBIDDEN.getStatusCode(), StatusMessage.FORBIDDEN_INSUFFICIENT_PERMISSIONS.name());
					return null;
				}

				if (query != null && !"".equals(query))
				{
					query = "%" + query + "%";
					step.where(VIEW_USER_DETAILS.USERNAME.like(query)
														 .or(VIEW_USER_DETAILS.ADDRESS.like(query))
														 .or(VIEW_USER_DETAILS.EMAIL_ADDRESS.like(query))
														 .or(VIEW_USER_DETAILS.NAME.like(query))
														 .or(VIEW_USER_DETAILS.FULL_NAME.like(query))
														 .or(VIEW_USER_DETAILS.ACRONYM.like(query)));
				}

				if (ascending != null && orderBy != null)
				{
					// Camelcase to underscore
					orderBy = orderBy.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();

					if (ascending)
						step.orderBy(DSL.field(getSafeColumn(orderBy)).asc());
					else
						step.orderBy(DSL.field(getSafeColumn(orderBy)).desc());
				}
			}

			List<ViewUserDetails> result = step.limit(pageSize)
											   .offset(pageSize * currentPage)
											   .fetch()
											   .into(ViewUserDetails.class);

			Integer count = context.fetchOne("SELECT FOUND_ROWS()").into(Integer.class);

			return new PaginatedResult<>(result, count);
		}
	}
}

package jhi.gatekeeper.server.resource;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.DatabaseSystems;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.*;
import org.jooq.DSLContext;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.Institutions.*;
import static jhi.gatekeeper.server.database.tables.UnapprovedUsers.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
@Path("request/new/{requestId}/decision")
@Secured(UserType.ADMIN)
public class NewRequestDecisionResource extends ContextResource
{
	public static boolean decide(Integer id, RequestDecision request, HttpServletResponse resp)
		throws IOException, SQLException
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			UnapprovedUsersRecord unapprovedUser = context.selectFrom(UNAPPROVED_USERS)
														  .where(UNAPPROVED_USERS.ID.eq(id))
														  .fetchAnyInto(UNAPPROVED_USERS);

			if (unapprovedUser == null)
			{
				resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_USER.name());
				return false;
			}

			switch (request.getDecision())
			{
				case REJECT:
					// Set rejected flag
					unapprovedUser.setHasBeenRejected((byte) 1);
					unapprovedUser.store();

					Email.sendUnapprovedUserRejected(Locale.ENGLISH, unapprovedUser, request.getFeedback());
					break;
				case APPROVE:
					InstitutionsRecord institution;
					if (unapprovedUser.getInstitutionId() != null)
					{
						// Get the institution by its id
						institution = context.selectFrom(INSTITUTIONS)
											 .where(INSTITUTIONS.ID.eq(unapprovedUser.getInstitutionId()))
											 .fetchAnyInto(InstitutionsRecord.class);

						if (institution == null)
						{
							resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_INSTITUTION.name());
							return false;
						}
					}
					else
					{
						institution = context.newRecord(INSTITUTIONS);
						institution.setName(unapprovedUser.getInstitutionName());
						institution.setAcronym(unapprovedUser.getInstitutionAcronym());
						institution.setAddress(unapprovedUser.getInstitutionAddress());

						// Check if there exists one with the same parameters
						Optional<InstitutionsRecord> optional = context.selectFrom(INSTITUTIONS).where(INSTITUTIONS.NAME.eq(institution.getName())
																														.and(INSTITUTIONS.ACRONYM.eq(institution.getAcronym()))
																														.and(INSTITUTIONS.ADDRESS.eq(institution.getAddress())))
																	   .fetchOptionalInto(InstitutionsRecord.class);

						// If it exists, get it, otherwise insert it
						if (optional.isPresent())
							institution = optional.get();
						else
							institution.store();
					}

					// Create the user
					UsersRecord user = context.newRecord(USERS);
					user.setUsername(unapprovedUser.getUserUsername());
					user.setPassword(unapprovedUser.getUserPassword());
					user.setFullName(unapprovedUser.getUserFullName());
					user.setEmailAddress(unapprovedUser.getUserEmailAddress());
					user.setInstitutionId(institution.getId());
					user.setHasAccessToGatekeeper((byte) 1);
					user.setCreatedOn(new Timestamp(System.currentTimeMillis()));
					user.store();

					DatabaseSystems system = context.selectFrom(DATABASE_SYSTEMS)
													.where(DATABASE_SYSTEMS.ID.eq(unapprovedUser.getDatabaseSystemId()))
													.fetchAnyInto(DatabaseSystems.class);

					// Grant access
					UserHasAccessToDatabasesRecord record = context.newRecord(USER_HAS_ACCESS_TO_DATABASES);
					record.setDatabaseId(unapprovedUser.getDatabaseSystemId());
					record.setUserId(user.getId());
					record.setUserTypeId(2);
					record.store();

					// Delete the request
					unapprovedUser.delete();

					Email.sendUnapprovedUserApproved(Locale.ENGLISH, unapprovedUser, system);
					break;
			}
			return true;
		}
		catch (EmailException e)
		{
			e.printStackTrace();
			resp.sendError(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), StatusMessage.UNAVAILABLE_EMAIL.name());
			return false;
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public boolean postJson(RequestDecision request, @PathParam("requestId") Integer requestId)
		throws IOException, SQLException
	{
		if (requestId == null)
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ID.name());
			return false;
		}
		if (request == null || !Objects.equals(request.getRequestId(), requestId))
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
			return false;
		}

		return decide(requestId, request, resp);
	}
}

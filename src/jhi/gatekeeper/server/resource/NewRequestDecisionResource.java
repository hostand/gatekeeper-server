package jhi.gatekeeper.server.resource;

import org.jooq.*;
import org.jooq.impl.*;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.*;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.exception.*;
import jhi.gatekeeper.server.util.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.Institutions.*;
import static jhi.gatekeeper.server.database.tables.UnapprovedUsers.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
public class NewRequestDecisionResource extends ServerResource
{
	private Integer id;

	public static boolean decide(Integer id, RequestDecision request)
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			UnapprovedUsersRecord unapprovedUser = context.selectFrom(UNAPPROVED_USERS)
														  .where(UNAPPROVED_USERS.ID.eq(id))
														  .fetchOneInto(UNAPPROVED_USERS);

			if (unapprovedUser == null)
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_USER);

			switch (request.getDecision())
			{
				case REJECT:
					// Set rejected flag
					unapprovedUser.setHasBeenRejected((byte) 1);
					unapprovedUser.store();

					Email.sendUnapprovedUserRejected(request.getJavaLocale(), unapprovedUser, request.getFeedback());
					break;
				case APPROVE:
					InstitutionsRecord institution;
					if (unapprovedUser.getInstitutionId() != null)
					{
						// Get the institution by its id
						institution = context.selectFrom(INSTITUTIONS)
											 .where(INSTITUTIONS.ID.eq(unapprovedUser.getInstitutionId()))
											 .fetchOneInto(InstitutionsRecord.class);

						if (institution == null)
							throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_INSTITUTION);
					}
					else
					{
						institution = new InstitutionsRecord();
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
							context.newRecord(INSTITUTIONS, institution).store();
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
													.fetchOneInto(DatabaseSystems.class);

					// Grant access
					UserHasAccessToDatabasesRecord record = context.newRecord(USER_HAS_ACCESS_TO_DATABASES);
					record.setDatabaseId(unapprovedUser.getDatabaseSystemId());
					record.setUserId(user.getId());
					record.setUserTypeId(2);
					record.store();

					// Delete the request
					unapprovedUser.delete();

					Email.sendUnapprovedUserApproved(request.getJavaLocale(), unapprovedUser, system);
					break;
			}
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

	@OnlyAdmin
	@Post("json")
	public boolean postJson(RequestDecision request)
	{
		if (id == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ID);
		if (request == null || !Objects.equals(request.getRequestId(), id))
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);

		return decide(id, request);
	}
}

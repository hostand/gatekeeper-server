package jhi.gatekeeper.server.resource;

import org.jooq.DSLContext;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.Locale;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.DatabaseSystems;
import jhi.gatekeeper.server.database.tables.records.UnapprovedUsersRecord;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UnapprovedUsers.*;

/**
 * @author Sebastian Raubach
 */
public class ActivationRequestResource extends ServerResource
{
	@Post("json")
	public ActivationDecision getJson(ActivationRequest request)
	{
		if (StringUtils.isEmpty(request.getActivationKey()))
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ACTIVATION_KEY.name());

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			UnapprovedUsersRecord userRequest = context.selectFrom(UNAPPROVED_USERS)
													   .where(UNAPPROVED_USERS.ACTIVATION_KEY.eq(request.getActivationKey()))
													   .fetchAnyInto(UnapprovedUsersRecord.class);

			if (userRequest == null)
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, StatusMessage.NOT_FOUND_ACTIVATION_REQUEST.name());

			DatabaseSystems database = context.selectFrom(DATABASE_SYSTEMS)
											  .where(DATABASE_SYSTEMS.ID.eq(userRequest.getDatabaseSystemId()))
											  .fetchAnyInto(DatabaseSystems.class);

			Byte needsApproval = userRequest.getNeedsApproval();
			Locale locale = request.getJavaLocale();

			if (needsApproval == 1)
			{
				Email.sendAwaitingApproval(locale, userRequest);
				Email.sendAdministratorNotification(locale, database, true);

				userRequest.setActivationKey(null);
				userRequest.store();

				return ActivationDecision.AWAITS_APPROVAL;
			}
			else
			{
				RequestDecision decision = new RequestDecision(userRequest.getId(), Decision.APPROVE, null);
				decision.setJavaLocale(request.getJavaLocale());
				boolean result = NewRequestDecisionResource.decide(userRequest.getId(), decision);
				Email.sendAdministratorNotification(locale, database, false);

				if (result)
					return ActivationDecision.GRANTED;
				else
					return ActivationDecision.ERROR;
			}
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}
		catch (EmailException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, StatusMessage.UNAVAILABLE_EMAIL.name());
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}
}

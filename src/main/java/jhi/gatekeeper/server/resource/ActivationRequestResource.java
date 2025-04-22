package jhi.gatekeeper.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.Database;
import jhi.gatekeeper.server.database.tables.pojos.DatabaseSystems;
import jhi.gatekeeper.server.database.tables.records.UnapprovedUsersRecord;
import jhi.gatekeeper.server.exception.EmailException;
import jhi.gatekeeper.server.util.*;
import org.jooq.DSLContext;

import java.io.IOException;
import java.sql.*;
import java.util.Locale;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UnapprovedUsers.*;

/**
 * @author Sebastian Raubach
 */
@Path("request/activation")
public class ActivationRequestResource extends ContextResource
{
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ActivationDecision postActivation(ActivationRequest request)
		throws IOException, SQLException
	{
		if (StringUtils.isEmpty(request.getActivationKey()))
		{
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ACTIVATION_KEY.name());
			return null;
		}

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			UnapprovedUsersRecord userRequest = context.selectFrom(UNAPPROVED_USERS)
													   .where(UNAPPROVED_USERS.ACTIVATION_KEY.eq(request.getActivationKey()))
													   .fetchAnyInto(UnapprovedUsersRecord.class);

			if (userRequest == null)
			{
				resp.sendError(Response.Status.NOT_FOUND.getStatusCode(), StatusMessage.NOT_FOUND_ACTIVATION_REQUEST.name());
				return null;
			}

			DatabaseSystems database = context.selectFrom(DATABASE_SYSTEMS)
											  .where(DATABASE_SYSTEMS.ID.eq(userRequest.getDatabaseSystemId()))
											  .fetchAnyInto(DatabaseSystems.class);

			Boolean needsApproval = userRequest.getNeedsApproval() == 1;
			Locale locale = request.getJavaLocale();

			if (needsApproval)
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
				boolean result = NewRequestDecisionResource.decide(userRequest.getId(), decision, resp);
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
			resp.sendError(Response.Status.NOT_FOUND.getStatusCode());
			return null;
		}
		catch (EmailException e)
		{
			e.printStackTrace();
			resp.sendError(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), StatusMessage.UNAVAILABLE_EMAIL.name());
			return null;
		}
	}
}

package jhi.gatekeeper.client;

import java.util.*;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.database.tables.pojos.*;
import retrofit2.*;
import retrofit2.http.*;

/**
 * This service is meant to be used alongside a Retrofit client.
 * Make sure to send the token with every call that requires authentication both as the Bearer token and as a cookie called token.
 *
 * @author Sebastian Raubach
 */
public interface GatekeeperService
{
	/**
	 * Sign in to Gatekeeper.
	 *
	 * @param user The {@link Users} object. Must contain {@link Users#username} and {@link Users#password}.
	 * @return The {@link Token} containing the {@link Token#token} to be submitted with any authenticated call as both the Bearer token and a cookie called token.
	 */
	@POST("token")
	Call<Token> postToken(@Body Users user);

	/**
	 * Deletes the token from the server.
	 *
	 * @param user The {@link Users} object. Must contain {@link Users#username} and the TOKEN as the {@link Users#password}.
	 * @return <code>true</code> if the token has successfully been deleted.
	 */
	@HTTP(method = "DELETE", path = "token", hasBody = true)
	Call<Boolean> deleteToken(@Body Users user);


	/**
	 * Returns the list of all {@link Institutions}.
	 *
	 * @return The list of all {@link Institutions}.
	 * @apiNote REQUIRES AUTHENTICATION
	 */
	@GET("institution")
	Call<List<Institutions>> getInstitutions();
}

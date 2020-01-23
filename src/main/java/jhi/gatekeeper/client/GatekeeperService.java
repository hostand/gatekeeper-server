package jhi.gatekeeper.client;

import java.util.List;

import jhi.gatekeeper.resource.*;
import jhi.gatekeeper.server.database.tables.pojos.*;
import retrofit2.Call;
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
	 * Returns the paginated list of all {@link Institutions}.
	 *
	 * @param page  The current page
	 * @param limit The page size
	 * @return The paginated list of all {@link Institutions}.
	 * @apiNote REQUIRES AUTHENTICATION
	 */
	@GET("institution")
	Call<PaginatedResult<List<Institutions>>> getInstitutions(@Query("page") int page, @Query("limit") int limit);


	/**
	 * Creates a new unapproved user.
	 *
	 * @param newUnapprovedUser The {@link NewUnapprovedUser} to create.
	 * @return <code>true</code> if the database item has successfully been created
	 */
	@POST("request/new")
	Call<Boolean> addNewRequest(@Body NewUnapprovedUser newUnapprovedUser);


	/**
	 * Creates a new access request.
	 *
	 * @param newAccessRequest The {@link NewAccessRequest} to create.
	 * @return <code>true</code> if the database item has successfully been created
	 */
	@POST("request/existing")
	Call<Boolean> addExistingRequest(@Body NewAccessRequest newAccessRequest);


	/**
	 * Returns the paginated list of {@link ViewUserPermissions} for the user with the given user id.
	 *
	 * @param userId         The user id
	 * @param databaseServer The optional database server to limit the request to.
	 * @param databaseName   The optional database name to limit the request to.
	 * @param page           The current page
	 * @param limit          The page size
	 * @return The paginated list of {@link ViewUserPermissions} for the user with the given user id.
	 */
	@GET("user/{userId}/permission")
	Call<PaginatedResult<List<ViewUserPermissions>>> getUserPermissions(@Path("userId") Integer userId, @Query("databaseServer") String databaseServer, @Query("databaseName") String databaseName, @Query("page") int page, @Query("limit") int limit);


	/**
	 * Returns a paginated list of {@link DatabaseSystems} for the server and database.
	 *
	 * @param server   The database server
	 * @param database The database name
	 * @param page     The current page
	 * @param limit    The page size
	 * @return The paginated list of {@link DatabaseSystems} for the server and database.
	 */
	@GET("database")
	Call<PaginatedResult<List<DatabaseSystems>>> getDatabaseSystems(@Query("server") String server, @Query("database") String database, @Query("page") int page, @Query("limit") int limit);


	@GET("user")
	Call<PaginatedResult<List<ViewUserDetails>>> getUsers(@Query("server") String server, @Query("database") String database, @Query("page") int page, @Query("limit") int limit);
}

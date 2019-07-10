package jhi.gatekeeper.server;

import org.restlet.*;
import org.restlet.data.*;
import org.restlet.engine.application.*;
import org.restlet.resource.*;
import org.restlet.routing.*;
import org.restlet.security.*;
import org.restlet.util.*;

import java.util.*;

import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.resource.*;

/**
 * @author Sebastian Raubach
 */
public class Gatekeeper extends Application
{
	public static String WEB_BASE = null;

	private static CustomVerifier         verifier = new CustomVerifier();
	private        ChallengeAuthenticator authenticator;
	private        MethodAuthorizer       authorizer;

	public Gatekeeper()
	{
		// Set information about API
		setName("Gatekeeper Server");
		setDescription("This is the server implementation for the Gatekeeper");
		setOwner("The James Hutton Institute");
		setAuthor("Sebastian Raubach, Information & Computational Sciences");
	}

	private void setUpAuthentication(Context context)
	{
		authorizer = new MethodAuthorizer();
		authorizer.getAuthenticatedMethods().add(Method.GET);
		authorizer.getAuthenticatedMethods().add(Method.OPTIONS);
		authorizer.getAuthenticatedMethods().add(Method.PATCH);
		authorizer.getAuthenticatedMethods().add(Method.POST);
		authorizer.getAuthenticatedMethods().add(Method.PUT);
		authorizer.getAuthenticatedMethods().add(Method.DELETE);

		authenticator = new ChallengeAuthenticator(context, true, ChallengeScheme.HTTP_OAUTH_BEARER, "Gatekeeper", verifier);
	}

	@Override
	public Restlet createInboundRoot()
	{
		Context context = getContext();

		setUpAuthentication(context);

		// Set the encoder
//		Filter encoder = new Encoder(context, false, true, new EncoderService(true));

		// Create new router
		Router routerAuth = new Router(context);
		Router routerUnauth = new Router(context);

		// Set the Cors filter
		CorsFilter corsFilter = new CorsFilter(context, routerUnauth)
		{
			@Override
			protected int beforeHandle(Request request, Response response)
			{
				if (getCorsResponseHelper().isCorsRequest(request))
				{
					Series<Header> headers = request.getHeaders();

					for (Header header : headers)
					{
						if (header.getName().equalsIgnoreCase("origin"))
						{
							response.setAccessControlAllowOrigin(header.getValue());
						}
					}
				}
				return super.beforeHandle(request, response);
			}
		};
		corsFilter.setAllowedOrigins(new HashSet<>(Collections.singletonList("*")));
		corsFilter.setSkippingResourceForCorsOptions(true);
		corsFilter.setAllowingAllRequestedHeaders(true);
		corsFilter.setDefaultAllowedMethods(new HashSet<>(Arrays.asList(Method.POST, Method.GET, Method.PUT, Method.PATCH, Method.DELETE, Method.OPTIONS)));
		corsFilter.setAllowedCredentials(true);

		// Attach the url handlers
		attachToRouter(routerAuth, "/stat/count", StatCountResource.class);

		attachToRouter(routerAuth, "/database", DatabaseResource.class);
		attachToRouter(routerAuth, "/database/{databaseId}", DatabaseResource.class);

		attachToRouter(routerUnauth, "/request/activation", ActivationRequestResource.class);
		attachToRouter(routerAuth, "/request/existing", ExistingRequestResource.class);
		attachToRouter(routerAuth, "/request/existing/{requestId}", ExistingRequestResource.class);
		attachToRouter(routerAuth, "/request/existing/{requestId}/decision", ExistingRequestDecisionResource.class);
		attachToRouter(routerAuth, "/request/new", NewRequestResource.class);
		attachToRouter(routerAuth, "/request/new/{requestId}", NewRequestResource.class);
		attachToRouter(routerAuth, "/request/new/{requestId}/decision", NewRequestDecisionResource.class);

		attachToRouter(routerAuth, "/user", UserResource.class);
		attachToRouter(routerAuth, "/user/{userId}", UserResource.class);
		attachToRouter(routerAuth, "/user/{userId}/email", UserEmailResource.class);
		attachToRouter(routerAuth, "/user/{userId}/gatekeeper", UserGatekeeperResource.class);
		attachToRouter(routerAuth, "/user/{userId}/password", UserPasswordResource.class);
		attachToRouter(routerAuth, "/user/{userId}/permission", UserPermissionResource.class);

		attachToRouter(routerUnauth, "/passwordreset", PasswordResetResource.class);

		attachToRouter(routerUnauth, "/token", TokenResource.class);

		// CORS first, then encoder
		corsFilter.setNext(routerUnauth);
		// After that the unauthorized paths
//		encoder.setNext(routerUnauth);
		// Set everything that isn't covered to go through the authenticator
		routerUnauth.attachDefault(authenticator);
		authenticator.setNext(authorizer);
		// And finally it ends up at the authenticated router
		authorizer.setNext(routerAuth);

		return corsFilter;
	}

	private void attachToRouter(Router router, String url, Class<? extends ServerResource> clazz)
	{
		router.attach(url, clazz);
		router.attach(url + "/", clazz);
	}
}

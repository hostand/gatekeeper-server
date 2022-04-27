package jhi.gatekeeper.server;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author Sebastian Raubach
 */
@ApplicationPath("/api/")
public class Gatekeeper extends ResourceConfig
{
	public Gatekeeper()
	{
		packages("jhi.gatekeeper.server");
	}
}

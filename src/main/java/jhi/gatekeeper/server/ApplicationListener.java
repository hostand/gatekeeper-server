package jhi.gatekeeper.server;

import javax.servlet.*;

import jhi.gatekeeper.resource.ServerProperty;
import jhi.gatekeeper.server.resource.TokenResource;
import jhi.gatekeeper.server.util.Email;
import jhi.gatekeeper.server.util.watcher.PropertyWatcher;

public class ApplicationListener implements ServletContextListener
{
	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		PropertyWatcher.initialize();
		Email.init();

		Integer salt;
		try
		{
			salt = PropertyWatcher.getInteger(ServerProperty.SALT);
		}
		catch (Exception e)
		{
			salt = 10;
		}

		TokenResource.SALT = salt;
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent)
	{
	}
}

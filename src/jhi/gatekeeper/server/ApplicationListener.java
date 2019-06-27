package jhi.gatekeeper.server;

import javax.servlet.*;

import jhi.gatekeeper.server.resource.*;

public class ApplicationListener implements ServletContextListener
{
	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		ServletContext ctx = sce.getServletContext();
		String database = ctx.getInitParameter("database");
		String username = ctx.getInitParameter("username");
		String password = ctx.getInitParameter("password");
		Database.init(database, username, password);

		Integer salt = 10;
		try
		{
			salt = Integer.parseInt(ctx.getInitParameter("salt"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		TokenResource.SALT = salt;
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent)
	{
	}
}

package jhi.gatekeeper.server;

import javax.servlet.*;

import jhi.gatekeeper.server.resource.*;
import jhi.gatekeeper.server.util.*;

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

		Gatekeeper.WEB_BASE = ctx.getInitParameter("web.base");

		String emailServer = ctx.getInitParameter("email.server");
		String emailAddress = ctx.getInitParameter("email.address");
		String emailUsername = ctx.getInitParameter("email.username");
		String emailPassword = ctx.getInitParameter("email.password");
		String emailPort = ctx.getInitParameter("email.port");
		Email.init(emailServer, emailAddress, emailUsername, emailPassword, emailPort);

		Integer salt;
		try
		{
			salt = Integer.parseInt(ctx.getInitParameter("salt"));
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

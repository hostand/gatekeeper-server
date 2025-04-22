package jhi.gatekeeper.server;

import jhi.gatekeeper.resource.ServerProperty;
import jhi.gatekeeper.server.database.GatekeeperDb;
import jhi.gatekeeper.server.util.*;
import jhi.gatekeeper.server.util.watcher.PropertyWatcher;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.jooq.*;
import org.jooq.conf.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.io.*;
import java.io.File;
import java.net.*;
import java.sql.*;
import java.util.TimeZone;
import java.util.logging.*;

import jhi.gatekeeper.server.auth.BCrypt;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.resource.TokenResource;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.UserTypes.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
public class Database
{
	private static String databaseServer;
	private static String databaseName;
	private static String databasePort;
	private static String username;
	private static String password;

	private static String utc = TimeZone.getDefault().getID();

	public static void init(String databaseServer, String databaseName, String databasePort, String username, String password, boolean initAndUpdate)
	{
		Database.databaseServer = databaseServer;
		Database.databaseName = databaseName;
		Database.databasePort = databasePort;
		Database.username = username;
		Database.password = password;

		try
		{
			// The newInstance() call is a work around for some
			// broken Java implementations
			Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
		}
		catch (Exception ex)
		{
			// handle the error
		}

		// Get an initial connection to try if it works
		try (Connection conn = getConnection())
		{
			DSL.using(conn, SQLDialect.MYSQL);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		if (initAndUpdate)
		{
			boolean databaseExists = true;
			// Check if the germinatebase table exists
			try (Connection conn = getConnection();
				 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(1) AS count FROM information_schema.tables WHERE table_schema = ? AND table_name = ?"))
			{
				stmt.setString(1, databaseName);
				stmt.setString(2, "users");
				ResultSet rs = stmt.executeQuery();

				while (rs.next())
					databaseExists = rs.getInt("count") > 0;

				rs.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}

			if (!databaseExists)
			{
				// Set up the database initially
				try
				{
					URL url = Database.class.getClassLoader().getResource("jhi/gatekeeper/server/util/databasesetup/db_setup.sql");

					if (url != null)
					{
						Logger.getLogger("").log(Level.INFO, "RUNNING DATABASE CREATION SCRIPT!");
						executeFile(new File(url.toURI()));
					}
					else
					{
						throw new IOException("Setup SQL file not found!");
					}
				}
				catch (IOException | URISyntaxException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				Logger.getLogger("").log(Level.INFO, "DATABASE EXISTS, NO NEED TO CREATE IT!");
			}

			// Convert the database to UTF-8
			try (Connection conn = getConnection())
			{
				DSLContext context = Database.getContext(conn);
				context.execute("ALTER DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
			}
			catch (SQLException | DataAccessException e)
			{
				e.printStackTrace();
			}

			// Run database update
			try
			{
				Logger.getLogger("").log(Level.INFO, "RUNNING FLYWAY on: " + databaseName);
				Flyway flyway = new Flyway();
				flyway.setTable("schema_version");
				flyway.setValidateOnMigrate(false);
				flyway.setDataSource(getDatabaseUrl(), username, password);
				flyway.setLocations("classpath:jhi.gatekeeper.server.util.databasemigration");
				flyway.setBaselineOnMigrate(true);
				flyway.migrate();
				flyway.repair();
			}
			catch (FlywayException e)
			{
				e.printStackTrace();
			}
		}

		// Then create all views and stored procedures
		try
		{
			URL url = Database.class.getClassLoader().getResource("jhi/gatekeeper/server/util/databasesetup/views_procedures.sql");

			if (url != null)
			{
				Logger.getLogger("").log(Level.INFO, "RUNNING VIEW/PROCEDURE CREATION SCRIPT!");
				executeFile(new File(url.toURI()));
			}
			else
			{
				throw new IOException("View/procedure SQL file not found!");
			}
		}
		catch (IOException | URISyntaxException e)
		{
			e.printStackTrace();
		}

		// Check if an admin user exists, if not, create it
		try (Connection conn = getConnection())
		{
			DSLContext context = Database.getContext(conn);
			DatabaseSystems db = context.selectFrom(DATABASE_SYSTEMS)
										.where(DATABASE_SYSTEMS.SERVER_NAME.eq("--"))
										.and(DATABASE_SYSTEMS.SYSTEM_NAME.eq("gatekeeper"))
										.fetchAnyInto(DatabaseSystems.class);

			UserTypes type = context.selectFrom(USER_TYPES)
									.where(USER_TYPES.DESCRIPTION.eq("Administrator"))
									.fetchAnyInto(UserTypes.class);

			Integer adminCount = context.selectCount()
									.from(USERS)
									.leftJoin(USER_HAS_ACCESS_TO_DATABASES).on(USERS.ID.eq(USER_HAS_ACCESS_TO_DATABASES.USER_ID))
									.where(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID.eq(db.getId()))
									.and(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID.eq(type.getId()))
									.fetchAnyInto(Integer.class);

			if (adminCount == null || adminCount < 1)
			{
				// Create a default Admin user with password "password".
				UsersRecord admin = context.newRecord(USERS);
				admin.setUsername("admin");
				admin.setPassword(BCrypt.hashpw(PropertyWatcher.get(ServerProperty.GENERAL_ADMIN_PASSWORD), BCrypt.gensalt(TokenResource.SALT)));
				admin.setFullName("The Admin");
				admin.setEmailAddress("--");
				admin.store();

				UserHasAccessToDatabasesRecord access = context.newRecord(USER_HAS_ACCESS_TO_DATABASES);
				access.setUserId(admin.getId());
				access.setDatabaseId(db.getId());
				access.setUserTypeId(type.getId());
				access.setPrimaryContact((byte) 1);
				access.store();
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		Email.initPrimaryContactMap(null);
	}

	private static void executeFile(File sqlFile)
	{
		try (Connection conn = Database.getConnection();
			 BufferedReader br = new BufferedReader(new FileReader(sqlFile)))
		{
			ScriptRunner runner = new ScriptRunner(conn, true, true);
			runner.runScript(br);
		}
		catch (SQLException | IOException e)
		{
			e.printStackTrace();
		}
	}

	private static String getDatabaseUrl()
	{
		return "jdbc:mysql://" + databaseServer + ":" + (databasePort != null ? databasePort : "3306") + "/" + databaseName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=" + utc;
	}

	public static Connection getConnection()
		throws SQLException
	{
		return DriverManager.getConnection(getDatabaseUrl(), username, password);
	}

	public static DSLContext getContext(Connection connection)
	{
		Settings settings = new Settings()
			.withRenderMapping(new RenderMapping()
				.withSchemata(
					new MappedSchema().withInput(GatekeeperDb.GATEKEEPER_DB.getQualifiedName().first())
									  .withOutput(databaseName)));

		return DSL.using(connection, SQLDialect.MYSQL, settings);
	}
}

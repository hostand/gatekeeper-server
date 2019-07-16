package jhi.gatekeeper.server;

import org.flywaydb.core.*;
import org.flywaydb.core.api.*;
import org.jooq.*;
import org.jooq.impl.*;

import java.sql.*;
import java.util.logging.*;

import jhi.gatekeeper.server.auth.*;
import jhi.gatekeeper.server.database.tables.pojos.*;
import jhi.gatekeeper.server.database.tables.records.*;
import jhi.gatekeeper.server.resource.*;

import static jhi.gatekeeper.server.database.tables.DatabaseSystems.*;
import static jhi.gatekeeper.server.database.tables.UserHasAccessToDatabases.*;
import static jhi.gatekeeper.server.database.tables.UserTypes.*;
import static jhi.gatekeeper.server.database.tables.Users.*;

/**
 * @author Sebastian Raubach
 */
public class Database
{
	private static String database;
	private static String username;
	private static String password;

	public static void init(String database, String username, String password)
	{
		Database.database = database;
		Database.username = username;
		Database.password = password;

		try
		{
			// The newInstance() call is a work around for some
			// broken Java implementations
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
		}
		catch (Exception ex)
		{
			// handle the error
		}

		// Get an initial connection to try if it works
		try (Connection conn = getConnection())
		{
			DSL.using(conn, SQLDialect.MYSQL).close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		// Run database update/init
		try
		{
			Logger.getLogger("").log(Level.INFO, "RUNNING FLYWAY on: " + database);
			Flyway flyway = new Flyway();
			flyway.setTable("schema_version");
			flyway.setValidateOnMigrate(false);
			flyway.setDataSource(database, username, password);
			flyway.setLocations("classpath:jhi.gatekeeper.server.util.databasemigration");
			flyway.setBaselineOnMigrate(true);
			flyway.migrate();
			flyway.repair();
		}
		catch (FlywayException e)
		{
			e.printStackTrace();
		}

		// Check if an admin user exists, if not, create it
		try (Connection conn = getConnection();
			 DSLContext context = DSL.using(conn, SQLDialect.MYSQL))
		{
			DatabaseSystems db = context.selectFrom(DATABASE_SYSTEMS)
										.where(DATABASE_SYSTEMS.SERVER_NAME.eq("--"))
										.and(DATABASE_SYSTEMS.SYSTEM_NAME.eq("gatekeeper"))
										.fetchOneInto(DatabaseSystems.class);

			UserTypes type = context.selectFrom(USER_TYPES)
									.where(USER_TYPES.DESCRIPTION.eq("Administrator"))
									.fetchOneInto(UserTypes.class);

			int adminCount = context.selectCount()
									.from(USERS)
									.leftJoin(USER_HAS_ACCESS_TO_DATABASES).on(USERS.ID.eq(USER_HAS_ACCESS_TO_DATABASES.USER_ID))
									.where(USER_HAS_ACCESS_TO_DATABASES.DATABASE_ID.eq(db.getId()))
									.and(USER_HAS_ACCESS_TO_DATABASES.USER_TYPE_ID.eq(type.getId()))
									.fetchOne(0, int.class);

			if (adminCount < 1)
			{
				UsersRecord admin = context.newRecord(USERS);
				admin.setUsername("admin");
				admin.setPassword(BCrypt.hashpw("password", BCrypt.gensalt(TokenResource.SALT)));
				admin.setFullName("The Admin");
				admin.setEmailAddress("--");
				admin.store();

				UserHasAccessToDatabasesRecord access = context.newRecord(USER_HAS_ACCESS_TO_DATABASES);
				access.setUserId(admin.getId());
				access.setDatabaseId(db.getId());
				access.setUserTypeId(type.getId());
				access.store();
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	public static Connection getConnection()
		throws SQLException
	{
		return DriverManager.getConnection(database, username, password);
	}
}

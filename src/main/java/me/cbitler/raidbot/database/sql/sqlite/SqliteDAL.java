package me.cbitler.raidbot.database.sql.sqlite;

import lombok.AccessLevel;
import lombok.Getter;
import me.cbitler.raidbot.database.RaidDao;
import me.cbitler.raidbot.database.ServerSettingsDao;
import me.cbitler.raidbot.database.UsersDao;
import me.cbitler.raidbot.database.UsersFlexRolesDao;
import me.cbitler.raidbot.database.sql.dao.RaidDaoImpl;
import me.cbitler.raidbot.database.sql.dao.ServerSettingsDaoImpl;
import me.cbitler.raidbot.database.sql.dao.UsersDaoImpl;
import me.cbitler.raidbot.database.sql.dao.UsersFlexRolesDaoImpl;
import me.cbitler.raidbot.database.sql.dbDAL;
import me.cbitler.raidbot.database.sql.tables.RaidTable;
import me.cbitler.raidbot.database.sql.tables.ServerSettingsTable;
import me.cbitler.raidbot.database.sql.tables.UserFlexRoleTable;
import me.cbitler.raidbot.database.sql.tables.UserTable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Getter
public class SqliteDAL implements dbDAL {
    private static SqliteDAL instance = null;

    private RaidDao raidDao;
    private ServerSettingsDao serverSettingsDao;
    private UsersDao usersDao;
    private UsersFlexRolesDao usersFlexRolesDao;
    @Getter(AccessLevel.NONE)
    private final Connection connection;

    public SqliteDAL() {
        connection = connect("GW2-raid-bot.db");
        initDatabaseTables(connection);

        initializeDao();
    }

    private void initializeDao() {
        raidDao = new RaidDaoImpl(connection);
        serverSettingsDao = new ServerSettingsDaoImpl(connection);
        usersDao = new UsersDaoImpl(connection);
        usersFlexRolesDao = new UsersFlexRolesDaoImpl(connection);
    }

    /**
     * Connect to the SQLite database and create the tables if they don't exist
     */
    private Connection connect(String databaseName) {
        String url = "jdbc:sqlite:" + databaseName;
        try {
            Connection connection;
            connection = DriverManager.getConnection(url);
            return connection;
        } catch (SQLException e) {
            System.out.println("SqliteDatabaseDAOImpl connection error");
            System.exit(1);
        }
        //This should be unreachable as the program will exit if the connection is able to be made
        return null;
    }

    /**
     * Create the database tables
     */
    private static void initDatabaseTables(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.addBatch(RaidTable.RAID_TABLE_CREATE);
            statement.addBatch(UserTable.USERS_TABLE_CREATE);
            statement.addBatch(UserFlexRoleTable.USERS_FLEX_ROLES_TABLE_CREATE);
            statement.addBatch(ServerSettingsTable.SERVER_SETTINGS_CREATE);

            statement.executeBatch();
        } catch (SQLException e) {
            System.out.println("Couldn't create tables");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

package me.cbitler.raidbot.database;

import me.cbitler.raidbot.database.sql.dbDAL;
import me.cbitler.raidbot.database.sql.postgres.PostgresDAL;
import me.cbitler.raidbot.database.sql.sqlite.SqliteDAL;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

public class UnitOfWork {
    private static dbDAL dbDAL = null;

    public static synchronized RaidDao getRaidDao() {
        return getDb().getRaidDao();
    }

    public static synchronized UsersDao getUsersDao() {
        return getDb().getUsersDao();
    }

    public static synchronized UsersFlexRolesDao getUsersFlexRolesDao() {
        return getDb().getUsersFlexRolesDao();

    }

    public static synchronized ServerSettingsDao getServerSettingsDao() {
        return getDb().getServerSettingsDao();


    }

    private static synchronized dbDAL getDb() {
        if (dbDAL == null) {
            System.out.println("UnitOfWork db == null");
            new UnitOfWork();
            return dbDAL;
        }
        return dbDAL;
    }

    public static synchronized void deleteRaid(String messageId) throws SQLException {
        getDb().getRaidDao().deleteRaid(messageId);
        getDb().getUsersDao().deleteRaid(messageId);
        getDb().getUsersFlexRolesDao().deleteRaid(messageId);
    }

    private UnitOfWork() {
        System.out.println("UnitOfWork ctr");
        String heroku = System.getenv("Heroku");
        if (heroku != null && heroku.equals("yes")) {
            System.out.println("You are using Heroku and therefore trying for postgres DB");
            try {
                URI dbUri = new URI(System.getenv("DATABASE_URL"));
                dbDAL = new PostgresDAL(dbUri);
                System.out.println("postgres dal assigned");
            } catch (URISyntaxException e) {
                System.out.println("bad shit crazy");
                e.printStackTrace();
            }
        } else {
            dbDAL = new SqliteDAL();
            System.out.println("sqlite dal assigned");
        }
    }
}

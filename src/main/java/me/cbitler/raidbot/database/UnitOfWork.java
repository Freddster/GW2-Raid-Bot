package me.cbitler.raidbot.database;

import me.cbitler.raidbot.database.sql.dbDAL;
import me.cbitler.raidbot.database.sql.postgres.PostgresDAL;
import me.cbitler.raidbot.database.sql.sqlite.SqliteDAL;

public class UnitOfWork {
    private static dbDAL dbDAL = null;

    public static synchronized dbDAL getDb() {
        System.out.println("UnitOfWork getDb");
        if (dbDAL == null) {
            System.out.println("UnitOfWork db == null");
            new UnitOfWork();
            return dbDAL;
        }
        return dbDAL;
    }

    private UnitOfWork() {
        System.out.println("UnitOfWork ctr");
        String heroku = System.getenv("Heroku");
        if (heroku != null && heroku.equals("yes")) {
            String dbUrl = System.getenv("JDBC_DATABASE_URL");
            dbDAL = new PostgresDAL(dbUrl);
            System.out.println("postgres dal assigned");
        } else {
            dbDAL = new SqliteDAL();
            System.out.println("sqlite dal assigned");
        }
    }
}

package me.cbitler.raidbot.database.sql.tables;

public class UserTable {

    private UserTable() {
    }

    public static final String TABLE_NAME = "raidUsers";
    public static final String RAID_ID = "raidId";
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String SPEC = "spec"; //TODO: RENAME
    public static final String ROLE = "role";

    public static final String USERS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
            + RAID_ID + " text, \n"
            + USER_ID + " text, \n"
            + USERNAME + " text, \n"
            + SPEC + " text, \n"
            + ROLE + " text)";
}

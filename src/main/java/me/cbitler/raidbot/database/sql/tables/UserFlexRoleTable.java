package me.cbitler.raidbot.database.sql.tables;

//TODO: Try to merge this into the user table by adding a flex column
public class UserFlexRoleTable {

    private UserFlexRoleTable() {
    }

    public static final String TABLE_NAME = "raidUsersFlexRoles";
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String SPEC = "spec"; //TODO: RENAME
    public static final String ROLE = "ROLE";
    public static final String RAID_ID = "raidId";

    public static final String USERS_FLEX_ROLES_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
            + RAID_ID + " text, \n"
            + USER_ID + " text, \n"
            + USERNAME + " text, \n"
            + SPEC + " text, \n"
            + ROLE + " text)";
}

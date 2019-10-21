package me.cbitler.raidbot.database.sql.tables;

public class RaidTable {

    private RaidTable() {
    }

    public static final String TABLE_NAME = "raids";
    public static final String RAID_ID = "raidId";
    public static final String SERVER_ID = "serverId";
    public static final String CHANNEL_ID = "channelId";
    public static final String IS_OPEN_WORLD = "isOpenWorld";
    public static final String LEADER = "leader";
    public static final String EVENT_NAME = "name";
    public static final String EVENT_DESCRIPTION = "description";
    public static final String EVENT_DATE = "date";
    public static final String EVENT_TIME = "time";
    public static final String ROLES = "roles";

    private static final String TEXT_NOT_NULL = " text NOT NULL";
    private static final String NEWLINE = ", \n";

    public static final String RAID_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
            + RAID_ID + " text PRIMARY KEY, \n"
            + SERVER_ID + TEXT_NOT_NULL + NEWLINE
            + CHANNEL_ID + TEXT_NOT_NULL + NEWLINE
            + IS_OPEN_WORLD + TEXT_NOT_NULL + NEWLINE
            + LEADER + TEXT_NOT_NULL + NEWLINE
            + EVENT_NAME + TEXT_NOT_NULL + NEWLINE
            + EVENT_DESCRIPTION + " text" + NEWLINE
            + EVENT_DATE + TEXT_NOT_NULL + NEWLINE
            + EVENT_TIME + TEXT_NOT_NULL + NEWLINE
            + ROLES + TEXT_NOT_NULL + ");";
}

package me.cbitler.raidbot.database.sql.tables;

public class ServerSettingsTable {

    private ServerSettingsTable() {
    }

    public static final String TABLE_NAME = "serverSettings";
    public static final String SERVER_ID = "serverId";
    public static final String RAID_LEADER_ROLE = "event_leader_role";

    public static final String SERVER_SETTINGS_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
            + SERVER_ID + " text PRIMARY KEY, \n"
            + RAID_LEADER_ROLE + " text)";
}

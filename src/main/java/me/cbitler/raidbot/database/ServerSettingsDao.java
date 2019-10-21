package me.cbitler.raidbot.database;

public interface ServerSettingsDao {
    void setEventLeaderRole(String id, String raidLeaderRole);

    String getEventLeaderRole(String id);
}

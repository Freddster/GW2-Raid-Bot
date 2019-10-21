package me.cbitler.raidbot.database;

import me.cbitler.raidbot.models.Raid;

import java.sql.SQLException;

public interface UsersFlexRolesDao {
    boolean addUserFlexRole(Raid raid, String id, String name, String spec, String role, boolean db_insert, boolean update_message);

    void deleteRaid(String messageId) throws SQLException;

    QueryResult getAllFlexUsers() throws SQLException;

    boolean removeUserFromFlexRoles(Raid raid, String id, String role, String spec);
}

package me.cbitler.raidbot.database;

import me.cbitler.raidbot.models.Raid;

import java.sql.SQLException;

public interface UsersDao {
    boolean addUser(Raid raid, String id, String name, String spec, String role, boolean db_insert, boolean update_message);

    void deleteRaid(String messageId) throws SQLException;

    QueryResult getAllUsers() throws SQLException;

    void removeUserFromMainRoles(Raid raid, String id);

    boolean removeUserFromRaid(Raid raid, String id);
}

package me.cbitler.raidbot.database.sql.dao;

import me.cbitler.raidbot.database.QueryResult;
import me.cbitler.raidbot.database.UsersDao;
import me.cbitler.raidbot.database.sql.tables.UserFlexRoleTable;
import me.cbitler.raidbot.models.FlexRole;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.models.RaidUser;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static me.cbitler.raidbot.database.sql.tables.UserTable.*;

public class UsersDaoImpl extends MessageUpdateFunctionality implements UsersDao {

    public UsersDaoImpl(Connection connection) {
        this.connection = connection;
    }

    /**
     * Add a user to this raid. This first creates the user and attempts to insert
     * it into the database, if needed Then it adds them to list of raid users with
     * their role
     *
     * @param id        The id of the user
     * @param name      The name of the user
     * @param spec      The specialization they are playing
     * @param role      The role they will be playing in the raid
     * @param db_insert Whether or not the user should be inserted. This is false
     *                  when the roles are loaded from the database.
     * @return true if the user was added, false otherwise
     */
    public boolean addUser(Raid raid, String id, String name, String spec, String role, boolean db_insert, boolean update_message) {
        RaidUser user = new RaidUser(id, name, spec, role);

        if (db_insert) {
            try {
                update("INSERT INTO " + TABLE_NAME + " (" + USER_ID + ", " + USERNAME + ", " + SPEC + ", " + ROLE + ", " + RAID_ID + ")"
                        + " VALUES (?,?,?,?,?)", new String[]{id, name, spec, role, raid.getMessageId()});
            } catch (SQLException e) {
                return false;
            }
        }

        raid.getUserToRole().put(user, role);
        raid.getUsersToFlexRoles().computeIfAbsent(new RaidUser(id, name, "", ""), k -> new ArrayList<FlexRole>());

        if (update_message) {
            updateMessage(raid);
        }
        return true;
    }

    public void deleteRaid(String messageId) throws SQLException {
        update("DELETE FROM " + TABLE_NAME + " WHERE " + RAID_ID + " = ?", new String[]{messageId});
    }

    public QueryResult getAllUsers() throws SQLException {
        return query("SELECT * FROM " + TABLE_NAME, new String[]{});
    }

    /**
     * Remove a user from their main role
     *
     * @param id The id of the user being removed
     */
    public void removeUserFromMainRoles(Raid raid, String id) {
        Iterator<Map.Entry<RaidUser, String>> users = raid.getUserToRole().entrySet().iterator();
        while (users.hasNext()) {
            Map.Entry<RaidUser, String> user = users.next();
            if (user.getKey().getId().equalsIgnoreCase(id)) {
                users.remove();
            }
        }

        try {
            update("DELETE FROM " + TABLE_NAME + " WHERE " + USER_ID + " = ? AND " + RAID_ID + " = ?", new String[]{id, raid.getMessageId()});
        } catch (SQLException e) {
            e.printStackTrace();
        }

        updateMessage(raid);
    }

    /**
     * Remove a user from this raid. This also updates the database to remove them
     * from the raid and any flex roles they are in
     *
     * @param id The user's id
     */
    public boolean removeUserFromRaid(Raid raid, String id) {
        boolean found = false;
        Iterator<Map.Entry<RaidUser, String>> users = raid.getUserToRole().entrySet().iterator();
        while (users.hasNext()) {
            Map.Entry<RaidUser, String> user = users.next();
            if (user.getKey().getId().equalsIgnoreCase(id)) {
                users.remove();
                found = true;
            }
        }

        Iterator<Map.Entry<RaidUser, List<FlexRole>>> usersFlex = raid.getUsersToFlexRoles().entrySet().iterator();
        while (usersFlex.hasNext()) {
            Map.Entry<RaidUser, List<FlexRole>> userFlex = usersFlex.next();
            if (userFlex.getKey().getId().equalsIgnoreCase(id)) {
                usersFlex.remove();
                found = true;
            }
        }

        try {
            update("DELETE FROM " + TABLE_NAME + " WHERE " + USER_ID + " = ? AND " + RAID_ID + " = ?", new String[]{id, raid.getMessageId()});
            update("DELETE FROM " + UserFlexRoleTable.TABLE_NAME + " WHERE " + UserFlexRoleTable.USER_ID + " = ? and " + UserFlexRoleTable.RAID_ID + " = ?", new String[]{id, raid.getMessageId()});
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (found)
            updateMessage(raid);

        return found;
    }
}

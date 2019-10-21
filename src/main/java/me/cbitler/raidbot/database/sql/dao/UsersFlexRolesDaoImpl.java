package me.cbitler.raidbot.database.sql.dao;

import me.cbitler.raidbot.database.QueryResult;
import me.cbitler.raidbot.database.UsersFlexRolesDao;
import me.cbitler.raidbot.models.FlexRole;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.models.RaidUser;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static me.cbitler.raidbot.database.sql.tables.UserFlexRoleTable.*;


public class UsersFlexRolesDaoImpl extends MessageUpdateFunctionality implements UsersFlexRolesDao {

    public UsersFlexRolesDaoImpl(Connection connection) {
        this.connection = connection;
    }

    /**
     * Add a user to a flex role in this raid. This first creates the user and
     * attempts to insert it into the database, if needed Then it adds them to list
     * of raid users' flex roles with their flex role
     *
     * @param id        The id of the user
     * @param name      The name of the user
     * @param spec      The specialization they are playing
     * @param role      The flex role they will be playing in the raid
     * @param db_insert Whether or not the user should be inserted. This is false
     *                  when the roles are loaded from the database.
     * @return true if the user was added, false otherwise
     */
    public boolean addUserFlexRole(Raid raid, String id, String name, String spec, String role, boolean db_insert, boolean update_message) {
        RaidUser user = new RaidUser(id, name, "", "");
        FlexRole flexRole = new FlexRole(spec, role);

        if (db_insert) {
            try {
                update("INSERT INTO `" + TABLE_NAME + "` (`" + USER_ID + "`, `" + USERNAME + "`, `" + SPEC + "`, `" + ROLE + "`, `" + RAID_ID + "`)"
                        + " VALUES (?,?,?,?,?)", new String[]{id, name, spec, role, raid.getMessageId()});
            } catch (Exception e) {
                return false;
            }
        }

        if (raid.getUsersToFlexRoles().get(user) == null) {
            raid.getUsersToFlexRoles().put(user, new ArrayList<FlexRole>());
        }

        raid.getUsersToFlexRoles().get(user).add(flexRole);
        if (update_message) {
            updateMessage(raid);
        }
        return true;
    }

    public void deleteRaid(String messageId) throws SQLException {
        update("DELETE FROM `" + TABLE_NAME + "` WHERE `" + RAID_ID + "` = ?", new String[]{messageId});
    }

    public QueryResult getAllFlexUsers() throws SQLException {
        return query("SELECT * FROM `" + TABLE_NAME + "`", new String[]{});
    }

    /**
     * Remove a user from their main role
     *
     * @param id   The id of the user being removed
     * @param role The role that should be removed
     * @param spec The class specialization that should be removed
     * @return true if user was signed up for this role and class, false otherwise
     */
    public boolean removeUserFromFlexRoles(Raid raid, String id, String role, String spec) {
        boolean found = false;
        Iterator<Map.Entry<RaidUser, List<FlexRole>>> users = raid.getUsersToFlexRoles().entrySet().iterator();
        while (users.hasNext()) {
            Map.Entry<RaidUser, List<FlexRole>> user = users.next();
            if (user.getKey().getId().equalsIgnoreCase(id)) {
                Iterator<FlexRole> froles = user.getValue().iterator();
                while (froles.hasNext()) {
                    FlexRole frole = froles.next();
                    if (frole.getSpec().equals(spec) && frole.getRole().equals(role)) {
                        froles.remove();
                        found = true;
                    }
                }
            }
        }

        try {
            update("DELETE FROM `" + TABLE_NAME + "` WHERE `" + USER_ID + "` = ? and `" + RAID_ID + "` = ? and `" + ROLE + "` = ? and `" + SPEC + "` = ?",
                    new String[]{id, raid.getMessageId(), role, spec});
        } catch (SQLException e) {
            e.printStackTrace();
        }

        updateMessage(raid);
        return found;
    }
}

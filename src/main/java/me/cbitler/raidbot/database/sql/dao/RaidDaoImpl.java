package me.cbitler.raidbot.database.sql.dao;

import me.cbitler.raidbot.database.QueryResult;
import me.cbitler.raidbot.database.RaidDao;
import me.cbitler.raidbot.database.sql.tables.UserFlexRoleTable;
import me.cbitler.raidbot.database.sql.tables.UserTable;
import me.cbitler.raidbot.models.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.cbitler.raidbot.database.sql.tables.RaidTable.*;
import static me.cbitler.raidbot.raids.RaidManager.formatRolesForDatabase;

public class RaidDaoImpl extends BaseFunctionality implements RaidDao {

    public RaidDaoImpl(Connection connection) {
        this.connection = connection;
    }

    /**
     * Add a new role to the event
     *
     * @param newrole new raid role
     * @return 0 success, 1 role exists, 2 SQL error
     */
    public int addRole(Raid raid, RaidRole newrole) {
        for (RaidRole role : raid.getRoles()) {
            if (role.getName().equalsIgnoreCase(newrole.getName())) {
                return ROLE_EXIST;
            }
        }
        raid.getRoles().add(newrole);

        return updateRaidRoles(raid);
    }

    /**
     * Change amount for a role of the event
     *
     * @param id        role
     * @param newAmount new amount for the role
     * @return 0 success, 1 number of users > new amount, 2 SQL error
     */
    public int changeAmountRole(Raid raid, int id, int newAmount) {
        String roleName = raid.getRoles().get(id).getName();
        int numberUsers = getUserNumberInRole(raid.getUserToRole(), roleName);
        if (newAmount < numberUsers) {
            return ROLE_EXIST;
        }

        raid.getRoles().get(id).setAmount(newAmount);

        // rename in database
        return updateRaidRoles(raid);
    }

    /**
     * Change flex only status of a role
     *
     * @param id        role
     * @param newStatus new amount for the role
     * @return 0 success, 1 number of users > 0 when enabling flexOnly, 2 SQL error
     */
    public int changeFlexOnlyRole(Raid raid, int id, boolean newStatus) {
        String roleName = raid.getRoles().get(id).getName();
        int numberUsers = getUserNumberInRole(raid.getUserToRole(), roleName);
        if (0 < numberUsers) {
            return ROLE_EXIST;
        }

        raid.getRoles().get(id).setFlexOnly(newStatus);

        // rename in database
        return updateRaidRoles(raid);
    }

    public void deleteRaid(String messageId) throws SQLException {
        update("DELETE FROM `" + TABLE_NAME + "` WHERE `" + RAID_ID + "` = ?", new String[]{messageId});
    }

    /**
     * Delete a role from the event
     *
     * @param id role
     * @return 0 success, 1 number of users > 0, 2 SQL error
     */
    public int deleteRole(Raid raid, int id) {
        String roleName = raid.getRoles().get(id).getName();
        int numberUsers = getUserNumberInRole(raid.getUserToRole(), roleName);
        int numberUsersFlex = getUserNumberInFlexRole(raid.getUsersToFlexRoles(), roleName);

        if (numberUsers > 0 || numberUsersFlex > 0) {
            return ROLE_EXIST;
        }

        raid.getRoles().remove(id);

        // delete in database
        return updateRaidRoles(raid);
    }

    public QueryResult getAllRaids() throws SQLException {
        return query("SELECT * FROM `" + TABLE_NAME + "`", new String[]{});
    }

    /**
     * Insert a raid into the database
     *
     * @param raid      The raid to insert
     * @param messageId The embedded message / 'raidId'
     * @param serverId  The serverId related to this raid
     * @param channelId The channelId for the announcement of this raid
     * @return True if inserted, false otherwise
     */
    public boolean insertToDatabase(PendingRaid raid, String messageId, String serverId, String channelId) {
        String roles = formatRolesForDatabase(raid.getRolesWithNumbers());

        try {
            update("INSERT INTO `" + TABLE_NAME + "` (`" + RAID_ID + "`, `" + SERVER_ID + "`, `" + CHANNEL_ID + "`, `" + IS_OPEN_WORLD + "`, `" + LEADER + "`, `" + EVENT_NAME + "`, `" + EVENT_DESCRIPTION + "`, `" + EVENT_DATE + "`, `" + EVENT_TIME + "`, `" + ROLES + "`) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    new String[]{messageId, serverId, channelId, Boolean.toString(raid.isOpenWorld()),
                            raid.getLeaderName(), raid.getName(), raid.getDescription(), raid.getDate(), raid.getTime(),
                            roles});
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Rename a role of the event
     *
     * @param roleId  role
     * @param newName new name for the role
     * @return 0 success, 1 role exists, 2 SQL error
     */
    public int renameRole(Raid raid, int roleId, String newName) {
        for (RaidRole role : raid.getRoles()) {
            if (role.getName().equalsIgnoreCase(newName)) {
                return ROLE_EXIST;
            }
        }
        String oldName = raid.getRoles().get(roleId).getName();
        raid.getRoles().get(roleId).setName(newName);

        // iterate over users' roles and rename
        for (Map.Entry<RaidUser, String> user : raid.getUserToRole().entrySet()) {
            if (user.getValue().equals(oldName))
                user.setValue(newName);
        }
        for (Map.Entry<RaidUser, List<FlexRole>> flex : raid.getUsersToFlexRoles().entrySet()) {
            for (FlexRole frole : flex.getValue()) {
                if (frole.getRole().equals(oldName))
                    frole.setRole(newName);
            }
        }

        // rename in database
        String rolesString = formatRolesForDatabase(raid.getRoles());
        try {
            update("UPDATE `" + TABLE_NAME + "` SET `" + ROLES + "`=? WHERE `" + RAID_ID + "`=?",
                    new String[]{rolesString, raid.getMessageId()});
            update("UPDATE `" + UserTable.TABLE_NAME + "` SET `" + UserTable.ROLE + "`=? WHERE `" + UserTable.ROLE + "`=? AND `" + UserTable.RAID_ID + "`=?",
                    new String[]{newName, oldName, raid.getMessageId()});
            update("UPDATE `" + UserFlexRoleTable.TABLE_NAME + "` SET `" + UserFlexRoleTable.ROLE + "`=? WHERE `" + UserFlexRoleTable.ROLE + "`=? AND `" + UserFlexRoleTable.RAID_ID + "`=?",
                    new String[]{newName, oldName, raid.getMessageId()});

            return ROLE_ADDED;
        } catch (SQLException e) {
            e.printStackTrace();
            return ROLE_ADD_DB_ERROR;
        }
    }

    /**
     * Updates the date of the raid in the database
     */
    public boolean updateDateDB(Raid raid) {
        try {
            update("UPDATE `" + TABLE_NAME + "` SET `" + EVENT_DATE + "`=? WHERE `" + RAID_ID + "`=?",
                    new String[]{raid.getDate(), raid.getMessageId()});
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Updates the description of the raid in the database
     */
    public boolean updateDescriptionDB(Raid raid) {
        try {
            update("UPDATE `" + TABLE_NAME + "` SET `" + EVENT_DESCRIPTION + "`=? WHERE `" + RAID_ID + "`=?",
                    new String[]{raid.getDescription(), raid.getMessageId()});
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Updates the leader of the raid in the database
     */
    public boolean updateLeaderDB(Raid raid) {
        try {
            update("UPDATE `" + TABLE_NAME + "` SET `" + LEADER + "`=? WHERE `" + RAID_ID + "`=?",
                    new String[]{raid.getRaidLeaderName(), raid.getMessageId()});
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Updates the name of the raid in the database
     */
    public boolean updateNameDB(Raid raid) {
        try {
            update("UPDATE `" + TABLE_NAME + "` SET `" + EVENT_NAME + "`=? WHERE `" + RAID_ID + "`=?",
                    new String[]{raid.getName(), raid.getMessageId()});
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Updates the time of the raid in the database
     */
    public boolean updateTimeDB(Raid raid) {
        try {
            update("UPDATE `" + TABLE_NAME + "` SET `" + EVENT_TIME + "`=? WHERE `" + RAID_ID + "`=?",
                    new String[]{raid.getTime(), raid.getMessageId()});
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * Get the number of users in a role
     *
     * @param role The name of the role
     * @return The number of users in the role
     */
    private int getUserNumberInRole(HashMap<RaidUser, String> userToRole, String role) {
        int inRole = 0;
        for (Map.Entry<RaidUser, String> entry : userToRole.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(role))
                inRole += 1;
        }

        return inRole;
    }

    private int updateRaidRoles(Raid raid) {
        String rolesString = formatRolesForDatabase(raid.getRoles());
        try {
            update("UPDATE `" + TABLE_NAME + "` SET `" + ROLES + "`=? WHERE `" + RAID_ID + "`=?",
                    new String[]{rolesString, raid.getMessageId()});
            return ROLE_ADDED;
        } catch (SQLException e) {
            e.printStackTrace();
            return ROLE_ADD_DB_ERROR;
        }
    }

    /**
     * Get the number of users in a flex role
     *
     * @param role The name of the role
     * @return The number of users in the role
     */
    private int getUserNumberInFlexRole(HashMap<RaidUser, List<FlexRole>> usersToFlexRoles, String role) {
        int inRole = 0;
        for (Map.Entry<RaidUser, List<FlexRole>> flex : usersToFlexRoles.entrySet()) {
            if (flex.getKey() != null) {
                for (FlexRole flexRole : flex.getValue()) {
                    if (flexRole.getRole().equalsIgnoreCase(role))
                        inRole++;
                }
            }
        }

        return inRole;
    }
}

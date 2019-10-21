package me.cbitler.raidbot.database.sql.dao;

import me.cbitler.raidbot.database.QueryResult;
import me.cbitler.raidbot.database.ServerSettingsDao;
import me.cbitler.raidbot.database.sql.tables.ServerSettingsTable;

import java.sql.Connection;
import java.sql.SQLException;


public class ServerSettingsDaoImpl extends BaseFunctionality implements ServerSettingsDao {
    public ServerSettingsDaoImpl(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get the raid leader role for a specific server.
     * This works by caching the role once it's retrieved once, and returning the default if a server hasn't set one.
     *
     * @param serverId the ID of the server
     * @return The name of the role that is considered the raid leader for that server
     */
    public String getEventLeaderRole(String serverId) {
        try {
            QueryResult results = query("SELECT `" + ServerSettingsTable.RAID_LEADER_ROLE + "` FROM `" + ServerSettingsTable.TABLE_NAME + "` WHERE `" + ServerSettingsTable.SERVER_ID + "` = ?", new String[]{serverId});

            if (results.getResults().next()) {
                return results.getResults().getString(ServerSettingsTable.RAID_LEADER_ROLE);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set the raid leader role for a server. This also updates it in SQLite
     *
     * @param serverId The server ID
     * @param role     The role name
     */
    public void setEventLeaderRole(String serverId, String role) {
        try {
            update("INSERT INTO `" + ServerSettingsTable.TABLE_NAME + "` (`" + ServerSettingsTable.SERVER_ID + "`,`" + ServerSettingsTable.RAID_LEADER_ROLE + "`) VALUES (?,?)",
                    new String[]{serverId, role});
        } catch (SQLException e) {
            //TODO: There is probably a much better way of doing this
            try {
                update("UPDATE `" + ServerSettingsTable.TABLE_NAME + "` SET `" + ServerSettingsTable.RAID_LEADER_ROLE + "` = ? WHERE `" + ServerSettingsTable.SERVER_ID + "` = ?",
                        new String[]{role, serverId});
            } catch (SQLException e1) {
                // Not much we can do if there is also an insert error
            }
        }
    }
}

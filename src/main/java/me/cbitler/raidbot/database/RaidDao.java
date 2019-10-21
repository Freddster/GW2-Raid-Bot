package me.cbitler.raidbot.database;

import me.cbitler.raidbot.models.PendingRaid;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.models.RaidRole;

import java.sql.SQLException;

public interface RaidDao {
    int ROLE_ADDED = 0;
    int ROLE_EXIST = 1;
    int ROLE_ADD_DB_ERROR = 2;

    int addRole(Raid raid, RaidRole raidRole);

    int changeAmountRole(Raid raid, int roleID, int newAmount);

    int changeFlexOnlyRole(Raid raid, int roleId, boolean newStatus);

    void deleteRaid(String messageId) throws SQLException;

    int deleteRole(Raid raid, int roleId);

    QueryResult getAllRaids() throws SQLException;

    boolean insertToDatabase(PendingRaid raid, String id, String serverId, String channelId);

    int renameRole(Raid raid, int roleId, String newName);

    boolean updateDateDB(Raid raid);

    boolean updateDescriptionDB(Raid raid);

    boolean updateLeaderDB(Raid raid);

    boolean updateNameDB(Raid raid);

    boolean updateTimeDB(Raid raid);
}

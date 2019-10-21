package me.cbitler.raidbot.database.sql;

import me.cbitler.raidbot.database.RaidDao;
import me.cbitler.raidbot.database.ServerSettingsDao;
import me.cbitler.raidbot.database.UsersDao;
import me.cbitler.raidbot.database.UsersFlexRolesDao;

public interface dbDAL {
    RaidDao getRaidDao();

    UsersDao getUsersDao();

    UsersFlexRolesDao getUsersFlexRolesDao();

    ServerSettingsDao getServerSettingsDao();
}

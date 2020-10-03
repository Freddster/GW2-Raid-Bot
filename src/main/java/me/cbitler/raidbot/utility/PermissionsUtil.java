package me.cbitler.raidbot.utility;

import me.cbitler.raidbot.database.UnitOfWork;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

/**
 * Utility class for handling permissions
 *
 * @author Christopher Bitler
 */
public class PermissionsUtil {
    private PermissionsUtil() {
    }

    /**
     * Check to see if a member has the raid leader role
     *
     * @param member The member to check
     * @return True if they have the role, false if they don't
     */
    public static boolean hasEventLeaderRole(Member member) {
        String eventLeaderRole = UnitOfWork.getServerSettingsDao().getEventLeaderRole(member.getGuild().getId());
        if (eventLeaderRole != null) {
            for (Role role : member.getRoles()) {
                if (role.getName().equalsIgnoreCase(eventLeaderRole)) {
                    return true;
                }
            }
        }
        return false;
    }
}

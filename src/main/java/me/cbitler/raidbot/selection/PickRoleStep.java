package me.cbitler.raidbot.selection;

import me.cbitler.raidbot.database.UnitOfWork;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.models.RaidRole;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

/**
 * Step for picking a role for a raid
 *
 * @author Christopher Bitler
 * @author Franziska Mueller
 */
public class PickRoleStep implements SelectionStep {
    Raid raid;
    User user;
    String spec;

    /**
     * Create a new step for this role selection with the specified raid and spec
     * that the user chose
     *
     * @param raid The raid
     * @param spec The specialization that the user chose
     */
    public PickRoleStep(Raid raid, String spec, User user) {
        this.raid = raid;
        this.spec = spec;
        this.user = user;
    }

    /**
     * Handle the user input - checks to see if the role they are picking is valid
     * and not full, and if so, adding them to that role
     *
     * @param e The private message event
     * @return True if the user chose a valid, not full, role, false otherwise
     */
    @Override
    public boolean handleDM(PrivateMessageReceivedEvent e) {
        boolean success = true;
        try {
            int roleId = Integer.parseInt(e.getMessage().getRawContent()) - 1;
            String roleName = raid.getRoles().get(roleId).getName();
            success = pickRole(e.getAuthor().getId(), e.getAuthor().getName(), roleName);
        } catch (Exception exp) {
            success = false;
        }

        return success;
    }

    /**
     * Get the next step - no next step here as this is a one step process
     *
     * @return null
     */
    @Override
    public SelectionStep getNextStep() {
        return null;
    }

    /**
     * The step text changes the text based on the available roles.
     *
     * @return The step text
     */
    @Override
    public String getStepText() {
        String text = "Pick a role:\n";
        for (int i = 0; i < raid.getRoles().size(); i++) {
            text += "`" + (i + 1) + "` " + raid.getRoles().get(i).getName() + "\n";
        }
        text += "or type *cancel* to cancel role selection.";

        return text;
    }

    /**
     * adds the user as the specified role
     *
     * @param userID   the user's id
     * @param username the user's name
     * @param roleName name of the role to be added
     * @return true if role was added, false otherwise
     */
    public boolean pickRole(String userID, String username, String roleName) {
        boolean success = true;

        if (raid.isValidRole(roleName)) {
            RaidRole role = raid.getRole(roleName);
            if (role.isFlexOnly()) {
                // there are no spots for the role to be a main role, so it can only be added as flex!
                UnitOfWork.getDb().getUsersFlexRolesDao().addUserFlexRole(raid, userID, username, spec, roleName, true, true);
                user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Added to event roster as flex role.").queue());
            } else if (raid.isUserInRaid(userID) == false) {
                // check if we can add it as main role
                if (raid.isValidNotFullRole(roleName)) {
                    UnitOfWork.getDb().getUsersDao().addUser(raid, userID, username, spec, roleName, true, true);
                    user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Added to event roster.").queue());
                } else {
                    // the role is already full
                    // check if we can add as flex role
                    if (raid.getUserNumFlexRoles(userID) < 2) {
                        UnitOfWork.getDb().getUsersFlexRolesDao().addUserFlexRole(raid, userID, username, spec, roleName, true, true);
                        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Added to event roster as flex role since the role you selected is full.").queue());
                    } else {
                        // role is full and user already has 2 flex roles
                        success = false;
                        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Could not add since the role you selected is full and you reached the maximum number of flex roles.\n"
                                + "Please choose a non-full role or write *cancel* to quit selection.").queue());
                    }
                }
            } else {
                // user has a main role already,
                // i.e., there has to be a flex role slot available
                // since we checked this in the ReactionHandler
                UnitOfWork.getDb().getUsersFlexRolesDao().addUserFlexRole(raid, userID, username, spec, roleName, true, true);
                user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Added to event roster as flex role.").queue());
            }
        } else {
            success = false;
            user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Please choose a valid role.").queue());
        }

        return success;
    }
}

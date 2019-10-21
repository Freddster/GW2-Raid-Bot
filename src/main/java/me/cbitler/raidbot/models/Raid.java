package me.cbitler.raidbot.models;

import lombok.Data;
import me.cbitler.raidbot.RaidBot;
import me.cbitler.raidbot.database.UnitOfWork;
import me.cbitler.raidbot.utility.Reactions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a raid and has methods for adding/removing users, user flex roles,
 * etc
 */
@Data
public class Raid {
    private String messageId;
    private String name;
    private String description;
    private String date;
    private String time;
    private String serverId;
    private String channelId;
    private String raidLeaderName;
    private List<RaidRole> roles = new ArrayList<>();
    private HashMap<RaidUser, String> userToRole = new HashMap<>();
    private HashMap<RaidUser, List<FlexRole>> usersToFlexRoles = new HashMap<>();

    /* *
     * open world events only have a single role (Participants) and users sign up without any class
     */
    private boolean isOpenWorld;

    /**
     * Create a new Raid with the specified data
     *
     * @param messageId      The embedded message Id related to this raid
     * @param serverId       The serverId that the raid is on
     * @param channelId      The announcement channel's id for this raid
     * @param raidLeaderName The name of the raid leader
     * @param name           The name of the raid
     * @param date           The date of the raid
     * @param time           The time of the raid
     */
    public Raid(String messageId, String serverId, String channelId, String raidLeaderName, String name,
            String description, String date, String time, boolean isOpenWorld) {
        this.messageId = messageId;
        this.serverId = serverId;
        this.channelId = channelId;
        this.raidLeaderName = raidLeaderName;
        this.name = name;
        this.description = description;
        this.date = date;
        this.time = time;
        this.isOpenWorld = isOpenWorld;
    }

    /**
     * Check if a specific role is valid, and whether or not it's full
     *
     * @param role The role to check
     * @return True if it is valid and not full, false otherwise
     */
    public boolean isValidNotFullRole(String role) {
        return this.isValidNotFullRole(role, false);
    }

    /**
     * Check if a specific role is valid, and whether or not it's full
     *
     * @param role The role to check
     * @return True if it is valid and not full, false otherwise
     */
    public boolean isValidNotFullRole(String role, boolean flex) {
        RaidRole r = getRole(role);

        if (r != null) {
            if(r.isFlexOnly() && !flex) return false;
            int max = r.getAmount();
            if (getUserNumberInRole(role) < max) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check to see if a role is valid
     *
     * @param role The role name
     * @return True if the role is valid, false otherwise
     */
    public boolean isValidRole(String role) {
        return getRole(role) != null;
    }

    /**
     * Get the object representing a role
     *
     * @param role The name of the role
     * @return The object representing the specified role
     */
    public RaidRole getRole(String role) {
        for (RaidRole r : roles) {
            if (r.getName().equalsIgnoreCase(role)) {
                return r;
            }
        }

        return null;
    }

    /**
     * Get the number of users in a role
     *
     * @param role The name of the role
     * @return The number of users in the role
     */
    private int getUserNumberInRole(String role) {
        int inRole = 0;
        for (Map.Entry<RaidUser, String> entry : userToRole.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(role))
                inRole += 1;
        }

        return inRole;
    }

    /**
     * Get list of users in a role
     *
     * @param role The name of the role
     * @return The users in the role
     */
    public List<RaidUser> getUsersInRole(String role) {
        List<RaidUser> users = new ArrayList<>();
        for (Map.Entry<RaidUser, String> entry : userToRole.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(role)) {
                users.add(entry.getKey());
            }
        }

        return users;
    }

    /**
     * Add a user to this open world event with the default role
     *
     * @param id        The id of the user
     * @param name      The name of the user
     * @return true if the user was added, false otherwise
     */
    public boolean addUserOpenWorld(Raid raid, String id, String name) {
        boolean success;

        String roleName = roles.get(0).getName();
        if (isValidNotFullRole(roleName)) // there is still space
            success = UnitOfWork.getDb().getUsersDao().addUser(raid, id, name, "", roleName, true, true);
        else
            success = UnitOfWork.getDb().getUsersFlexRolesDao().addUserFlexRole(raid, id, name, "", roleName, true, true);

        return success;
    }

    /**
     * Check if a specific user is in this raid (main roles)
     *
     * @param id The id of the user
     * @return True if they are in the raid, false otherwise
     */
    public boolean isUserInRaid(String id) {
        for (Map.Entry<RaidUser, String> entry : userToRole.entrySet()) {
            if (entry.getKey().getId().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send the dps report log links to the players in this raid
     *
     * @param logLinks The list of links
     */
    public void messagePlayersWithLogLinks(List<String> logLinks) {
        String logLinkMessage = "ArcDPS reports from **" + this.getName() + "**:\n";
        for (String link : logLinks) {
            logLinkMessage += (link + "\n");
        }

        final String finalLogLinkMessage = logLinkMessage;
        for (RaidUser user : this.userToRole.keySet()) {
            RaidBot.getInstance().getServer(this.serverId).getMemberById(user.getId()).getUser().openPrivateChannel()
                    .queue(privateChannel -> privateChannel.sendMessage(finalLogLinkMessage).queue());
        }
    }

    /**
     * Update the embedded message for the raid
     */
    public void updateMessage() {
        MessageEmbed embed = buildEmbed();
        try {
            RaidBot.getInstance().getServer(getServerId()).getTextChannelById(getChannelId())
                    .editMessageById(getMessageId(), embed).queue();
        } catch (Exception e) {
        }
    }

    /**
     * Build the embedded message that shows the information about this raid
     *
     * @return The embedded message representing this raid
     */
    private MessageEmbed buildEmbed() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(getName());
        builder.addField("Description:", getDescription(), false);
        builder.addBlankField(false);
        if (getRaidLeaderName() != null) {
            builder.addField("Leader: ", "**" + getRaidLeaderName() + "**", false);
        }
        builder.addBlankField(false);
        builder.addField("Date: ", getDate(), true);
        builder.addField("Time: ", getTime(), true);
        builder.addBlankField(false);
        builder.addField("Roles:", buildRolesText(), true);
        builder.addField("Flex Roles:", buildFlexRolesText(), true);
        builder.addBlankField(false);
        builder.addField("ID: ", messageId, false);

        return builder.build();
    }

    /**
     * Build the flex roles text, which includes a list of flex roles users are
     * playing and their specs
     *
     * @return The flex role text
     */
    private String buildFlexRolesText() {
        String text = "";
        if (isOpenWorld) {
            for (Map.Entry<RaidUser, List<FlexRole>> flex : usersToFlexRoles.entrySet()) {
                if (flex.getKey() != null && flex.getValue().isEmpty() == false)
                    text += ("- " + flex.getKey().getName() + "\n");
            }
        } else {
            // collect names and specializations for each role
            Map<String, List<RaidUser>> flexUsersByRole = new HashMap<String, List<RaidUser>>();
            for (int r = 0; r < roles.size(); r++) {
                flexUsersByRole.put(roles.get(r).getName(), new ArrayList<RaidUser>());
            }

            for (Map.Entry<RaidUser, List<FlexRole>> flex : usersToFlexRoles.entrySet()) {
                if (flex.getKey() != null) {
                    for (FlexRole frole : flex.getValue()) {
                        flexUsersByRole.get(frole.getRole()).add(new RaidUser(flex.getKey().getId(), flex.getKey().getName(), frole.getSpec(), null));
                    }
                }
            }
            for (int r = 0; r < roles.size(); r++) {
                String roleName = roles.get(r).getName();
                text += (roleName + ": \n");

                for (RaidUser user : flexUsersByRole.get(roleName)) {
                        Emote userEmote = Reactions.getEmoteByName(user.getSpec());
                        if(userEmote == null)
                            text += ("- " + user.getName() + " (" + user.getSpec() + ")\n");
                        else
                            text += ("<:"+userEmote.getName()+":"+userEmote.getId()+"> " + user.getName() + " (" + user.getSpec() + ")\n");
                }
                text += "\n";
            }
        }

        return text;
    }

    /**
     * Build the role text, which shows the roles users are playing in the raids
     *
     * @return The role text
     */
    private String buildRolesText() {
        String text = "";
        for (RaidRole role : roles) {
            if(role.isFlexOnly()) continue;
            List<RaidUser> raidUsersInRole = getUsersInRole(role.getName());
            text += ("**" + role.getName() + " ( " + raidUsersInRole.size() + " / " + role.getAmount() + " ):** \n");
            for (RaidUser user : raidUsersInRole) {
                if (isOpenWorld) {
                    text += ("- " + user.getName() + "\n");
                } else {
                    Emote userEmote = Reactions.getEmoteByName(user.getSpec());
                    if(userEmote == null)
                        text += "   - " + user.getName() + " (" + user.getSpec() + ")\n";
                    else
                        text += "   <:"+userEmote.getName()+":"+userEmote.getId()+"> " + user.getName() + " (" + user.getSpec() + ")\n";
                }
            }
            text += "\n";
        }
        return text;
    }

    /**
     * Get a List of RaidUsers from main roles in this raid by their ID
     *
     * @param id The user's ID
     * @return The List of RaidUsers if they are in this raid, null otherwise
     */
    public ArrayList<RaidUser> getRaidUsersById(String id) {
        ArrayList<RaidUser> raidUsers = new ArrayList<RaidUser>();
        for (RaidUser user : userToRole.keySet()) {
            if (user.getId().equalsIgnoreCase(id)) {
                raidUsers.add(user);
            }
        }
        return raidUsers;
    }

    /**
     * Get a List of RaidUsers from flex roles in this raid by their ID
     *
     * @param id The user's ID
     * @return The List of RaidUsers if they are in this raid, null otherwise
     */
    public ArrayList<FlexRole> getRaidUsersFlexRolesById(String id) {
        ArrayList<FlexRole> raidRoles = new ArrayList<FlexRole>();
        for (RaidUser user : usersToFlexRoles.keySet()) {
            if (user.getId().equalsIgnoreCase(id)) {
                for(FlexRole fRole : usersToFlexRoles.get(user)){
                    raidRoles.add(fRole);
                }
            }
        }
        return raidRoles;
    }

    /**
     * Remove a user by their username
     *
     * @param raid
     * @param name The name of the user being removed
     */
    public void removeUserByName(Raid raid, String name) {
        String idToRemove = "";
        for (Map.Entry<RaidUser, String> entry : userToRole.entrySet()) {
            if (entry.getKey().getName().equalsIgnoreCase(name)) {
                idToRemove = entry.getKey().getId();
                break;
            }
        }
        if (idToRemove.isEmpty()) { // did not find the user in main roles, check flex roles
            for (Map.Entry<RaidUser, List<FlexRole>> entry : usersToFlexRoles.entrySet()) {
                if (entry.getKey().getName().equalsIgnoreCase(name)) {
                    idToRemove = entry.getKey().getId();
                    break;
                }
            }
        }

        UnitOfWork.getDb().getUsersDao().removeUserFromRaid(raid, idToRemove);
    }

    /**
     * Get the number of flex roles a user has
     *
     * @param id The id of the user
     * @return The number of flex roles that a user has
     */
    public int getUserNumFlexRoles(String id) {
        for (Map.Entry<RaidUser, List<FlexRole>> entry : usersToFlexRoles.entrySet()) {
            RaidUser user = entry.getKey();
            if (user != null && user.getId() != null) {
                if (user.getId().equalsIgnoreCase(id)) {
                    return entry.getValue().size();
                }
            }
        }
        return 0;
    }
}

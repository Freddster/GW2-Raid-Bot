package me.cbitler.raidbot.database.sql.dao;

import me.cbitler.raidbot.RaidBot;
import me.cbitler.raidbot.models.FlexRole;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.models.RaidRole;
import me.cbitler.raidbot.models.RaidUser;
import me.cbitler.raidbot.utility.Reactions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageUpdateFunctionality extends BaseFunctionality {

    /**
     * Update the embedded message for the raid
     */
    protected void updateMessage(Raid raid) {
        MessageEmbed embed = buildEmbed(raid);
        try {
            RaidBot.getInstance().getServer(raid.getServerId()).getTextChannelById(raid.getChannelId())
                    .editMessageById(raid.getMessageId(), embed).queue();
        } catch (Exception e) {
        }
    }

    /**
     * Build the embedded message that shows the information about this raid
     *
     * @return The embedded message representing this raid
     */
    private MessageEmbed buildEmbed(Raid raid) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(raid.getName());
        builder.addField("Description:", raid.getDescription(), false);
        builder.addBlankField(false);
        if (raid.getRaidLeaderName() != null) {
            builder.addField("Leader: ", "**" + raid.getRaidLeaderName() + "**", false);
        }
        builder.addBlankField(false);
        builder.addField("Date: ", raid.getDate(), true);
        builder.addField("Time: ", raid.getTime(), true);
        builder.addBlankField(false);
        builder.addField("Roles:", buildRolesText(raid), true);
        builder.addField("Flex Roles:", buildFlexRolesText(raid), true);
        builder.addBlankField(false);
        builder.addField("ID: ", raid.getMessageId(), false);

        return builder.build();
    }

    /**
     * Build the role text, which shows the roles users are playing in the raids
     *
     * @param raid
     * @return The role text
     */
    private String buildRolesText(Raid raid) {
        String text = "";
        for (RaidRole role : raid.getRoles()) {
            if (role.isFlexOnly()) continue;
            List<RaidUser> raidUsersInRole = raid.getUsersInRole(role.getName());
            text += ("**" + role.getName() + " ( " + raidUsersInRole.size() + " / " + role.getAmount() + " ):** \n");
            for (RaidUser user : raidUsersInRole) {
                if (raid.isOpenWorld()) {
                    text += ("- " + user.getName() + "\n");
                } else {
                    Emote userEmote = Reactions.getEmoteByName(user.getSpec());
                    if (userEmote == null)
                        text += "   - " + user.getName() + " (" + user.getSpec() + ")\n";
                    else
                        text += "   <:" + userEmote.getName() + ":" + userEmote.getId() + "> " + user.getName() + " (" + user.getSpec() + ")\n";
                }
            }
            text += "\n";
        }
        return text;
    }

    /**
     * Build the flex roles text, which includes a list of flex roles users are
     * playing and their specs
     *
     * @param raid
     * @return The flex role text
     */
    private String buildFlexRolesText(Raid raid) {
        String text = "";
        if (raid.isOpenWorld()) {
            for (Map.Entry<RaidUser, List<FlexRole>> flex : raid.getUsersToFlexRoles().entrySet()) {
                if (flex.getKey() != null && flex.getValue().isEmpty() == false)
                    text += ("- " + flex.getKey().getName() + "\n");
            }
        } else {
            // collect names and specializations for each role
            Map<String, List<RaidUser>> flexUsersByRole = new HashMap<String, List<RaidUser>>();
            for (int r = 0; r < raid.getRoles().size(); r++) {
                flexUsersByRole.put(raid.getRoles().get(r).getName(), new ArrayList<RaidUser>());
            }

            for (Map.Entry<RaidUser, List<FlexRole>> flex : raid.getUsersToFlexRoles().entrySet()) {
                if (flex.getKey() != null) {
                    for (FlexRole frole : flex.getValue()) {
                        flexUsersByRole.get(frole.getRole()).add(new RaidUser(flex.getKey().getId(), flex.getKey().getName(), frole.getSpec(), null));
                    }
                }
            }
            for (int r = 0; r < raid.getRoles().size(); r++) {
                String roleName = raid.getRoles().get(r).getName();
                text += (roleName + ": \n");

                for (RaidUser user : flexUsersByRole.get(roleName)) {
                    Emote userEmote = Reactions.getEmoteByName(user.getSpec());
                    if (userEmote == null)
                        text += ("- " + user.getName() + " (" + user.getSpec() + ")\n");
                    else
                        text += ("<:" + userEmote.getName() + ":" + userEmote.getId() + "> " + user.getName() + " (" + user.getSpec() + ")\n");
                }
                text += "\n";
            }
        }

        return text;
    }
}

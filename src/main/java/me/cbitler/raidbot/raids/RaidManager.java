package me.cbitler.raidbot.raids;

import me.cbitler.raidbot.RaidBot;
import me.cbitler.raidbot.database.QueryResult;
import me.cbitler.raidbot.database.UnitOfWork;
import me.cbitler.raidbot.models.PendingRaid;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.models.RaidRole;
import me.cbitler.raidbot.utility.Reactions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Serves as a manager for all of the raids. This includes creating, loading,
 * and deleting raids
 *
 * @author Christopher Bitler
 * @author Franziska Mueller
 */
public class RaidManager {
    private RaidManager() {
    }

    private static List<Raid> raids = new ArrayList<>();

    /**
     * Create a raid. This turns a PendingRaid object into a Raid object and inserts
     * it into the list of raids. It also sends the associated embedded message and
     * adds the reactions for people to join to the embed
     *
     * @param raid The pending raid to create
     */
    public static void createRaid(PendingRaid raid) {
        MessageEmbed message = buildEmbed(raid);

        Guild guild = RaidBot.getInstance().getServer(raid.getServerId());
        List<TextChannel> channels = guild.getTextChannelsByName(raid.getAnnouncementChannel(), true);
        if (!channels.isEmpty()) {
            // We always go with the first channel if there is more than one
            try {
                channels.get(0).sendMessage(message).queue(message1 -> {
                    boolean inserted = UnitOfWork.getRaidDao().insertToDatabase(raid, message1.getId(), message1.getGuild().getId(),
                            message1.getChannel().getId());
                    if (inserted) {
                        Raid newRaid = new Raid(message1.getId(), message1.getGuild().getId(),
                                message1.getChannel().getId(), raid.getLeaderName(), raid.getName(),
                                raid.getDescription(), raid.getDate(), raid.getTime(), raid.isOpenWorld());
                        newRaid.getRoles().addAll(raid.getRolesWithNumbers());
//                        raids.add(newRaid);
                        newRaid.updateMessage();

                        List<Emote> emoteList;
                        if (newRaid.isOpenWorld())
                            emoteList = Reactions.getOpenWorldEmotes();
                        else
                            emoteList = Reactions.getCoreClassEmotes();
                        for (Emote emote : emoteList)
                            message1.addReaction(emote).queue();
                    } else {
                        message1.delete().queue();
                    }
                });
            } catch (Exception e) {
                System.out.println("Error encountered in sending message.");
                e.printStackTrace();
                throw e;
            }
        }
    }

    //TODO: FIGURE OUT WHAT THIS METHOD DOES, AND MOVE OR DELETE IT

    /**
     * Load raids This first queries all of the raids and loads the raid data and
     * adds the raids to the raid list Then, it queries the raid users and inserts
     * them into their relevant raids, updating the embedded messages Finally, it
     * queries the raid users' flex roles and inserts those to the raids
     */
    public static void loadRaids() {
        try {
            QueryResult results = UnitOfWork.getDb().getRaidDao().getAllRaids();
            while (results.getResults().next()) {
                //TODO: USE NAMES FROM TABLE
                String name = results.getResults().getString("name");
                String description = results.getResults().getString("description");
                if (description == null) {
                    description = "N/A";
                }
                String date = results.getResults().getString("date");
                String time = results.getResults().getString("time");
                String rolesText = results.getResults().getString("roles");
                String messageId = results.getResults().getString("raidId");
                String serverId = results.getResults().getString("serverId");
                String channelId = results.getResults().getString("channelId");

                String leaderName = null;
                try {
                    leaderName = results.getResults().getString("leader");
                } catch (Exception e) {
                }

                boolean isOpenWorld = false;
                try {
                    isOpenWorld = results.getResults().getString("isOpenWorld").equals("true");
                } catch (Exception e) {
                }

                Raid raid = new Raid(messageId, serverId, channelId, leaderName, name, description, date, time,
                        isOpenWorld);
                String[] roleSplit = rolesText.split(";");
                for (String roleAndAmount : roleSplit) {
                    String[] parts = roleAndAmount.split(":");
                    int amnt = Integer.parseInt(parts[0]);
                    String role = parts[1];
                    raid.getRoles().add(new RaidRole(amnt, role));
                }
                raids.add(raid);
            }
            results.getResults().close();
            results.getStmt().close();

            QueryResult userResults = UnitOfWork.getDb().getUsersDao().getAllUsers();

            while (userResults.getResults().next()) {
                //TODO: FIX DUPLICATE STUFF
                //TODO: USE NAMES FROM TABLES
                String id = userResults.getResults().getString("userId");
                String name = userResults.getResults().getString("username");
                String spec = userResults.getResults().getString("spec");
                String role = userResults.getResults().getString("role");
                String raidId = userResults.getResults().getString("raidId");

                Raid raid = RaidManager.getRaid(raidId);
                if (raid != null) {
                    UnitOfWork.getUsersDao().addUser(raid, id, name, spec, role, false, false);
                }
            }

            QueryResult userFlexRolesResults = UnitOfWork.getUsersFlexRolesDao().getAllFlexUsers();

            while (userFlexRolesResults.getResults().next()) {
                String id = userFlexRolesResults.getResults().getString("userId");
                String name = userFlexRolesResults.getResults().getString("username");
                String spec = userFlexRolesResults.getResults().getString("spec");
                String role = userFlexRolesResults.getResults().getString("role");
                String raidId = userFlexRolesResults.getResults().getString("raidId");

                Raid raid = RaidManager.getRaid(raidId);
                if (raid != null) {
                    UnitOfWork.getUsersFlexRolesDao().addUserFlexRole(raid, id, name, spec, role, false, false);
                }
            }


//            for (Raid raid : raids) {
//                raid.updateMessage();
//            }
            for (Raid raid : UnitOfWork.getRaidDao().getAllRaids()) {
                raid.updateMessage();
            }
        } catch (SQLException e) {
            System.out.println("Couldn't load events... exiting.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Delete the raid from the database and maps, and delete the message if it is
     * still there
     *
     * @param messageId The raid ID
     * @return true if deleted, false if not deleted
     */
    public static boolean deleteRaid(String messageId) {
        Raid r = getRaid(messageId);
        if (r != null) {
            // try {
            // RaidBot.getInstance().getServer(r.getServerId())
            // .getTextChannelById(r.getChannelId()).getMessageById(messageId).queue(message
            // -> message.delete().queue());
            // } catch (Exception e) {
            // // Nothing, the message doesn't exist - it can happen
            // }

            Iterator<Raid> raidIterator = raids.iterator();
            while (raidIterator.hasNext()) {
                Raid raid = raidIterator.next();
                if (raid.getMessageId().equalsIgnoreCase(messageId)) {
                    raidIterator.remove();
                }
            }

            try {
                UnitOfWork.getDb().getRaidDao().deleteRaid(messageId);
                UnitOfWork.getDb().getUsersDao().deleteRaid(messageId);
                UnitOfWork.getDb().getUsersFlexRolesDao().deleteRaid(messageId);
            } catch (Exception e) {
                System.out.println("Error encountered deleting event.");
            }

            return true;
        }

        return false;
    }

    /**
     * Get a raid from the discord message ID
     *
     * @param messageId The discord message ID associated with the raid's embedded
     *                  message
     * @return The raid object related to that messageId, if it exist.
     */
    public static Raid getRaid(String messageId) {
        for (Raid raid : raids) {
            if (raid.getMessageId().equalsIgnoreCase(messageId)) {
                return raid;
            }
        }
        return null;
    }

    /**
     * Formats the roles associated with a raid in a form that can be inserted into
     * a database row. This combines them as [number]:[name];[number]:[name];...
     *
     * @param rolesWithNumbers The roles and their amounts
     * @return The formatted string
     */
    public static String formatRolesForDatabase(List<RaidRole> rolesWithNumbers) {
        String data = "";

        for (int i = 0; i < rolesWithNumbers.size(); i++) {
            RaidRole role = rolesWithNumbers.get(i);
            String roleName = role.getName();
            if (role.isFlexOnly()) roleName = "!" + roleName;
            if (i == rolesWithNumbers.size() - 1) {
                data += (role.getAmount() + ":" + roleName);
            } else {
                data += (role.getAmount() + ":" + roleName + ";");
            }
        }

        return data;
    }

    /**
     * Create a message embed to show the raid
     *
     * @param raid The raid object
     * @return The embedded message
     */
    private static MessageEmbed buildEmbed(PendingRaid raid) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(raid.getName());
        builder.addField("Description:", raid.getDescription(), false);
        builder.addBlankField(false);
        if (raid.getLeaderName() != null) {
            builder.addField("Leader: ", "**" + raid.getLeaderName() + "**", false);
        }
        builder.addBlankField(false);
        builder.addField("Date: ", raid.getDate(), true);
        builder.addField("Time: ", raid.getTime(), true);
        builder.addBlankField(false);
        builder.addField("Roles: ", buildRolesText(raid), true);
        builder.addField("Flex Roles: ", buildFlexRolesText(raid), true);
        builder.addBlankField(false);
        return builder.build();
    }

    /**
     * Builds the text to go into the roles field in the embedded message
     *
     * @param raid The raid object
     * @return The role text
     */
    private static String buildRolesText(PendingRaid raid) {
        String text = "";
        for (RaidRole role : raid.getRolesWithNumbers()) {
            if (role.isFlexOnly()) continue;
            text += ("**" + role.getName() + ":**\n");
        }
        return text;
    }

    /**
     * Build the flex role text. This is blank here as we have no flex roles at this
     * point.
     *
     * @param raid
     * @return The flex roles text (blank here)
     */
    private static String buildFlexRolesText(PendingRaid raid) {
        String text = "";
        for (RaidRole role : raid.getRolesWithNumbers()) {
            if (role.isFlexOnly()) text += ("**" + role.getName() + " (" + role.getAmount() + "):**\n");
        }
        return text;
    }
}

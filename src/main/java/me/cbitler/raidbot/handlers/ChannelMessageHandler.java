package me.cbitler.raidbot.handlers;

import me.cbitler.raidbot.RaidBot;
import me.cbitler.raidbot.commands.Command;
import me.cbitler.raidbot.commands.CommandRegistry;
import me.cbitler.raidbot.creation.CreationStep;
import me.cbitler.raidbot.creation.RunNameStep;
import me.cbitler.raidbot.database.UnitOfWork;
import me.cbitler.raidbot.edit.EditIdleStep;
import me.cbitler.raidbot.edit.EditStep;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.raids.RaidManager;
import me.cbitler.raidbot.utility.PermissionsUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Handle channel message-related events sent to the bot
 *
 * @author Christopher Bitler
 * @author Franziska Mueller
 */
public class ChannelMessageHandler extends ListenerAdapter {

    /**
     * Handle receiving a message. This checks to see if it matches the !createEvent or !removeFromEvent commands
     * and acts on them accordingly.
     *
     * @param e The event
     */
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        RaidBot bot = RaidBot.getInstance();
        if (e.getAuthor().isBot()) {
            return;
        }

        if (e.getMessage().getRawContent().startsWith(CommandRegistry.CMD_PREFIX)) {
            String[] messageParts = e.getMessage().getRawContent().split(" ");
            String[] arguments = CommandRegistry.getArguments(messageParts);
            Command command = CommandRegistry.getCommand(messageParts[0].replace(CommandRegistry.CMD_PREFIX, ""));
            if (command != null) {
                command.handleCommand(messageParts[0], arguments, e.getChannel(), e.getAuthor());

                try {
                    e.getMessage().delete().queue();
                } catch (Exception exception) {
                }
            }
        }

        if (PermissionsUtil.hasEventLeaderRole(e.getMember())) {
            if (e.getMessage().getRawContent().equalsIgnoreCase(CommandRegistry.CMD_PREFIX + CommandRegistry.CREATE_EVENT_COMMAND)) {
                // check if this user is already editing or creating
                if (bot.getCreationMap().get(e.getAuthor().getId()) != null) {
                    e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You cannot create two events at the same time. Finish the creation process first.").queue());
                } else if (bot.getEditMap().get(e.getAuthor().getId()) != null) {
                    e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You cannot create an event while editing. Finish editing the event first.").queue());
                } else {
                    CreationStep runNameStep = new RunNameStep(e.getMessage().getGuild().getId());
                    e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(runNameStep.getStepText()).queue());
                    bot.getCreationMap().put(e.getAuthor().getId(), runNameStep);
                }
                try {
                    e.getMessage().delete().queue();
                } catch (Exception exception) {
                }
            } else if (e.getMessage().getRawContent().toLowerCase().startsWith(CommandRegistry.CMD_PREFIX + "removefromevent")) {
                String[] split = e.getMessage().getRawContent().split(" ");
                if (split.length < 3) {
                    e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Format for " + CommandRegistry.CMD_PREFIX + "removeFromEvent: " + CommandRegistry.CMD_PREFIX + "removeFromEvent [event id] [name]").queue());
                } else {
                    String messageId = split[1];
                    String name = split[2];

                    Raid raid = RaidManager.getRaid(messageId);

                    if (raid != null && raid.getServerId().equalsIgnoreCase(e.getGuild().getId())) {
                        raid.removeUserByName(raid, name);
                    } else {
                        e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Non-existant event.").queue());
                    }
                }
                try {
                    e.getMessage().delete().queue();
                } catch (Exception exception) {
                }
            } else if (e.getMessage().getRawContent().toLowerCase().startsWith(CommandRegistry.CMD_PREFIX + CommandRegistry.EDIT_EVENT_COMMAND)) {
                String[] split = e.getMessage().getRawContent().split(" ");
                if (split.length < 2) {
                    e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Format for " + CommandRegistry.CMD_PREFIX + CommandRegistry.EDIT_EVENT_COMMAND + ": " + CommandRegistry.CMD_PREFIX + CommandRegistry.EDIT_EVENT_COMMAND + " [event id]").queue());
                } else {
                    String messageId = split[1];

                    Raid raid = RaidManager.getRaid(messageId);

                    if (raid != null && raid.getServerId().equalsIgnoreCase(e.getGuild().getId())) {
                        // check if this user is already editing or creating, or the raid is being edited by someone else
                        if (bot.getCreationMap().get(e.getAuthor().getId()) != null) {
                            e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You cannot edit an event while creating.").queue());
                        } else if (bot.getEditMap().get(e.getAuthor().getId()) != null) {
                            e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You can only edit one event at a time.").queue());
                        } else if (bot.getEditList().contains(messageId)) {
                            e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("The selected event is already being edited.").queue());
                        } else {
                            // start editing process
                            EditStep editIdleStep = new EditIdleStep(messageId);
                            e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(editIdleStep.getStepText()).queue());
                            bot.getEditMap().put(e.getAuthor().getId(), editIdleStep);
                            bot.getEditList().add(messageId);
                        }
                    } else {
                        e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Non-existant event.").queue());
                    }
                }
                try {
                    e.getMessage().delete().queue();
                } catch (Exception exception) {
                }

            }
        }
//        else {
//            if (!e.getMember().getPermissions().contains(Permission.MANAGE_SERVER)) {
//                e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You do not have permissions to manage events").queue());
//            }
//        }

        if (e.getMember().getPermissions().contains(Permission.MANAGE_SERVER) &&
                (e.getMessage().getRawContent().toLowerCase().startsWith(CommandRegistry.CMD_PREFIX + CommandRegistry.SET_EVENT_MANAGER_ROLE_COMMAND))) {
            String[] commandParts = e.getMessage().getRawContent().split(" ");
            String raidLeaderRole = combineArguments(commandParts, 1);
            UnitOfWork.getServerSettingsDao().setEventLeaderRole(e.getMember().getGuild().getId(), raidLeaderRole);
            e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Event manager role updated to: " + raidLeaderRole).queue());
            e.getMessage().delete().queue();
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent e) {
        if (RaidManager.getRaid(e.getMessageId()) != null) {
            RaidManager.deleteRaid(e.getMessageId());
        }
    }

    /**
     * Combine the strings in an array of strings
     *
     * @param parts  The array of strings
     * @param offset The offset in the array to start at
     * @return The combined string
     */
    private String combineArguments(String[] parts, int offset) {
        String text = "";
        for (int i = offset; i < parts.length; i++) {
            text += (parts[i] + " ");
        }

        return text.trim();
    }
}

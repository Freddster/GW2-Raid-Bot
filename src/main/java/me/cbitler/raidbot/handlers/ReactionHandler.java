package me.cbitler.raidbot.handlers;

import me.cbitler.raidbot.RaidBot;
import me.cbitler.raidbot.database.UnitOfWork;
import me.cbitler.raidbot.deselection.DeselectIdleStep;
import me.cbitler.raidbot.deselection.DeselectionStep;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.raids.RaidManager;
import me.cbitler.raidbot.selection.PickSpecStep;
import me.cbitler.raidbot.selection.SelectionStep;
import me.cbitler.raidbot.utility.Reactions;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ReactionHandler extends ListenerAdapter {
    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent e) {
        Raid raid = RaidManager.getRaid(e.getMessageId());
        if (e.getUser().isBot()) {
            return;
        }
        if (raid != null) {
            Emote emote = e.getReactionEmote().getEmote();
            if (emote != null) {
                if (!raid.isOpenWorld()) {
                    if (Reactions.getSpecs().contains(emote.getName())) {
                        RaidBot bot = RaidBot.getInstance();
                        // check if the user is already selecting a role
                        if (bot.getRoleSelectionMap().get(e.getUser().getId()) == null) {
                            // check if the user can select a role, i.e. not main + 2 flex roles yet
                            if (raid.isUserInRaid(e.getUser().getId()) && raid.getUserNumFlexRoles(e.getUser().getId()) >= 2) {
                                e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You have selected the maximum number of roles. Press the X reaction to re-select your roles.").queue());
                            } else {
                                SelectionStep step = new PickSpecStep(raid, e.getReactionEmote().getEmote().getName(), e.getUser());
                                bot.getRoleSelectionMap().put(e.getUser().getId(), step);
                                e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(step.getStepText()).queue());
                            }
                        } else {
                            e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You are already selecting a role.").queue());
                        }
                    } else if (emote.getName().equalsIgnoreCase("X_")) {
                        if (raid.isUserInRaid(e.getUser().getId()) || raid.getUserNumFlexRoles(e.getUser().getId()) > 0) {
                            DeselectionStep step = new DeselectIdleStep(raid);
                            RaidBot.getInstance().getRoleDeselectionMap().put(e.getUser().getId(), step);
                            e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(step.getStepText()).queue());
                        } else {
                            e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You are not signed up for this event.").queue());
                        }
                    }
                } else { // open world event
                    if (emote.getName().equalsIgnoreCase("Check")) {
                        if (raid.isUserInRaid(e.getUser().getId()) || raid.getUserNumFlexRoles(e.getUser().getId()) > 0) { // already signed up
                            e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You are already signed up for this event.").queue());
                        } else {
                            if (raid.addUserOpenWorld(raid, e.getUser().getId(), e.getUser().getName())) {
                                e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Successfully signed up for the event.").queue());
                            } else {
                                e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Sign-up failed.").queue());
                            }
                        }
                    } else if (emote.getName().equalsIgnoreCase("X_")) {
                        boolean removedSuccessfully = UnitOfWork.getDb().getUsersDao().removeUserFromRaid(raid, e.getUser().getId());
                        if (removedSuccessfully) {
                            e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Successfully removed from event.").queue());
                        } else {
                            e.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("You are not signed up for this event.").queue());
                        }
                    }
                }
            }
            e.getReaction().removeReaction(e.getUser()).queue();
        }
    }
}
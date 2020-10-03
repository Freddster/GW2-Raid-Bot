package me.cbitler.raidbot.commands;

import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.raids.RaidManager;
import me.cbitler.raidbot.utility.PermissionsUtil;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EndEventCommand implements Command {
    public static final String END_EVENT_COMMAND = "endEvent";

    @Override
    public void handleCommand(String command, String[] args, TextChannel channel, User author) {
        Member member = channel.getGuild().getMember(author);
        if(PermissionsUtil.hasEventLeaderRole(member) && args.length >= 1) {
                String raidId = args[0];
                Raid raid = RaidManager.getRaid(raidId);
                if (raid != null && raid.getServerId().equalsIgnoreCase(channel.getGuild().getId())) {
                    //Get list of log messages and send them
                    if (args.length > 1) {
                        List<String> links = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

                        raid.messagePlayersWithLogLinks(links);
                    }

                    boolean deleted = RaidManager.deleteRaid(raidId);

                    sendEventDeletedMessage(author, deleted);
                } else {
                    author.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("That event doesn't exist on this server.").queue());
                }
            }
    }

    private void sendEventDeletedMessage(User author, boolean deleted) {
        if (deleted) {
            author.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Event ended").queue());
        } else {
            author.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("An error occured ending the event.").queue());
        }
    }
}

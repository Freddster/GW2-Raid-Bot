package me.cbitler.raidbot.commands;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class InfoCommand implements Command {
    public static final String INFO_COMMAND = "info";

    @Override
    public void handleCommand(String command, String[] args, TextChannel channel, User author) {
        String information = "Riz-GW2-Event-Bot Information:\n" +
                "Authors: J8-ET#1337, Raika-Sternensucher#6392\n" +
                "Based on GW2-Raid-Bot written by VoidWhisperer#5905\n";
        channel.sendMessage(information).queue();
    }
}

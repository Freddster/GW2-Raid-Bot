package me.cbitler.raidbot.edit;

import me.cbitler.raidbot.database.UnitOfWork;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.raids.RaidManager;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

/**
 * Edit the leader for the event
 *
 * @author Franziska Mueller
 */
public class EditLeaderStep implements EditStep {

    private String messageID;

    public EditLeaderStep(String messageId) {
        this.messageID = messageId;
    }

    /**
     * Handle changing the leader for the event
     *
     * @param e The direct message event
     * @return True if the leader is set, false otherwise
     */
    public boolean handleDM(PrivateMessageReceivedEvent e) {
        Raid raid = RaidManager.getRaid(messageID);
        raid.setRaidLeaderName(e.getMessage().getRawContent());
        if (UnitOfWork.getDb().getRaidDao().updateLeaderDB(raid)) {
            e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Leader successfully updated in database.").queue());
        } else {
            e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Leader could not be updated in database.").queue());
        }
        raid.updateMessage();

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String getStepText() {
        return "Enter the new leader for the event:";
    }

    /**
     * {@inheritDoc}
     */
    public EditStep getNextStep() {
        return new EditIdleStep(messageID);
    }

    @Override
    public String getMessageID() {
        return messageID;
    }
}

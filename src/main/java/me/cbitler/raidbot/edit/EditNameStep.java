package me.cbitler.raidbot.edit;

import me.cbitler.raidbot.database.UnitOfWork;
import me.cbitler.raidbot.models.Raid;
import me.cbitler.raidbot.raids.RaidManager;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

/**
 * Edit the name for the event
 *
 * @author Franziska Mueller
 */
public class EditNameStep implements EditStep {

    private String messageID;

    public EditNameStep(String messageId) {
        this.messageID = messageId;
    }

    /**
     * Handle changing the name for the event
     *
     * @param e The direct message event
     * @return True if the name is set, false otherwise
     */
    public boolean handleDM(PrivateMessageReceivedEvent e) {
        Raid raid = RaidManager.getRaid(messageID);
        raid.setName(e.getMessage().getRawContent());
        if (UnitOfWork.getDb().getRaidDao().updateNameDB(raid)) {
            e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Name successfully updated in database.").queue());
        } else {
            e.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Name could not be updated in database.").queue());
        }
        raid.updateMessage();

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String getStepText() {
        return "Enter the new name for the event:";
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

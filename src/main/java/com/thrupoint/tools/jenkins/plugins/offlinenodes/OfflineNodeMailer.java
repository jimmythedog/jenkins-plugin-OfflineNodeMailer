package com.thrupoint.tools.jenkins.plugins.offlinenodes;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.User;
import hudson.slaves.OfflineCause;

import java.io.IOException;

import javax.mail.MessagingException;

import jenkins.model.Jenkins;

/**
 * Periodically checks if nodes are offline, and mails a reminder to the user
 * who took each node offline
 */
@Extension
public class OfflineNodeMailer extends AsyncPeriodicWork
{
    private static final long POLL_INTERVAL_HOURS = 2;
    private static final Jenkins JENKINS = Jenkins.getInstance();

    protected OfflineNodeMailer(String name)
    {
        super(name);
    }

    @Override
    public long getRecurrencePeriod()
    {
        return POLL_INTERVAL_HOURS * 60 * 1000; // mSecs
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException
    {
        for (Computer slave : JENKINS.getComputers())
        {
            if (slave.isOffline())
            {
                OfflineCause offlineCause = slave.getOfflineCause();
                if (offlineCause instanceof OfflineCause.SimpleOfflineCause)
                {
                    final User takenOfflineBy = getTakenOfflineBy(offlineCause);
                    if (takenOfflineBy != null)
                    {
                        try
                        {
                            Emailer.mailOfflineComputerReminderToUser(takenOfflineBy, slave);
                        } catch (MessagingException e)
                        {
                            throw new IOException("Unable to send an email reminder", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the {@link User} who took the computer offline
     * 
     * @param offlineCause
     *            The cause of the computer being taken offline
     * @return The user who took the computer offline, or <code>null</code> if
     *         the user cannot be determined
     */
    private User getTakenOfflineBy(OfflineCause offlineCause)
    {
        // this is all a hack, as it looks like we cannot get the user object -
        // of course, this won't cope with i18n!
        String reason = ((OfflineCause.SimpleOfflineCause) offlineCause).toString();
        if (reason.startsWith("Disconnected by ")) // "Disconnected by xxxx : dah-de-dah..."
        {
            String userid = reason.split(" ")[2];
            return User.get(userid, false); // Don't create a user!
        }
        return null;
    }

}

package com.thrupoint.tools.jenkins.plugins.offlinenodes;

import hudson.model.Computer;
import hudson.model.User;
import hudson.tasks.Mailer;
import hudson.tasks.Mailer.UserProperty;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;

final class Emailer
{
    private Emailer()
    {
        //Utility class
    }
    
    /**
     * Mail a reminder to the user that they have taken a computer offline
     * 
     * @param recipient
     *            The user to email
     * @param offlineComputer
     *            The offline computer
     * @throws MessagingException If the email could not be sent
     */
    static void mailOfflineComputerReminderToUser(User recipient, Computer offlineComputer) throws MessagingException
    {
        UserProperty mailerUserProperty = recipient.getProperty(Mailer.UserProperty.class);
        if (mailerUserProperty != null)
        {
            String emailAddress = mailerUserProperty.getAddress();
            String computerName = offlineComputer.getDisplayName();
            MimeMessage emailMessage = createOfflineComputerEmail(emailAddress, computerName);
            Transport.send(emailMessage);
        }
    }

    private static MimeMessage createOfflineComputerEmail(String recipient, String computerName)
            throws MessagingException
    {
        MimeMessage msg = createEmptyMail(recipient);

        msg.setSubject(Messages.getString("Emailer.subject")); //$NON-NLS-1$

        StringBuilder msgBody = new StringBuilder();
        msgBody.append(String.format(Messages.getString("Emailer.body1"), computerName)); //$NON-NLS-1$
        msgBody.append(String.format(Messages.getString("Emailer.body2"))); //$NON-NLS-1$
        msgBody.append(String.format(Messages.getString("Emailer.body3"))); //$NON-NLS-1$
        msgBody.append(String.format(Messages.getString("Emailer.body4"))); //$NON-NLS-1$
        msgBody.append(String.format(Messages.getString("Emailer.body5"))); //$NON-NLS-1$
        msg.setText(msgBody.toString());

        return msg;
    }

    private static MimeMessage createEmptyMail(String address) throws MessagingException
    {
        MimeMessage msg = new MimeMessage(Mailer.descriptor().createSession());
        msg.setContent("", "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
        msg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
        msg.setSentDate(new Date());

        String replyTo = Mailer.descriptor().getReplyToAddress();
        if (StringUtils.isNotBlank(replyTo))
        {
            msg.setHeader("Reply-To", replyTo); //$NON-NLS-1$
        }

        Set<InternetAddress> rcp = new LinkedHashSet<InternetAddress>();
        String defaultSuffix = Mailer.descriptor().getDefaultSuffix();

        // if not a valid address (i.e. no '@'), then try adding suffix
        if (!address.contains("@") && defaultSuffix != null && defaultSuffix.contains("@")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            address += defaultSuffix;
        }

        rcp.add(new InternetAddress(address));

        msg.setRecipients(Message.RecipientType.TO, rcp.toArray(new InternetAddress[rcp.size()]));

        return msg;
    }
}

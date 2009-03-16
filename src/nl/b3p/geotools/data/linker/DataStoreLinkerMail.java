package nl.b3p.geotools.data.linker;

import java.text.SimpleDateFormat;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;

/**
 *
 * @author Gertjan Al, B3Partners
 */
public class DataStoreLinkerMail {

    public static void mail(Properties batch, String message) {
        String smtpServer = DataStoreLinker.getSaveProp(batch, "log.mail.smtp.host", "");
        String from = DataStoreLinker.getSaveProp(batch, "log.mail.from", "ETL DataStoreLinker Logger <noreply@b3partners.nl>");
        String to = DataStoreLinker.getSaveProp(batch, "log.mail.to", "");
        String subject = DataStoreLinker.getSaveProp(batch, "log.mail.subject", "Fout tijdens ETL proces");

        mail(smtpServer, to, from, subject, message);
    }

    public static void mail(Properties batch, String subject, String message) {
        String smtpServer = DataStoreLinker.getSaveProp(batch, "log.mail.smtp.host", "");
        String from = DataStoreLinker.getSaveProp(batch, "log.mail.from", "ETL DataStoreLinker Logger <noreply@b3partners.nl>");
        String to = DataStoreLinker.getSaveProp(batch, "log.mail.to", "");

        mail(smtpServer, to, from, subject, message);
    }

    private static void mail(String smtpServer, String to, String from, String subject, String body) {
        if (!(smtpServer).equals("") && !body.equals("")) {
            try {
                Properties props = System.getProperties();
                //Attaching to default Session, or we could start a new one
                props.put("mail.smtp.host", smtpServer);
                Session session = Session.getDefaultInstance(props, null);

                //Create a new message
                Message msg = new MimeMessage(session);

                //Set the FROM and TO fields
                msg.setFrom(new InternetAddress(from));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));

                //Set the subject and body text
                msg.setSubject(subject);
                msg.setText(body);

                //Set some other header information
                msg.setHeader("X-Mailer", "B3Partners");
                msg.setSentDate(new Date());

                //Send the message
                Transport.send(msg);
            } catch (Exception ex) {
            }
        }
    }
}

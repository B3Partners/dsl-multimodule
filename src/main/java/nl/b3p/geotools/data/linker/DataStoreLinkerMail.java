package nl.b3p.geotools.data.linker;

import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Gertjan Al, B3Partners
 */
public class DataStoreLinkerMail {
    private static final Log log = LogFactory.getLog(DataStoreLinkerMail.class);
    
    public static void mail(Properties batch, String message) throws Exception {
        String smtpServer = DataStoreLinker.getSaveProp(batch, "log.mail.smtp.host", "");
        String from = DataStoreLinker.getSaveProp(batch, "log.mail.from", "ETL DataStoreLinker Logger <noreply@b3partners.nl>");
        String to = DataStoreLinker.getSaveProp(batch, "log.mail.to", "");
        String subject = DataStoreLinker.getSaveProp(batch, "log.mail.subject", "Fout tijdens ETL proces");

        mail(smtpServer, to, from, subject, message);
    }

    public static void mail(Properties batch, String subject, String message) throws Exception {
        String smtpServer = DataStoreLinker.getSaveProp(batch, "log.mail.smtp.host", "");
        String from = DataStoreLinker.getSaveProp(batch, "log.mail.from", "ETL DataStoreLinker Logger <noreply@b3partners.nl>");
        String to = DataStoreLinker.getSaveProp(batch, "log.mail.to", "");

        mail(smtpServer, to, from, subject, message);
    }

    public static void mail(nl.b3p.datastorelinker.entity.Process process, String message) throws Exception {
        mail(process, message, process.getMail().getSubject() + ": \"" + process.getName() + "\"");
    }

    public static void mail(nl.b3p.datastorelinker.entity.Process process, String message, String subject) throws Exception {
        String smtpServer = process.getMail().getSmtpHost();
        // TODO: defaults in een localization file zetten
        String from = process.getMail().getFromEmailAddress();
        String to = process.getMail().getToEmailAddress();

        mail(smtpServer, to, from, subject, message);
    }

    private static void mail(String smtpServer, String to, String from, String subject, String body) throws Exception {
        log.debug("Sending process complete mail.");
        
        if ( (from == null || from.isEmpty() )
                && DataStoreLinker.DEFAULT_FROM!=null 
                && !DataStoreLinker.DEFAULT_FROM.isEmpty()) {
            from = DataStoreLinker.DEFAULT_FROM;
        }
        if ( (smtpServer == null || smtpServer.isEmpty()  )
                && DataStoreLinker.DEFAULT_SMTPHOST!=null 
                && !DataStoreLinker.DEFAULT_SMTPHOST.isEmpty()) {
            smtpServer = DataStoreLinker.DEFAULT_SMTPHOST;
        }
        if (smtpServer==null || smtpServer.isEmpty()) {
            throw new Exception("no smtp server!");
        }
        if (body==null || body.isEmpty()) {
            throw new Exception("nothing to send!");
        }
         
        Properties props = System.getProperties();
        //Attaching to default Session, or we could start a new one
        props.put("mail.smtp.host", smtpServer);
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(log.isDebugEnabled());
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
    }
}

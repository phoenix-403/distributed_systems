package app_kvServer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class Email {

    private int port = 587;
    private String smtpServer = "smtp.gmail.com";
    private String contentType = "text/html";

    private final String userid = "notifications.authority.ece419@gmail.com";//change accordingly
    private final String password = "nohakkopls";

    private Properties props;
    private Session mailSession;


    public Email() {

        props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.port", "587");
        props.put("mail.transport.protocol", "smtp");

        mailSession = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(userid, password);
                    }
                });
    }

    public void send(Logger logger, String to, String msg) throws MessagingException {


        MimeMessage message = new MimeMessage(mailSession);
        message.addFrom(InternetAddress.parse(userid));
        message.setRecipients(Message.RecipientType.TO, to);
        message.setSubject("");
        message.setContent(msg, contentType);

        Transport transport = mailSession.getTransport();
        try {
            logger.info("Sending email ....");
            transport.connect(smtpServer, port, userid, password);
            transport.sendMessage(message,
                    message.getRecipients(Message.RecipientType.TO));
            logger.info("Email sent");
        } catch (Exception e) {
            logger.error("Error Sending email: " + e.getMessage());
        }
        transport.close();
    }

    public static void main(String[] args) throws MessagingException {
        Email email = new Email();
        email.send(LogManager.getLogger(Email.class),"rotaneggaws@gmail.com", "henry!!!!");
    }
}
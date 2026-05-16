package com.eseo.steevejobs.service;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.net.URI;
import java.util.Properties;

public class MailService {


    public static void EnvoyerMail(String destinataire, String objet, String contenu) {
        try {
            Dotenv dotenv = Dotenv.load();
            String smtpUrl = dotenv.get("SMTP_URL");
            String expediteur = dotenv.get("EXPEDITEUR");

            URI uri = new URI(smtpUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            String usernameNAS = uri.getUserInfo().split(":")[0];
            String passwordNAS = uri.getUserInfo().split(":")[1];

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.starttls.required", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(usernameNAS, passwordNAS);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(expediteur));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(objet, "UTF-8");
            message.setContent(contenu, "text/plain; charset=UTF-8");

            Transport.send(message);
            System.out.println("Mail envoyé avec succès à " + destinataire);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erreur lors de l'envoi du mail.");
        }
    }

}

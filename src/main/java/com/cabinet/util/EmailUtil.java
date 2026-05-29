package com.cabinet.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Envoi d'emails de confirmation d'inscription patient via SMTP configurable.
 *
 * <p><b>Rôle :</b> unique point d'envoi d'email du système (code de confirmation).</p>
 * <p><b>Objectif :</b> valider l'adresse, vérifier la configuration {@link EmailConfig}
 * et retourner un {@link EmailSendResult} sans propager d'exception vers les servlets.</p>
 * <p><b>Place MVC :</b> utilitaire appelé par le servlet d'inscription après
 * {@link com.cabinet.service.AuthService} — ne touche pas directement la base MySQL.</p>
 *
 * @see EmailSendResult
 * @see EmailConfig
 * @since 1.0
 */
public final class EmailUtil {

    private static final Logger LOG = Logger.getLogger(EmailUtil.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Constructeur privé — classe utilitaire statique. */
    private EmailUtil() {
    }

    /**
     * Envoie les documents médicaux PDF en pièces jointes au patient.
     *
     * @param toEmail     adresse email du patient
     * @param pdfFiles    chemins absolus des fichiers PDF à joindre
     * @return résultat d'envoi SMTP
     */
    public static EmailSendResult sendMedicalDocuments(String toEmail, List<Path> pdfFiles) {
        String subject = "Vos documents médicaux";
        String body = "Bonjour,\n\n"
                + "Veuillez trouver vos documents médicaux en pièce jointe.\n"
                + "Ils sont aussi disponibles dans votre espace patient.\n\n"
                + "Cabinet Médical";

        if (!isValidEmail(toEmail)) {
            return EmailSendResult.failure("adresse_email_invalide");
        }
        if (pdfFiles == null || pdfFiles.isEmpty()) {
            return EmailSendResult.failure("aucun_fichier");
        }

        EmailConfig cfg = EmailConfig.get();
        if (!cfg.isEnabled()) {
            LOG.warning("Envoi documents médicaux ignoré : email.enabled=false");
            return EmailSendResult.skipped("smtp_desactive");
        }
        if (!cfg.isSmtpReady()) {
            return EmailSendResult.failure("smtp_non_configure");
        }

        try {
            sendMultipartViaSmtp(cfg, toEmail, subject, body, pdfFiles);
            LOG.info(() -> "Email documents médicaux envoyé à " + toEmail + " (" + pdfFiles.size() + " PJ)");
            return EmailSendResult.ok("envoye");
        } catch (MessagingException e) {
            LOG.log(Level.SEVERE, "Échec envoi documents médicaux vers " + toEmail, e);
            return EmailSendResult.failure("echec_smtp");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erreur envoi documents médicaux", e);
            return EmailSendResult.failure("erreur_envoi");
        }
    }

    public static EmailSendResult sendRegistrationConfirmation(String toEmail, String confirmationCode) {
        String subject = "Inscription – Confirmation de votre compte";
        String body = "Bonjour,\n\n"
                + "Votre inscription au cabinet médical a bien été enregistrée.\n"
                + "Votre code de confirmation est : " + confirmationCode
                + "\n\nVous pouvez vous connecter avec l'email utilisé lors de l'inscription.\n\n"
                + "Cabinet Médical";
        return sendConfirmationEmail(toEmail, confirmationCode, subject, body);
    }

    /**
     * Envoie le code OTP de confirmation de rendez-vous au patient.
     *
     * @param toEmail adresse email du patient connecté
     * @param otp     code à 6 chiffres
     * @return résultat d'envoi SMTP
     */
    public static EmailSendResult sendOtpForAppointment(String toEmail, String otp) {
        String subject = "Rendez-vous – Code de vérification";
        String body = "Bonjour,\n\n"
                + "Votre code pour confirmer votre rendez-vous est : " + otp
                + "\n\nCe code est valable pendant " + RdvOtpConstants.OTP_VALIDITY_MINUTES + " minutes.\n\n"
                + "Cabinet Médical";
        return sendConfirmationEmail(toEmail, otp, subject, body);
    }

    /**
     * Pipeline interne : validation, configuration SMTP et envoi Jakarta Mail.
     *
     * @param toEmail destinataire
     * @param confirmationCode code à journaliser en mode dev si échec
     * @param subject objet du message
     * @param body corps texte brut
     * @return {@link EmailSendResult} décrivant l'issue de l'opération
     */
    private static EmailSendResult sendConfirmationEmail(String toEmail, String confirmationCode,
                                                       String subject, String body) {
        LOG.info(() -> "Envoi email inscription — destinataire=" + toEmail + ", sujet=" + subject);

        if (!isValidEmail(toEmail)) {
            LOG.warning(() -> "Adresse email invalide : " + toEmail);
            return EmailSendResult.failure("adresse_email_invalide");
        }
        if (confirmationCode == null || confirmationCode.isBlank()) {
            LOG.warning("Code de confirmation manquant ou vide.");
            return EmailSendResult.failure("code_confirmation_manquant");
        }

        EmailConfig cfg = EmailConfig.get();

        if (!cfg.isEnabled()) {
            LOG.warning("Envoi SMTP ignoré : email.enabled=false");
            logCodeOnFailure(cfg, toEmail, confirmationCode, "SMTP désactivé (email.enabled=false)");
            return EmailSendResult.skipped("smtp_desactive");
        }

        if (!cfg.isSmtpReady()) {
            LOG.warning("SMTP activé mais configuration incomplète (user/password/from/host).");
            logCodeOnFailure(cfg, toEmail, confirmationCode, "configuration SMTP incomplète");
            return EmailSendResult.failure("smtp_non_configure");
        }

        LOG.info(() -> String.format(
                "Envoi SMTP — host=%s, port=%d, startTls=%s, auth=%s, to=%s",
                cfg.getHost(), cfg.getPort(), cfg.isStartTls(), cfg.isAuth(), toEmail));

        try {
            sendViaSmtp(cfg, toEmail, subject, body);
            LOG.info(() -> "Email de confirmation envoyé avec succès à " + toEmail);
            return EmailSendResult.ok("envoye");
        } catch (MessagingException e) {
            LOG.log(Level.SEVERE, "Échec envoi email vers " + toEmail + " : " + e.getMessage(), e);
            logCodeOnFailure(cfg, toEmail, confirmationCode, "échec SMTP");
            return EmailSendResult.failure("echec_smtp");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erreur inattendue envoi email vers " + toEmail, e);
            logCodeOnFailure(cfg, toEmail, confirmationCode, "erreur inattendue");
            return EmailSendResult.failure("erreur_envoi");
        }
    }

    /**
     * Construit la session Jakarta Mail et transmet le message via {@link Transport#send}.
     *
     * @param cfg configuration SMTP
     * @param toEmail destinataire
     * @param subject objet
     * @param body corps texte
     * @throws MessagingException en cas d'erreur protocole SMTP
     */
    private static void sendViaSmtp(EmailConfig cfg, String toEmail, String subject, String body)
            throws MessagingException {

        Properties mailProps = new Properties();
        mailProps.put("mail.transport.protocol", "smtp");
        mailProps.put("mail.smtp.host", cfg.getHost());
        mailProps.put("mail.smtp.port", String.valueOf(cfg.getPort()));
        mailProps.put("mail.smtp.auth", String.valueOf(cfg.isAuth()));
        mailProps.put("mail.smtp.starttls.enable", String.valueOf(cfg.isStartTls()));
        mailProps.put("mail.smtp.ssl.enable", String.valueOf(cfg.isSslEnable()));
        if (cfg.isStartTls()) {
            mailProps.put("mail.smtp.starttls.required", "true");
        }
        mailProps.put("mail.smtp.ssl.protocols", "TLSv1.2");
        mailProps.put("mail.smtp.ssl.trust", cfg.getHost());
        mailProps.put("mail.smtp.connectiontimeout", "15000");
        mailProps.put("mail.smtp.timeout", "15000");
        mailProps.put("mail.smtp.writetimeout", "15000");

        Session session;
        if (cfg.isAuth()) {
            session = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(cfg.getUser(), cfg.getPassword());
                }
            });
        } else {
            session = Session.getInstance(mailProps);
        }

        Message message = new MimeMessage(session);
        String from = cfg.getFrom();
        try {
            if (isNotBlank(cfg.getFromName())) {
                message.setFrom(new InternetAddress(from, cfg.getFromName(), "UTF-8"));
            } else {
                message.setFrom(new InternetAddress(from));
            }
        } catch (UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(from));
        }
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail.trim()));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    private static void sendMultipartViaSmtp(EmailConfig cfg, String toEmail, String subject,
                                             String body, List<Path> attachments) throws Exception {
        Properties mailProps = buildMailProperties(cfg);
        Session session = createMailSession(cfg, mailProps);

        MimeMessage message = new MimeMessage(session);
        setFromAddress(message, cfg);
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail.trim()));
        message.setSubject(subject, "UTF-8");

        MimeMultipart multipart = new MimeMultipart();

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body, "UTF-8");
        multipart.addBodyPart(textPart);

        for (Path file : attachments) {
            if (file == null || !Files.isRegularFile(file)) {
                continue;
            }
            MimeBodyPart attachment = new MimeBodyPart();
            attachment.attachFile(file.toFile());
            attachment.setFileName(file.getFileName().toString());
            multipart.addBodyPart(attachment);
        }

        message.setContent(multipart);
        Transport.send(message);
    }

    private static Properties buildMailProperties(EmailConfig cfg) {
        Properties mailProps = new Properties();
        mailProps.put("mail.transport.protocol", "smtp");
        mailProps.put("mail.smtp.host", cfg.getHost());
        mailProps.put("mail.smtp.port", String.valueOf(cfg.getPort()));
        mailProps.put("mail.smtp.auth", String.valueOf(cfg.isAuth()));
        mailProps.put("mail.smtp.starttls.enable", String.valueOf(cfg.isStartTls()));
        mailProps.put("mail.smtp.ssl.enable", String.valueOf(cfg.isSslEnable()));
        if (cfg.isStartTls()) {
            mailProps.put("mail.smtp.starttls.required", "true");
        }
        mailProps.put("mail.smtp.ssl.protocols", "TLSv1.2");
        mailProps.put("mail.smtp.ssl.trust", cfg.getHost());
        mailProps.put("mail.smtp.connectiontimeout", "15000");
        mailProps.put("mail.smtp.timeout", "15000");
        mailProps.put("mail.smtp.writetimeout", "15000");
        return mailProps;
    }

    private static Session createMailSession(EmailConfig cfg, Properties mailProps) {
        if (cfg.isAuth()) {
            return Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(cfg.getUser(), cfg.getPassword());
                }
            });
        }
        return Session.getInstance(mailProps);
    }

    private static void setFromAddress(Message message, EmailConfig cfg) throws MessagingException, UnsupportedEncodingException {
        String from = cfg.getFrom();
        if (isNotBlank(cfg.getFromName())) {
            message.setFrom(new InternetAddress(from, cfg.getFromName(), "UTF-8"));
        } else {
            message.setFrom(new InternetAddress(from));
        }
    }

    /**
     * Journalise le code OTP en développement lorsque l'envoi SMTP échoue ou est désactivé.
     *
     * @param cfg configuration (flag {@code email.log.otp.on.failure})
     * @param toEmail destinataire visé
     * @param code code de confirmation
     * @param reason motif journalisé
     */
    private static void logCodeOnFailure(EmailConfig cfg, String toEmail, String code, String reason) {
        if (cfg.isLogOtpOnFailure()) {
            LOG.info(() -> "[EMAIL-DEV] " + reason + " | destinataire=" + toEmail + " | code=" + code);
        }
    }

    /**
     * Valide le format d'une adresse email (expression régulière simplifiée).
     *
     * @param email adresse à tester
     * @return {@code true} si le format est accepté
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * @param s chaîne testée
     * @return {@code true} si non nulle et non vide
     */
    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}


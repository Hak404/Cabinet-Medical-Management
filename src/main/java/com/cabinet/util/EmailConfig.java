package com.cabinet.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration SMTP chargée une fois au démarrage (singleton).
 *
 * <p><b>Rôle :</b> centraliser les paramètres d'envoi d'email pour {@link EmailUtil}.</p>
 * <p><b>Objectif :</b> fusionner {@code email.properties}, {@code email.local.properties}
 * et les variables d'environnement (prioritaires sur les fichiers).</p>
 * <p><b>Place MVC :</b> infrastructure du package {@code util}, sans lien HTTP —
 * utilisée uniquement lors de l'inscription patient (servlet → {@link EmailUtil}).</p>
 *
 * @see EmailUtil
 * @since 1.0
 */
final class EmailConfig {

    private static final Logger LOG = Logger.getLogger(EmailConfig.class.getName());
    private static final EmailConfig INSTANCE = load();

    private final boolean enabled;
    private final boolean logOtpOnFailure;
    private final String host;
    private final int port;
    private final boolean auth;
    private final boolean startTls;
    private final boolean sslEnable;
    private final String user;
    private final String password;
    private final String from;
    private final String fromName;

    /**
     * Constructeur privé — instance immuable.
     */
    private EmailConfig(boolean enabled, boolean logOtpOnFailure, String host, int port,
                        boolean auth, boolean startTls, boolean sslEnable, String user, String password,
                        String from, String fromName) {
        this.enabled = enabled;
        this.logOtpOnFailure = logOtpOnFailure;
        this.host = host;
        this.port = port;
        this.auth = auth;
        this.startTls = startTls;
        this.sslEnable = sslEnable;
        this.user = user;
        this.password = password;
        this.from = from;
        this.fromName = fromName;
    }

    /**
     * @return instance unique chargée au démarrage de la JVM
     */
    static EmailConfig get() {
        return INSTANCE;
    }

    /**
     * @return {@code true} si l'envoi SMTP est activé ({@code email.enabled})
     */
    boolean isEnabled() {
        return enabled;
    }

    /**
     * @return {@code true} si le code OTP doit être journalisé en cas d'échec SMTP (mode dev)
     */
    boolean isLogOtpOnFailure() {
        return logOtpOnFailure;
    }

    /**
     * @return nom d'hôte du serveur SMTP
     */
    String getHost() {
        return host;
    }

    /**
     * @return port SMTP (ex. 587)
     */
    int getPort() {
        return port;
    }

    /**
     * @return {@code true} si l'authentification SMTP est requise
     */
    boolean isAuth() {
        return auth;
    }

    /**
     * @return {@code true} si STARTTLS est activé
     */
    boolean isStartTls() {
        return startTls;
    }

    /**
     * @return {@code true} si SSL direct est activé sur le port SMTP
     */
    boolean isSslEnable() {
        return sslEnable;
    }

    /**
     * @return identifiant SMTP (souvent l'adresse email)
     */
    String getUser() {
        return user;
    }

    /**
     * @return mot de passe SMTP
     */
    String getPassword() {
        return password;
    }

    /**
     * @return adresse expéditeur ({@code From})
     */
    String getFrom() {
        return from;
    }

    /**
     * @return nom affiché de l'expéditeur, ou chaîne vide
     */
    String getFromName() {
        return fromName;
    }

    /**
     * Indique si les paramètres minimaux pour un envoi réel sont présents.
     *
     * @return {@code true} si hôte, port, utilisateur, mot de passe et expéditeur sont renseignés
     */
    boolean isSmtpReady() {
        return isNotBlank(host) && port > 0 && isNotBlank(user) && isNotBlank(password) && isNotBlank(from);
    }

    /**
     * Charge la configuration depuis les fichiers classpath et l'environnement.
     *
     * @return instance configurée ; désactive l'envoi si l'hôte est vide alors que {@code enabled=true}
     */
    private static EmailConfig load() {
        Properties props = new Properties();
        loadResource(props, "email.properties");
        loadResource(props, "email.local.properties");

        boolean enabled = parseBoolean(envOrProp(props, "EMAIL_ENABLED", "email.enabled"), false);
        boolean logOtpOnFailure = parseBoolean(envOrProp(props, "EMAIL_LOG_OTP_ON_FAILURE", "email.log.otp.on.failure"), true);

        String host = envOrProp(props, "EMAIL_SMTP_HOST", "mail.smtp.host", "email.host");
        int port = parseInt(envOrProp(props, "EMAIL_SMTP_PORT", "mail.smtp.port", "email.port"), 587);
        boolean auth = parseBoolean(envOrProp(props, "EMAIL_SMTP_AUTH", "mail.smtp.auth"), true);
        boolean startTls = parseBoolean(envOrProp(props, "EMAIL_SMTP_STARTTLS", "mail.smtp.starttls.enable"), true);
        boolean sslEnable = parseBoolean(envOrProp(props, "EMAIL_SMTP_SSL_ENABLE", "mail.smtp.ssl.enable"), false);

        String user = envOrProp(props, "EMAIL_SMTP_USER", "mail.smtp.user");
        String password = envOrProp(props, "EMAIL_SMTP_PASSWORD", "mail.smtp.password");
        String from = envOrProp(props, "EMAIL_FROM", "mail.from", "email.from");
        if (!isNotBlank(from)) {
            from = user;
        }
        String fromName = envOrProp(props, "EMAIL_FROM_NAME", "mail.from.name");

        if (enabled && !isNotBlank(host)) {
            LOG.warning("email.enabled=true mais mail.smtp.host est vide — envoi désactivé.");
            enabled = false;
        }

        EmailConfig cfg = new EmailConfig(enabled, logOtpOnFailure, host, port, auth, startTls, sslEnable,
                user, password, from, fromName);
        logStartupSummary(cfg);
        return cfg;
    }

    /**
     * Journalise un résumé de la configuration au démarrage (identifiants masqués).
     *
     * @param cfg configuration chargée
     */
    private static void logStartupSummary(EmailConfig cfg) {
        LOG.info(() -> String.format(
                "Configuration email: enabled=%s, smtpReady=%s, host=%s, port=%d, auth=%s, startTls=%s, user=%s, from=%s",
                cfg.enabled,
                cfg.isSmtpReady(),
                blankToDash(cfg.host),
                cfg.port,
                cfg.auth,
                cfg.startTls,
                maskUser(cfg.user),
                blankToDash(cfg.from)));
        if (cfg.enabled && !cfg.isSmtpReady()) {
            LOG.warning("email.enabled=true mais identifiants SMTP incomplets — créez email.local.properties "
                    + "(mail.smtp.user, mail.smtp.password, mail.from) ou variables EMAIL_SMTP_*.");
        }
        if (!cfg.enabled) {
            LOG.warning("email.enabled=false — aucun email ne sera envoyé (OTP journalisé si email.log.otp.on.failure=true). "
                    + "Pour la production, copiez email.properties.example vers email.local.properties et activez SMTP.");
        }
    }

    /**
     * @param s chaîne à afficher
     * @return la chaîne ou {@code "-"} si vide
     */
    private static String blankToDash(String s) {
        return isNotBlank(s) ? s : "-";
    }

    /**
     * Masque partiellement l'identifiant SMTP pour les logs.
     *
     * @param user adresse ou identifiant SMTP
     * @return forme masquée (ex. {@code a***@domaine.com})
     */
    private static String maskUser(String user) {
        if (!isNotBlank(user)) {
            return "-";
        }
        int at = user.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return user.charAt(0) + "***" + user.substring(at);
    }

    /**
     * Fusionne un fichier properties du classpath dans la cible.
     *
     * @param target propriétés accumulées
     * @param name nom de la ressource (ex. {@code email.local.properties})
     */
    private static void loadResource(Properties target, String name) {
        try (InputStream in = EmailConfig.class.getClassLoader().getResourceAsStream(name)) {
            if (in != null) {
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    target.load(reader);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Impossible de charger " + name, e);
        }
    }

    /**
     * Lit une valeur : variable d'environnement prioritaire, puis clés properties.
     *
     * @param props propriétés chargées depuis les fichiers
     * @param envKey nom de la variable d'environnement
     * @param propKeys clés successives dans le fichier properties
     * @return valeur trouvée, ou chaîne vide
     */
    private static String envOrProp(Properties props, String envKey, String... propKeys) {
        String env = System.getenv(envKey);
        if (isNotBlank(env)) {
            return env.trim();
        }
        for (String propKey : propKeys) {
            String value = props.getProperty(propKey, "").trim();
            if (isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    /**
     * @param raw chaîne brute
     * @param defaultValue valeur par défaut si absente ou non reconnue
     * @return booléen interprété ({@code true}, {@code 1}, {@code yes})
     */
    private static boolean parseBoolean(String raw, boolean defaultValue) {
        if (!isNotBlank(raw)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw);
    }

    /**
     * @param raw chaîne entière
     * @param defaultValue valeur par défaut si parse impossible
     * @return entier parsé ou défaut
     */
    private static int parseInt(String raw, int defaultValue) {
        if (!isNotBlank(raw)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * @param s chaîne testée
     * @return {@code true} si non nulle et non vide après trim implicite ({@link String#isBlank()})
     */
    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}

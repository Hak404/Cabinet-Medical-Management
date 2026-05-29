package com.cabinet.util;

/**
 * Résultat typé d'une tentative d'envoi d'email SMTP.
 *
 * <p><b>Rôle :</b> encapsuler succès, échec ou envoi ignoré (SMTP désactivé) sans lever
 * d'exception vers les servlets.</p>
 * <p><b>Objectif :</b> permettre au contrôleur d'inscription de décider si le flux utilisateur
 * peut se poursuivre (OTP en session) même en mode développement.</p>
 * <p><b>Place MVC :</b> valeur de retour de {@link EmailUtil}, consommée par les servlets
 * après appel depuis la couche contrôleur.</p>
 *
 * @see EmailUtil
 * @since 1.0
 */
public final class EmailSendResult {

    private final boolean success;
    private final boolean skipped;
    private final String message;

    /**
     * Constructeur interne.
     *
     * @param success {@code true} si l'opération est considérée comme réussie côté applicatif
     * @param skipped {@code true} si l'envoi SMTP a été volontairement ignoré
     * @param message code ou libellé descriptif du résultat
     */
    private EmailSendResult(boolean success, boolean skipped, String message) {
        this.success = success;
        this.skipped = skipped;
        this.message = message;
    }

    /**
     * Crée un résultat de succès (email envoyé via SMTP).
     *
     * @param message code de succès (ex. {@code envoye})
     * @return instance avec {@link #isSuccess()} et {@link #wasDelivered()} à {@code true}
     */
    public static EmailSendResult ok(String message) {
        return new EmailSendResult(true, false, message);
    }

    /**
     * Crée un résultat « ignoré » (SMTP désactivé, flux autorisé à continuer).
     *
     * @param message code d'ignorance (ex. {@code smtp_desactive})
     * @return instance avec {@link #isSkipped()} à {@code true}
     */
    public static EmailSendResult skipped(String message) {
        return new EmailSendResult(true, true, message);
    }

    /**
     * Crée un résultat d'échec.
     *
     * @param message code d'erreur (ex. {@code echec_smtp})
     * @return instance avec {@link #isSuccess()} à {@code false}
     */
    public static EmailSendResult failure(String message) {
        return new EmailSendResult(false, false, message);
    }

    /**
     * Indique si l'opération est réussie du point de vue applicatif.
     *
     * @return {@code true} pour succès ou envoi ignoré
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Indique si l'envoi SMTP a été ignoré (configuration désactivée).
     *
     * @return {@code true} si aucun message n'a été transmis au serveur mail
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * Retourne le code ou message associé au résultat.
     *
     * @return libellé technique (souvent une clé i18n ou un identifiant interne)
     */
    public String getMessage() {
        return message;
    }

    /**
     * Indique si l'appelant peut poursuivre le flux métier (ex. stocker l'OTP en session).
     *
     * @return {@code true} si succès ou envoi ignoré — équivalent à {@link #isSuccess()}
     */
    public boolean mayContinueFlow() {
        return success;
    }

    /**
     * Indique si le message a réellement été délivré via SMTP.
     *
     * @return {@code true} uniquement en cas de succès sans mode ignoré
     */
    public boolean wasDelivered() {
        return success && !skipped;
    }
}

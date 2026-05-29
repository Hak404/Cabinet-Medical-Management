package com.cabinet.util;

import com.cabinet.model.RendezVous;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Politique d'annulation des rendez-vous par le patient (délai minimum 48 h).
 *
 * <p><b>Rôle :</b> centraliser la règle métier d'annulation avant mise à jour en base.</p>
 * <p><b>Objectif :</b> refuser l'annulation si le rendez-vous est trop proche, déjà passé,
 * annulé ou dans un statut terminal.</p>
 * <p><b>Place MVC :</b> règle pure invoquée par le servlet/service de rendez-vous ;
 * les comparaisons utilisent le fuseau JVM ({@link ZoneId#systemDefault()}),
 * cohérent avec {@link java.time.LocalDate#now()} et l'affichage des créneaux.</p>
 *
 * @see com.cabinet.model.RendezVous
 * @since 1.0
 */
public final class RendezVousCancellationPolicy {

    /** Délai minimum entre l'annulation et le début du rendez-vous. */
    public static final Duration MIN_NOTICE_BEFORE_APPOINTMENT = Duration.ofHours(48);

    /** Message d'erreur affiché lorsque le délai de 48 h n'est pas respecté. */
    public static final String ERROR_LESS_THAN_48H =
            "Impossible d'annuler un RDV moins de 48h avant la consultation.";

    private static final ZoneId ZONE = ZoneId.systemDefault();

    /** Constructeur privé — classe de politique statique. */
    private RendezVousCancellationPolicy() {
    }

    /**
     * Reconstruit la date-heure de début du rendez-vous.
     *
     * @param rdv rendez-vous source
     * @return {@link LocalDateTime} de début, ou {@code null} si date/heure manquantes
     */
    public static LocalDateTime appointmentDateTime(RendezVous rdv) {
        if (rdv == null || rdv.getDateRendezVous() == null || rdv.getStartTime() == null) {
            return null;
        }
        return LocalDateTime.of(rdv.getDateRendezVous(), rdv.getStartTime());
    }

    /**
     * Indique si le patient peut annuler ce rendez-vous (règle 48 h et statut).
     *
     * @param rdv rendez-vous à évaluer
     * @return {@code true} si l'annulation est autorisée
     */
    public static boolean isPatientCancellationAllowed(RendezVous rdv) {
        return evaluate(rdv).isAllowed();
    }

    /**
     * Évalue la politique d'annulation à l'instant courant (fuseau système).
     *
     * @param rdv rendez-vous à évaluer
     * @return résultat détaillé avec message d'erreur éventuel
     */
    public static Evaluation evaluate(RendezVous rdv) {
        return evaluate(rdv, ZonedDateTime.now(ZONE));
    }

    /**
     * Évalue la politique d'annulation à un instant de référence (tests ou recalcul).
     *
     * @param rdv rendez-vous à évaluer
     * @param now instant « maintenant » pour la comparaison
     * @return {@link Evaluation#allowed()} ou {@link Evaluation#denied(String)}
     */
    public static Evaluation evaluate(RendezVous rdv, ZonedDateTime now) {
        if (rdv == null) {
            return Evaluation.denied("Rendez-vous introuvable.");
        }

        RendezVous.Statut statut = rdv.getStatut();
        if (statut == RendezVous.Statut.ANNULE) {
            return Evaluation.denied("Ce rendez-vous est déjà annulé.");
        }
        if (statut == RendezVous.Statut.TERMINE || statut == RendezVous.Statut.EN_COURS) {
            return Evaluation.denied("Ce rendez-vous ne peut plus être annulé.");
        }

        LocalDateTime appointment = appointmentDateTime(rdv);
        if (appointment == null) {
            return Evaluation.denied("Date ou heure du rendez-vous invalide.");
        }

        ZonedDateTime appointmentZoned = appointment.atZone(ZONE);
        if (!appointmentZoned.isAfter(now)) {
            return Evaluation.denied("Ce rendez-vous est déjà passé.");
        }

        Duration remaining = Duration.between(now, appointmentZoned);
        if (remaining.compareTo(MIN_NOTICE_BEFORE_APPOINTMENT) < 0) {
            return Evaluation.denied(ERROR_LESS_THAN_48H);
        }

        return Evaluation.allowed();
    }

    /**
     * Résultat structuré d'une évaluation de politique d'annulation.
     */
    public static final class Evaluation {
        private final boolean allowed;
        private final String message;

        private Evaluation(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }

        /**
         * Crée une évaluation positive (annulation permise).
         *
         * @return instance autorisée sans message d'erreur
         */
        public static Evaluation allowed() {
            return new Evaluation(true, null);
        }

        /**
         * Crée une évaluation négative avec motif.
         *
         * @param message message à afficher à l'utilisateur
         * @return instance refusée
         */
        public static Evaluation denied(String message) {
            return new Evaluation(false, message);
        }

        /**
         * Indique si l'annulation est autorisée.
         *
         * @return {@code true} si le patient peut annuler
         */
        public boolean isAllowed() {
            return allowed;
        }

        /**
         * Retourne le motif de refus, le cas échéant.
         *
         * @return message d'erreur, ou {@code null} si autorisé
         */
        public String getMessage() {
            return message;
        }
    }
}

package com.cabinet.model;

import com.cabinet.util.RendezVousCancellationPolicy;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Rendez-vous médical, mappé sur la table {@code rendez_vous}.
 * Lie un patient, un médecin et un cabinet sur un créneau horaire, avec un statut de suivi.
 * Fournit des méthodes d'aide à l'affichage des actions d'annulation côté patient.
 */
public class RendezVous {

    /**
     * États possibles d'un rendez-vous dans le parcours de soins.
     */
    public enum Statut {
        /** Demande en attente de confirmation. */
        EN_ATTENTE,
        /** Rendez-vous confirmé par le cabinet. */
        CONFIRME,
        /** Consultation en cours. */
        EN_COURS,
        /** Consultation terminée. */
        TERMINE,
        /** Rendez-vous annulé. */
        ANNULE
    }

    private Long id;
    private Long cabinetId;
    private String cabinetNom;
    private Long medecinId;
    private String medecinNomComplet;
    private Long patientId;
    private String patientNomComplet;
    private LocalDate dateRendezVous;
    private LocalTime startTime;
    private LocalTime endTime;
    private Statut statut = Statut.EN_ATTENTE;

    /**
     * @return identifiant technique en base
     */
    public Long getId() { return id; }

    /**
     * @param id identifiant technique en base
     */
    public void setId(Long id) { this.id = id; }

    /**
     * @return identifiant du cabinet concerné
     */
    public Long getCabinetId() { return cabinetId; }

    /**
     * @param cabinetId identifiant du cabinet
     */
    public void setCabinetId(Long cabinetId) { this.cabinetId = cabinetId; }

    /**
     * @return nom du cabinet (rempli via JOIN, non persisté sur {@code rendez_vous})
     */
    public String getCabinetNom() { return cabinetNom; }

    /**
     * @param cabinetNom nom du cabinet pour affichage
     */
    public void setCabinetNom(String cabinetNom) { this.cabinetNom = cabinetNom; }

    /**
     * @return identifiant du médecin assigné
     */
    public Long getMedecinId() { return medecinId; }

    /**
     * @param medecinId identifiant du médecin
     */
    public void setMedecinId(Long medecinId) { this.medecinId = medecinId; }

    /**
     * @return nom complet du médecin (rempli via JOIN, non persisté)
     */
    public String getMedecinNomComplet() { return medecinNomComplet; }

    /**
     * @param medecinNomComplet nom complet du médecin pour affichage
     */
    public void setMedecinNomComplet(String medecinNomComplet) { this.medecinNomComplet = medecinNomComplet; }

    /**
     * @return identifiant du patient
     */
    public Long getPatientId() { return patientId; }

    /**
     * @param patientId identifiant du patient
     */
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    /**
     * @return nom complet du patient (rempli via JOIN, non persisté)
     */
    public String getPatientNomComplet() { return patientNomComplet; }

    /**
     * @param patientNomComplet nom complet du patient pour affichage
     */
    public void setPatientNomComplet(String patientNomComplet) { this.patientNomComplet = patientNomComplet; }

    /**
     * @return date du rendez-vous
     */
    public LocalDate getDateRendezVous() { return dateRendezVous; }

    /**
     * @param dateRendezVous date du rendez-vous
     */
    public void setDateRendezVous(LocalDate dateRendezVous) { this.dateRendezVous = dateRendezVous; }

    /**
     * @return heure de début du créneau
     */
    public LocalTime getStartTime() { return startTime; }

    /**
     * @param startTime heure de début du créneau
     */
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    /**
     * @return heure de fin du créneau
     */
    public LocalTime getEndTime() { return endTime; }

    /**
     * @param endTime heure de fin du créneau
     */
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    /**
     * @return statut courant du rendez-vous
     */
    public Statut getStatut() { return statut; }

    /**
     * @param statut statut courant
     */
    public void setStatut(Statut statut) { this.statut = statut; }

    /**
     * Indique si le patient peut annuler ce rendez-vous (délai ≥ 48 h, statut éligible).
     *
     * @return {@code true} si l'annulation patient est autorisée par la politique métier
     */
    public boolean isPatientCancellationAllowed() {
        return RendezVousCancellationPolicy.isPatientCancellationAllowed(this);
    }

    /**
     * Message affiché lorsque l'annulation patient est refusée (JSP, info-bulle).
     *
     * @return motif du blocage, ou chaîne vide si l'annulation est possible
     */
    public String getPatientCancellationBlockReason() {
        return RendezVousCancellationPolicy.evaluate(this).getMessage();
    }

    /**
     * Indique si le bouton « Annuler » doit être affiché (actif ou désactivé)
     * pour les rendez-vous futurs en statut {@link Statut#EN_ATTENTE} ou {@link Statut#CONFIRME}.
     *
     * @return {@code true} si l'action d'annulation doit apparaître dans l'interface patient
     */
    public boolean isPatientCancelActionVisible() {
        if (statut == Statut.ANNULE || statut == Statut.TERMINE || statut == Statut.EN_COURS) {
            return false;
        }
        return RendezVousCancellationPolicy.appointmentDateTime(this) != null;
    }
}

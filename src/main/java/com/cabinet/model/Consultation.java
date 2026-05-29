package com.cabinet.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Compte-rendu de consultation médicale, mappé sur la table {@code consultation}.
 * Certains champs sont enrichis par JOIN (rendez-vous, patient) et ne sont pas persistés
 * directement sur cette entité.
 */
public class Consultation {
    private Long id;
    private Long rendezVousId;
    private String diagnostic;
    private String remarque;
    private Instant createdAt;
    /** Rempli via JOIN rendez_vous (non persisté). */
    private LocalDate dateConsultation;
    /** Rempli via JOIN rendez_vous (non persisté). */
    private LocalTime heureConsultation;
    /** Rempli via JOIN user patient (non persisté). */
    private String patientNomComplet;
    /** Dérivé diagnostic/remarque pour affichage (non persisté). */
    private String motif;
    /** Statut du rendez-vous lié (non persisté). */
    private String statut;

    /**
     * @return identifiant technique en base
     */
    public Long getId() { return id; }

    /**
     * @param id identifiant technique en base
     */
    public void setId(Long id) { this.id = id; }

    /**
     * @return identifiant du rendez-vous associé
     */
    public Long getRendezVousId() { return rendezVousId; }

    /**
     * @param rendezVousId identifiant du rendez-vous associé
     */
    public void setRendezVousId(Long rendezVousId) { this.rendezVousId = rendezVousId; }

    /**
     * @return diagnostic médical établi lors de la consultation
     */
    public String getDiagnostic() { return diagnostic; }

    /**
     * @param diagnostic diagnostic médical
     */
    public void setDiagnostic(String diagnostic) { this.diagnostic = diagnostic; }

    /**
     * @return remarques complémentaires du médecin
     */
    public String getRemarque() { return remarque; }

    /**
     * @param remarque remarques complémentaires
     */
    public void setRemarque(String remarque) { this.remarque = remarque; }

    /**
     * @return horodatage de création de la fiche consultation
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * @param createdAt horodatage de création
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * @return date du rendez-vous lié (champ dérivé, non persisté sur {@code consultation})
     */
    public LocalDate getDateConsultation() { return dateConsultation; }

    /**
     * @param dateConsultation date du rendez-vous pour affichage
     */
    public void setDateConsultation(LocalDate dateConsultation) { this.dateConsultation = dateConsultation; }

    /**
     * @return heure de début du rendez-vous lié (champ dérivé, non persisté)
     */
    public LocalTime getHeureConsultation() { return heureConsultation; }

    /**
     * @param heureConsultation heure du rendez-vous pour affichage
     */
    public void setHeureConsultation(LocalTime heureConsultation) { this.heureConsultation = heureConsultation; }

    /**
     * @return nom complet du patient (champ dérivé via JOIN, non persisté)
     */
    public String getPatientNomComplet() { return patientNomComplet; }

    /**
     * @param patientNomComplet nom complet du patient pour affichage
     */
    public void setPatientNomComplet(String patientNomComplet) { this.patientNomComplet = patientNomComplet; }

    /**
     * @return motif ou résumé dérivé pour l'affichage (non persisté)
     */
    public String getMotif() { return motif; }

    /**
     * @param motif motif ou résumé pour affichage
     */
    public void setMotif(String motif) { this.motif = motif; }

    /**
     * @return libellé du statut du rendez-vous lié (non persisté)
     */
    public String getStatut() { return statut; }

    /**
     * @param statut statut du rendez-vous pour affichage
     */
    public void setStatut(String statut) { this.statut = statut; }
}

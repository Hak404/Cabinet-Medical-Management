package com.cabinet.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Ordonnance médicale issue d'une consultation, mappée sur la table {@code ordonnance}.
 * Contient le statut de préparation en pharmacie et la liste des lignes de médicaments.
 */
public class Ordonnance {

    /**
     * Cycle de vie d'une ordonnance du point de vue de la pharmacie.
     */
    public enum Statut {
        /** Prescrite par le médecin, en attente de préparation. */
        PRESCRITE,
        /** Préparée par la pharmacie, en attente de confirmation patient. */
        PREPAREE,
        /** Confirmée par le patient (retrait ou validation). */
        CONFIRMEE
    }

    private Long id;
    private Long consultationId;
    private Statut statut = Statut.PRESCRITE;
    private Long pharmacieId;
    private Instant createdAt;
    private List<MedicamentOrdonnance> lignes = new ArrayList<>();

    /**
     * @return identifiant technique en base
     */
    public Long getId() { return id; }

    /**
     * @param id identifiant technique en base
     */
    public void setId(Long id) { this.id = id; }

    /**
     * @return identifiant de la consultation à l'origine de l'ordonnance
     */
    public Long getConsultationId() { return consultationId; }

    /**
     * @param consultationId identifiant de la consultation liée
     */
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    /**
     * @return statut courant dans le circuit pharmacie
     */
    public Statut getStatut() { return statut; }

    /**
     * @param statut statut courant
     */
    public void setStatut(Statut statut) { this.statut = statut; }

    /**
     * @return identifiant de la pharmacie chargée de la préparation
     */
    public Long getPharmacieId() { return pharmacieId; }

    /**
     * @param pharmacieId identifiant de la pharmacie
     */
    public void setPharmacieId(Long pharmacieId) { this.pharmacieId = pharmacieId; }

    /**
     * @return date et heure de création de l'ordonnance
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * @param createdAt horodatage de création
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * @return lignes de médicaments prescrits (peut être vide, jamais {@code null} après init)
     */
    public List<MedicamentOrdonnance> getLignes() { return lignes; }

    /**
     * @param lignes liste des lignes de médicaments
     */
    public void setLignes(List<MedicamentOrdonnance> lignes) { this.lignes = lignes; }
}

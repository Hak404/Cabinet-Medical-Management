package com.cabinet.model;

/**
 * Ligne de médicament sur une ordonnance, mappée sur la table {@code medicament_ordonnance}.
 * Décrit le nom du produit, la posologie, la durée du traitement et l'ordre d'affichage.
 */
public class MedicamentOrdonnance {
    private Long id;
    private Long ordonnanceId;
    private String nom;
    private String posologie;
    private String duree;
    private int ligneOrdre;

    /**
     * @return identifiant technique de la ligne en base
     */
    public Long getId() { return id; }

    /**
     * @param id identifiant technique de la ligne
     */
    public void setId(Long id) { this.id = id; }

    /**
     * @return identifiant de l'ordonnance parente
     */
    public Long getOrdonnanceId() { return ordonnanceId; }

    /**
     * @param ordonnanceId identifiant de l'ordonnance parente
     */
    public void setOrdonnanceId(Long ordonnanceId) { this.ordonnanceId = ordonnanceId; }

    /**
     * @return nom du médicament prescrit
     */
    public String getNom() { return nom; }

    /**
     * @param nom nom du médicament
     */
    public void setNom(String nom) { this.nom = nom; }

    /**
     * @return posologie (ex. « 1 comprimé matin et soir »)
     */
    public String getPosologie() { return posologie; }

    /**
     * @param posologie posologie du traitement
     */
    public void setPosologie(String posologie) { this.posologie = posologie; }

    /**
     * @return durée du traitement (ex. « 7 jours »)
     */
    public String getDuree() { return duree; }

    /**
     * @param duree durée du traitement
     */
    public void setDuree(String duree) { this.duree = duree; }

    /**
     * @return numéro d'ordre de la ligne sur l'ordonnance (affichage)
     */
    public int getLigneOrdre() { return ligneOrdre; }

    /**
     * @param ligneOrdre numéro d'ordre de la ligne
     */
    public void setLigneOrdre(int ligneOrdre) { this.ligneOrdre = ligneOrdre; }
}

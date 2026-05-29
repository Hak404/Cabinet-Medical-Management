package com.cabinet.model;

/**
 * Profil pharmacie partenaire, extension de {@link User} mappée sur les tables
 * {@code user} et {@code pharmacie}.
 */
public class Pharmacie extends User {
    private Long cabinetId;
    private String adresse;

    /** Construit une pharmacie avec le rôle {@link Role#PHARMACIE} par défaut. */
    public Pharmacie() {
        setRole(Role.PHARMACIE);
    }

    /**
     * @return identifiant du cabinet médical associé
     */
    public Long getCabinetId() { return cabinetId; }

    /**
     * @param cabinetId identifiant du cabinet médical associé
     */
    public void setCabinetId(Long cabinetId) { this.cabinetId = cabinetId; }

    /**
     * @return adresse physique de la pharmacie
     */
    public String getAdresse() { return adresse; }

    /**
     * @param adresse adresse physique de la pharmacie
     */
    public void setAdresse(String adresse) { this.adresse = adresse; }
}

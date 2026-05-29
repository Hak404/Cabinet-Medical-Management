package com.cabinet.model;

/**
 * Entité cabinet médical, mappée sur la table {@code cabinet}.
 * Regroupe les informations du lieu d'exercice et la durée standard d'une consultation.
 */
public class Cabinet {
    private Long id;
    private String nom;
    private String adresse;
    private int dureeConsultationMinutes = 30;
    private Long medecinId;

    /** Construit un cabinet vide (instanciation par le DAO). */
    public Cabinet() {}

    /**
     * Construit un cabinet avec tous les champs persistés.
     *
     * @param id                        identifiant technique
     * @param nom                       nom du cabinet
     * @param adresse                   adresse postale
     * @param dureeConsultationMinutes  durée par défaut d'un créneau, en minutes
     * @param medecinId                 identifiant du médecin titulaire ou référent
     */
    public Cabinet(Long id, String nom, String adresse,
                   int dureeConsultationMinutes, Long medecinId) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.dureeConsultationMinutes = dureeConsultationMinutes;
        this.medecinId = medecinId;
    }

    /**
     * @return identifiant technique en base
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id identifiant technique en base
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return nom du cabinet
     */
    public String getNom() {
        return nom;
    }

    /**
     * @param nom nom du cabinet
     */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /**
     * @return adresse postale du cabinet
     */
    public String getAdresse() {
        return adresse;
    }

    /**
     * @param adresse adresse postale du cabinet
     */
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    /**
     * @return durée standard d'une consultation, en minutes
     */
    public int getDureeConsultationMinutes() {
        return dureeConsultationMinutes;
    }

    /**
     * @param dureeConsultationMinutes durée standard d'une consultation, en minutes
     */
    public void setDureeConsultationMinutes(int dureeConsultationMinutes) {
        this.dureeConsultationMinutes = dureeConsultationMinutes;
    }

    /**
     * @return identifiant du médecin associé au cabinet
     */
    public Long getMedecinId() {
        return medecinId;
    }

    /**
     * @param medecinId identifiant du médecin associé
     */
    public void setMedecinId(Long medecinId) {
        this.medecinId = medecinId;
    }
}

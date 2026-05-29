package com.cabinet.model;

import java.time.LocalDate;

/**
 * Profil patient, extension de {@link User} mappée sur les tables {@code user} et {@code patient}.
 */
public class Patient extends User {

    private String cin;
    private String adresse;
    private LocalDate dateNaissance;

    /** Construit un patient avec le rôle {@link Role#PATIENT} par défaut. */
    public Patient() {
        setRole(Role.PATIENT);
    }

    /**
     * Construit un patient avec identité, coordonnées et données médicales administratives.
     *
     * @param nom            nom de famille
     * @param prenom         prénom
     * @param email          adresse e-mail
     * @param password       mot de passe
     * @param telephone      numéro de téléphone
     * @param cin            numéro de carte d'identité nationale
     * @param adresse        adresse postale
     * @param dateNaissance  date de naissance
     */
    public Patient(String nom, String prenom, String email, String password, String telephone,
                   String cin, String adresse, LocalDate dateNaissance) {
        super(nom, prenom, email, password, telephone, Role.PATIENT);
        this.cin = cin;
        this.adresse = adresse;
        this.dateNaissance = dateNaissance;
    }

    /**
     * @return numéro de carte d'identité nationale (CIN)
     */
    public String getCin() { return cin; }

    /**
     * @param cin numéro de carte d'identité nationale (CIN)
     */
    public void setCin(String cin) { this.cin = cin; }

    /**
     * @return adresse postale du patient
     */
    public String getAdresse() { return adresse; }

    /**
     * @param adresse adresse postale du patient
     */
    public void setAdresse(String adresse) { this.adresse = adresse; }

    /**
     * @return date de naissance
     */
    public LocalDate getDateNaissance() { return dateNaissance; }

    /**
     * @param dateNaissance date de naissance
     */
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
}

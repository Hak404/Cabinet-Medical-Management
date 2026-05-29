package com.cabinet.model;

import java.time.LocalTime;
import java.util.List;

/**
 * Profil médecin, extension de {@link User} mappée sur les tables {@code user} et {@code medecin}.
 * Inclut le rattachement au cabinet, la spécialité, les horaires de consultation et les congés.
 */
public class Medecin extends User {
    private Long cabinetId;
    private String cabinetNom;
    private String specialite;
    private List<Conge> conges;
    private LocalTime heureDebut;
    private LocalTime heureFin;

    /** Construit un médecin avec le rôle {@link Role#MEDECIN} par défaut. */
    public Medecin() {
        setRole(Role.MEDECIN);
    }

    /**
     * @return identifiant du cabinet d'exercice
     */
    public Long getCabinetId() { return cabinetId; }

    /**
     * @param cabinetId identifiant du cabinet d'exercice
     */
    public void setCabinetId(Long cabinetId) { this.cabinetId = cabinetId; }

    /**
     * @return nom du cabinet (rempli via JOIN, non persisté sur {@code medecin})
     */
    public String getCabinetNom() { return cabinetNom; }

    /**
     * @param cabinetNom nom du cabinet pour affichage
     */
    public void setCabinetNom(String cabinetNom) { this.cabinetNom = cabinetNom; }

    /**
     * @return spécialité médicale du praticien
     */
    public String getSpecialite() { return specialite; }

    /**
     * @param specialite spécialité médicale
     */
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    /**
     * @return liste des jours de congé planifiés
     */
    public List<Conge> getConges() {return conges;}

    /**
     * @param conges liste des jours de congé planifiés
     */
	public void setConges(List<Conge> conges) {this.conges = conges;}

    /**
     * @return heure de début des consultations quotidiennes
     */
	public LocalTime getHeureDebut() { return heureDebut; }

    /**
     * @param heureDebut heure de début des consultations
     */
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    /**
     * @return heure de fin des consultations quotidiennes
     */
    public LocalTime getHeureFin() { return heureFin; }

    /**
     * @param heureFin heure de fin des consultations
     */
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }
}

package com.cabinet.model;

/**
 * Profil secrétaire médicale, extension de {@link User} mappée sur les tables
 * {@code user} et {@code secretaire}.
 */
public class Secretaire extends User {
    private Long medecinId;
    private Long cabinetId;
    private String bureau;

    /** Construit une secrétaire avec le rôle {@link Role#SECRETAIRE} par défaut. */
    public Secretaire() {
        setRole(Role.SECRETAIRE);
    }

    /**
     * @return identifiant du médecin auquel la secrétaire est rattachée
     */
    public Long getMedecinId() { return medecinId; }

    /**
     * @param medecinId identifiant du médecin rattaché
     */
    public void setMedecinId(Long medecinId) { this.medecinId = medecinId; }

    /**
     * @return identifiant du cabinet d'exercice
     */
    public Long getCabinetId() { return cabinetId; }

    /**
     * @param cabinetId identifiant du cabinet d'exercice
     */
    public void setCabinetId(Long cabinetId) { this.cabinetId = cabinetId; }

    /**
     * @return libellé ou numéro du bureau de la secrétaire
     */
    public String getBureau() { return bureau; }

    /**
     * @param bureau libellé ou numéro du bureau
     */
    public void setBureau(String bureau) { this.bureau = bureau; }
}

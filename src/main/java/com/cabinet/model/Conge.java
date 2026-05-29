package com.cabinet.model;

import java.time.LocalDate;

/**
 * Jour de congé d'un médecin, mappé sur la table {@code conge}.
 * Utilisé pour bloquer la prise de rendez-vous sur les dates concernées.
 */
public class Conge {

    private Long id;
    private LocalDate dateConge;
    private Long medecinId;

    /**
     * @return identifiant technique en base
     */
    public Long getId() { return id; }

    /**
     * @param id identifiant technique en base
     */
    public void setId(Long id) { this.id = id; }

    /**
     * @return date du congé (jour non travaillé)
     */
    public LocalDate getDateConge() { return dateConge; }

    /**
     * @param dateConge date du congé
     */
    public void setDateConge(LocalDate dateConge) { this.dateConge = dateConge; }

    /**
     * @return identifiant du médecin concerné
     */
    public Long getMedecinId() { return medecinId; }

    /**
     * @param medecinId identifiant du médecin concerné
     */
    public void setMedecinId(Long medecinId) { this.medecinId = medecinId; }
}

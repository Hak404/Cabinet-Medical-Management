package com.cabinet.model;

/**
 * Analyse médicale demandée lors d'une consultation, mappée sur la table
 * {@code analyse_demandee}.
 */
public class AnalyseDemandee {

    private Long id;
    private Long consultationId;
    private String codeAnalyse;

    /**
     * @return identifiant technique en base
     */
    public Long getId() { return id; }

    /**
     * @param id identifiant technique en base
     */
    public void setId(Long id) { this.id = id; }

    /**
     * @return identifiant de la consultation à l'origine de la demande
     */
    public Long getConsultationId() { return consultationId; }

    /**
     * @param consultationId identifiant de la consultation liée
     */
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    /**
     * @return code ou libellé court de l'analyse prescrite
     */
    public String getCodeAnalyse() { return codeAnalyse; }

    /**
     * @param codeAnalyse code ou libellé de l'analyse
     */
    public void setCodeAnalyse(String codeAnalyse) { this.codeAnalyse = codeAnalyse; }
}

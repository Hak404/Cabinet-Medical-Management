package com.cabinet.model;

import java.time.LocalDateTime;

/**
 * Métadonnées d'un document médical PDF (table {@code documents}).
 */
public class DocumentMedical {

    /**
     * Types de documents générés après consultation.
     */
    public enum TypeDocument {
        ORDONNANCE,
        ANALYSE,
        COMPTE_RENDU
    }

    private Long id;
    private Long patientId;
    private Long medecinId;
    private Long consultationId;
    private TypeDocument typeDocument;
    private String titre;
    private String fileName;
    private String filePath;
    private LocalDateTime dateCreation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getMedecinId() {
        return medecinId;
    }

    public void setMedecinId(Long medecinId) {
        this.medecinId = medecinId;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public TypeDocument getTypeDocument() {
        return typeDocument;
    }

    public void setTypeDocument(TypeDocument typeDocument) {
        this.typeDocument = typeDocument;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    /**
     * Libellé français pour l'affichage patient.
     */
    public String getTypeDocumentLabel() {
        if (typeDocument == null) {
            return "";
        }
        return switch (typeDocument) {
            case ORDONNANCE -> "Ordonnance";
            case ANALYSE -> "Analyses médicales";
            case COMPTE_RENDU -> "Compte rendu de consultation";
        };
    }
}

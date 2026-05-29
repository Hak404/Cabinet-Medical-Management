package com.cabinet.service;

import com.cabinet.controller.ConsultationServlet;
import com.cabinet.dao.AnalyseDemandeeDAO;
import com.cabinet.dao.ConsultationDAO;
import com.cabinet.dao.DocumentMedicalDAO;
import com.cabinet.dao.MedecinDAO;
import com.cabinet.dao.OrdonnanceDAO;
import com.cabinet.dao.PatientDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.Consultation;
import com.cabinet.model.DocumentMedical;
import com.cabinet.model.MedicamentOrdonnance;
import com.cabinet.model.Medecin;
import com.cabinet.model.Patient;
import com.cabinet.model.RendezVous;
import com.cabinet.util.DocumentStorageUtil;
import com.cabinet.util.EmailSendResult;
import com.cabinet.util.EmailUtil;
import com.cabinet.util.MedicalDocumentPdfGenerator;
import com.lowagie.text.DocumentException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Orchestration : génération PDF, enregistrement disque + base, envoi email patient.
 */
public class MedicalDocumentService {

    private static final Logger LOG = Logger.getLogger(MedicalDocumentService.class.getName());

    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final OrdonnanceDAO ordonnanceDAO = new OrdonnanceDAO();
    private final AnalyseDemandeeDAO analyseDemandeeDAO = new AnalyseDemandeeDAO();
    private final DocumentMedicalDAO documentMedicalDAO = new DocumentMedicalDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();

    /**
     * Résultat de la génération pour affichage médecin.
     */
    public static final class GenerationResult {
        private final List<DocumentMedical> documents;
        private final EmailSendResult emailResult;
        private final String errorMessage;

        public GenerationResult(List<DocumentMedical> documents, EmailSendResult emailResult, String errorMessage) {
            this.documents = documents != null ? documents : List.of();
            this.emailResult = emailResult;
            this.errorMessage = errorMessage;
        }

        public List<DocumentMedical> getDocuments() {
            return documents;
        }

        public EmailSendResult getEmailResult() {
            return emailResult;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return errorMessage == null && !documents.isEmpty();
        }
    }

    /**
     * Génère les PDF pour une consultation déjà enregistrée, persiste les métadonnées et envoie l'email.
     *
     * @param rdvId         identifiant du rendez-vous
     * @param medecinUserId identifiant du médecin connecté (contrôle d'accès)
     * @return résultat avec documents créés et statut email
     */
    public GenerationResult generateForConsultation(Long rdvId, Long medecinUserId) {
        RendezVous rdv = rendezVousDAO.findById(rdvId);
        if (rdv == null || !medecinUserId.equals(rdv.getMedecinId())) {
            return new GenerationResult(List.of(), null, "Accès refusé ou rendez-vous introuvable.");
        }

        Consultation consultation = consultationDAO.findByRendezVousId(rdvId);
        if (consultation == null || consultation.getId() == null) {
            return new GenerationResult(List.of(), null,
                    "Enregistrez d'abord la consultation avant de générer les documents.");
        }

        Patient patient = patientDAO.findById(rdv.getPatientId());
        Medecin medecin = medecinDAO.findById(rdv.getMedecinId());
        if (patient == null || medecin == null) {
            return new GenerationResult(List.of(), null, "Patient ou médecin introuvable.");
        }

        String patientNom = formatNom(patient.getPrenom(), patient.getNom());
        String medecinNom = formatNom(medecin.getPrenom(), medecin.getNom());
        LocalDate dateConsultation = rdv.getDateRendezVous() != null ? rdv.getDateRendezVous() : LocalDate.now();

        List<MedicamentOrdonnance> medicaments = List.of();
        Long ordonnanceId = ordonnanceDAO.findOrdonnanceIdByConsultationId(consultation.getId());
        if (ordonnanceId != null) {
            medicaments = ordonnanceDAO.findMedicamentsByOrdonnanceId(ordonnanceId);
        }

        List<String> codesAnalyse = new ArrayList<>(analyseDemandeeDAO.findCodesByConsultationId(consultation.getId()));
        Map<String, String> analyseLabels = ConsultationServlet.ANALYSES_DISPONIBLES;

        List<DocumentMedical> created = new ArrayList<>();
        List<Path> filesForEmail = new ArrayList<>();

        try {
            Path patientDir = DocumentStorageUtil.getPatientDirectory(rdv.getPatientId());

            if (hasOrdonnanceContent(medicaments)) {
                Path ordonnancePath = patientDir.resolve("ordonnance_" + consultation.getId() + ".pdf");
                try {
                    MedicalDocumentPdfGenerator.generateOrdonnance(
                            ordonnancePath, patientNom, medecinNom, dateConsultation, medicaments);
                } catch (Exception e) {
                    throw new RuntimeException("Erreur génération ordonnance", e);
                }
                DocumentMedical doc = buildAndSave(patientDir, rdv, consultation.getId(),
                        DocumentMedical.TypeDocument.ORDONNANCE, "ordonnance", dateConsultation,
                        path -> {});              
                created.add(doc);
                filesForEmail.add(DocumentStorageUtil.resolveStoredPath(doc.getFilePath()));
            }

            if (!codesAnalyse.isEmpty()) {
                DocumentMedical doc = buildAndSave(patientDir, rdv, consultation.getId(),
                        DocumentMedical.TypeDocument.ANALYSE, "analyses", dateConsultation,
                        path -> MedicalDocumentPdfGenerator.generateAnalyses(
                                path, patientNom, medecinNom, dateConsultation, codesAnalyse, analyseLabels));
                created.add(doc);
                filesForEmail.add(DocumentStorageUtil.resolveStoredPath(doc.getFilePath()));
            }

            if (hasCompteRenduContent(consultation)) {
                DocumentMedical doc = buildAndSave(patientDir, rdv, consultation.getId(),
                        DocumentMedical.TypeDocument.COMPTE_RENDU, "compte_rendu", dateConsultation,
                        path -> MedicalDocumentPdfGenerator.generateCompteRendu(
                                path, patientNom, medecinNom, dateConsultation,
                                consultation.getDiagnostic(), consultation.getRemarque()));
                created.add(doc);
                filesForEmail.add(DocumentStorageUtil.resolveStoredPath(doc.getFilePath()));
            }

            if (created.isEmpty()) {
                return new GenerationResult(List.of(), null,
                        "Aucun contenu à générer : saisissez diagnostic, ordonnance ou analyses.");
            }

            EmailSendResult emailResult = EmailUtil.sendMedicalDocuments(patient.getEmail(), filesForEmail);
            LOG.info(() -> "Documents générés pour consultation " + consultation.getId()
                    + " — count=" + created.size() + ", email=" + emailResult.getMessage());

            return new GenerationResult(created, emailResult, null);

        } catch (Exception e) {
            LOG.severe("Erreur génération documents : " + e.getMessage());
            return new GenerationResult(created, null, "Erreur lors de la génération : " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface PdfWriterAction {
        void write(Path path) throws IOException, DocumentException;
    }

    private DocumentMedical buildAndSave(Path patientDir,
                                         RendezVous rdv,
                                         Long consultationId,
                                         DocumentMedical.TypeDocument type,
                                         String filePrefix,
                                         LocalDate date,
                                         PdfWriterAction writer) throws IOException, DocumentException {
        String fileName = DocumentStorageUtil.buildFileName(filePrefix, date);
        Path absolutePath = patientDir.resolve(fileName);
        writer.write(absolutePath);

        DocumentMedical meta = new DocumentMedical();
        meta.setPatientId(rdv.getPatientId());
        meta.setMedecinId(rdv.getMedecinId());
        meta.setConsultationId(consultationId);
        meta.setTypeDocument(type);
        meta.setTitre(MedicalDocumentPdfGenerator.defaultTitre(type, date));
        meta.setFileName(fileName);
        meta.setFilePath(DocumentStorageUtil.toRelativePath(absolutePath));
        return documentMedicalDAO.save(meta);
    }

    private static boolean hasOrdonnanceContent(List<MedicamentOrdonnance> medicaments) {
        if (medicaments == null || medicaments.isEmpty()) {
            return false;
        }
        return medicaments.stream().anyMatch(m -> m.getNom() != null && !m.getNom().isBlank());
    }

    private static boolean hasCompteRenduContent(Consultation c) {
        return (c.getDiagnostic() != null && !c.getDiagnostic().isBlank())
                || (c.getRemarque() != null && !c.getRemarque().isBlank());
    }

    private static String formatNom(String prenom, String nom) {
        String p = prenom != null ? prenom.trim() : "";
        String n = nom != null ? nom.trim() : "";
        return (p + " " + n).trim();
    }
}

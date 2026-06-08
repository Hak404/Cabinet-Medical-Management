package com.cabinet.service;

import com.cabinet.dao.AnalyseDemandeeDAO;
import com.cabinet.dao.ConsultationDAO;
import com.cabinet.dao.OrdonnanceDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.Consultation;
import com.cabinet.model.MedicamentOrdonnance;
import com.cabinet.model.RendezVous;
import com.cabinet.util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service gérant la logique métier des consultations et les transactions associées.
 */
public class ConsultationService {

    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final OrdonnanceDAO ordonnanceDAO = new OrdonnanceDAO();
    private final AnalyseDemandeeDAO analyseDemandeeDAO = new AnalyseDemandeeDAO();
    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();

    /**
     * Enregistre le dossier complet d'une consultation (diagnostic, ordonnance, analyses)
     * dans une seule transaction atomique.
     */
    public void saveFullConsultation(Long rdvId, String diagnostic, String remarque, 
                                    List<MedicamentOrdonnance> medicaments, 
                                    List<String> codesAnalyse) {
        
        DBConnection.executeInTransaction(con -> {
            // 1. Consultation (diagnostic/remarque)
            Consultation consultation = consultationDAO.findByRendezVousId(rdvId)
                    .orElse(new Consultation());
            consultation.setRendezVousId(rdvId);
            consultation.setDiagnostic(diagnostic);
            consultation.setRemarque(remarque);
            
            Long consultationId = consultationDAO.save(con, consultation)
                    .orElseThrow(() -> new SQLException("Échec de sauvegarde de la consultation"));

            // 2. Ordonnance et médicaments
            Long ordonnanceId = ordonnanceDAO.ensureOrdonnance(con, consultationId)
                    .orElseThrow(() -> new SQLException("Échec de création de l'ordonnance"));
            
            ordonnanceDAO.deleteMedicamentsByOrdonnanceId(con, ordonnanceId);
            int ordre = 1;
            for (MedicamentOrdonnance m : medicaments) {
                ordonnanceDAO.insertMedicament(con, ordonnanceId, m.getNom(), m.getPosologie(), m.getDuree(), ordre++);
            }

            // 3. Analyses demandées
            analyseDemandeeDAO.deleteByConsultationId(con, consultationId);
            for (String code : codesAnalyse) {
                analyseDemandeeDAO.save(con, consultationId, code);
            }

            // 4. Mise à jour statut RDV
            rendezVousDAO.updateStatut(rdvId, RendezVous.Statut.TERMINE);

            return null;
        });
    }
}

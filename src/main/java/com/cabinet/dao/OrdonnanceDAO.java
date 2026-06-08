package com.cabinet.dao;

import com.cabinet.model.MedicamentOrdonnance;
import com.cabinet.model.Ordonnance;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation de OrdonnanceDAO utilisant BaseDAO.
 */
public class OrdonnanceDAO extends BaseDAO {

    /**
     * Projection pour l'affichage des ordonnances.
     */
    public static class OrdonnanceView {
        private Long id;
        private Long consultationId;
        private Long rendezVousId;
        private LocalDate dateRendezVous;
        private String patientNomComplet;
        private String statut;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getConsultationId() { return consultationId; }
        public void setConsultationId(Long cid) { this.consultationId = cid; }
        public Long getRendezVousId() { return rendezVousId; }
        public void setRendezVousId(Long rid) { this.rendezVousId = rid; }
        public LocalDate getDateRendezVous() { return dateRendezVous; }
        public void setDateRendezVous(LocalDate d) { this.dateRendezVous = d; }
        public String getPatientNomComplet() { return patientNomComplet; }
        public void setPatientNomComplet(String n) { this.patientNomComplet = n; }
        public String getStatut() { return statut; }
        public void setStatut(String s) { this.statut = s; }
    }

    public Optional<Long> findIdByConsultationId(Long consultationId) {
        String sql = "SELECT id FROM ordonnance WHERE consultation_id = ?";
        return queryOne(sql, rs -> rs.getLong("id"), consultationId);
    }

    public Optional<Long> ensureOrdonnance(Connection con, Long consultationId) throws SQLException {
        Optional<Long> existing = queryOne(con, "SELECT id FROM ordonnance WHERE consultation_id = ?", rs -> rs.getLong("id"), consultationId);
        if (existing.isPresent()) {
            return existing;
        }
        String sql = "INSERT INTO ordonnance (consultation_id, statut) VALUES (?, ?)";
        return insert(con, sql, consultationId, Ordonnance.Statut.PRESCRITE.name());
    }

    public void deleteMedicamentsByOrdonnanceId(Connection con, Long ordonnanceId) throws SQLException {
        String sql = "DELETE FROM medicament_ordonnance WHERE ordonnance_id = ?";
        update(con, sql, ordonnanceId);
    }

    public void insertMedicament(Connection con, Long ordonnanceId, String nom, String poso, String duree, int ordre) throws SQLException {
        String sql = "INSERT INTO medicament_ordonnance (ordonnance_id, nom, posologie, duree, ligne_ordre) VALUES (?, ?, ?, ?, ?)";
        insert(con, sql, ordonnanceId, nom, poso, duree, ordre);
    }

    public List<MedicamentOrdonnance> findMedicamentsByOrdonnanceId(Long ordonnanceId) {
        String sql = """
                SELECT id, ordonnance_id, nom, posologie, duree, ligne_ordre
                FROM medicament_ordonnance
                WHERE ordonnance_id = ?
                ORDER BY ligne_ordre, id
                """;
        return queryList(sql, rs -> {
            MedicamentOrdonnance m = new MedicamentOrdonnance();
            m.setId(rs.getLong("id"));
            m.setOrdonnanceId(rs.getLong("ordonnance_id"));
            m.setNom(rs.getString("nom"));
            m.setPosologie(rs.getString("posologie"));
            m.setDuree(rs.getString("duree"));
            m.setLigneOrdre(rs.getInt("ligne_ordre"));
            return m;
        }, ordonnanceId);
    }

    public List<OrdonnanceView> findByMedecinAndDate(Long medecinId, LocalDate date) {
        String sql = """
                SELECT o.id, o.consultation_id, o.statut,
                       rv.id AS rendez_vous_id, rv.date_rendez_vous,
                       CONCAT(pu.nom, ' ', pu.prenom) AS patient_nom
                FROM ordonnance o
                INNER JOIN consultation c ON c.id = o.consultation_id
                INNER JOIN rendez_vous rv ON rv.id = c.rendez_vous_id
                INNER JOIN user pu ON pu.id = rv.patient_id
                WHERE rv.medecin_id = ?
                  AND rv.date_rendez_vous = ?
                ORDER BY rv.start_time
                """;
        return queryList(sql, rs -> {
            OrdonnanceView o = new OrdonnanceView();
            o.setId(rs.getLong("id"));
            o.setConsultationId(rs.getLong("consultation_id"));
            o.setRendezVousId(rs.getLong("rendez_vous_id"));
            o.setDateRendezVous(rs.getDate("date_rendez_vous").toLocalDate());
            o.setPatientNomComplet(rs.getString("patient_nom"));
            o.setStatut(rs.getString("statut"));
            return o;
        }, medecinId, Date.valueOf(date));
    }

    public Long findOrdonnanceIdByConsultationId(Long consultationId) {
        return findIdByConsultationId(consultationId).orElse(null);
    }
}

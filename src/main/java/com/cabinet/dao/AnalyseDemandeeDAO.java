package com.cabinet.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Implémentation de AnalyseDemandeeDAO utilisant BaseDAO.
 */
public class AnalyseDemandeeDAO extends BaseDAO {

    /**
     * Projection pour l'affichage des analyses demandées.
     */
    public static class AnalyseView {
        private Long consultationId;
        private Long rendezVousId;
        private LocalDate dateRendezVous;
        private String patientNomComplet;
        private String codeAnalyse;

        public Long getConsultationId() { return consultationId; }
        public void setConsultationId(Long cid) { this.consultationId = cid; }
        public Long getRendezVousId() { return rendezVousId; }
        public void setRendezVousId(Long rid) { this.rendezVousId = rid; }
        public LocalDate getDateRendezVous() { return dateRendezVous; }
        public void setDateRendezVous(LocalDate d) { this.dateRendezVous = d; }
        public String getPatientNomComplet() { return patientNomComplet; }
        public void setPatientNomComplet(String n) { this.patientNomComplet = n; }
        public String getCodeAnalyse() { return codeAnalyse; }
        public void setCodeAnalyse(String c) { this.codeAnalyse = c; }
    }

    public Set<String> findCodesByConsultationId(Long consultationId) {
        String sql = "SELECT code_analyse FROM analyse_demandee WHERE consultation_id = ? ORDER BY code_analyse";
        return new LinkedHashSet<>(queryList(sql, rs -> rs.getString("code_analyse"), consultationId));
    }

    public void deleteByConsultationId(Connection con, Long consultationId) throws SQLException {
        String sql = "DELETE FROM analyse_demandee WHERE consultation_id = ?";
        update(con, sql, consultationId);
    }

    public void save(Connection con, Long consultationId, String codeAnalyse) throws SQLException {
        String sql = "INSERT INTO analyse_demandee (consultation_id, code_analyse) VALUES (?, ?)";
        insert(con, sql, consultationId, codeAnalyse);
    }

    public List<AnalyseView> findByMedecinAndDate(Long medecinId, LocalDate date) {
        String sql = """
                SELECT ad.consultation_id, ad.code_analyse,
                       rv.id AS rendez_vous_id, rv.date_rendez_vous,
                       CONCAT(pu.nom, ' ', pu.prenom) AS patient_nom
                FROM analyse_demandee ad
                INNER JOIN consultation c ON c.id = ad.consultation_id
                INNER JOIN rendez_vous rv ON rv.id = c.rendez_vous_id
                INNER JOIN user pu ON pu.id = rv.patient_id
                WHERE rv.medecin_id = ?
                  AND rv.date_rendez_vous = ?
                ORDER BY rv.start_time, ad.code_analyse
                """;
        return queryList(sql, rs -> {
            AnalyseView a = new AnalyseView();
            a.setConsultationId(rs.getLong("consultation_id"));
            a.setRendezVousId(rs.getLong("rendez_vous_id"));
            a.setDateRendezVous(rs.getDate("date_rendez_vous").toLocalDate());
            a.setPatientNomComplet(rs.getString("patient_nom"));
            a.setCodeAnalyse(rs.getString("code_analyse"));
            return a;
        }, medecinId, Date.valueOf(date));
    }
}

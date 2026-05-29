package com.cabinet.dao;

import com.cabinet.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Accès JDBC aux analyses médicales demandées lors des consultations.
 *
 * <p>Table principale : {@code analyse_demandee}, jointures {@code consultation},
 * {@code rendez_vous}, {@code user} pour les listes par médecin et date.</p>
 */
public class AnalyseDemandeeDAO {

    /**
     * Projection lecture pour l'affichage des analyses demandées (agenda médecin).
     */
    public static class AnalyseView {
        private Long consultationId;
        private Long rendezVousId;
        private LocalDate dateRendezVous;
        private String patientNomComplet;
        private String codeAnalyse;

        /** @return identifiant de la consultation */
        public Long getConsultationId() { return consultationId; }
        /** @return identifiant du rendez-vous lié */
        public Long getRendezVousId() { return rendezVousId; }
        /** @return date du rendez-vous */
        public LocalDate getDateRendezVous() { return dateRendezVous; }
        /** @return nom complet du patient */
        public String getPatientNomComplet() { return patientNomComplet; }
        /** @return code de l'analyse demandée */
        public String getCodeAnalyse() { return codeAnalyse; }
    }

    /**
     * Retourne l'ensemble des codes d'analyse demandés pour une consultation.
     *
     * <p><strong>SQL :</strong> {@code SELECT code_analyse FROM analyse_demandee}
     * avec {@code WHERE consultation_id = ?}, tri par code. Pas de transaction.</p>
     *
     * @param consultationId identifiant de la consultation
     * @return ensemble ordonné des codes (vide si aucun)
     */
    public Set<String> findCodesByConsultationId(Long consultationId) {
        Set<String> codes = new LinkedHashSet<>();
        String sql = "SELECT code_analyse FROM analyse_demandee WHERE consultation_id = ? ORDER BY code_analyse";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, consultationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    codes.add(rs.getString("code_analyse"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return codes;
    }

    /**
     * Remplace l'ensemble des analyses demandées pour une consultation.
     *
     * <p><strong>SQL (transaction) :</strong></p>
     * <ul>
     *   <li>{@code DELETE FROM analyse_demandee WHERE consultation_id = ?}</li>
     *   <li>{@code INSERT INTO analyse_demandee} (consultation_id, code_analyse) en batch,
     *       codes dédupliqués et trimés</li>
     * </ul>
     * <p>{@code setAutoCommit(false)} ; {@code commit} ou {@code rollback}.</p>
     *
     * @param consultationId identifiant de la consultation
     * @param codes          codes d'analyses (peut être {@code null} ou vide)
     * @throws RuntimeException si la transaction échoue
     */
    public void replaceDemandes(Long consultationId, Iterable<String> codes) {
        String deleteSql = "DELETE FROM analyse_demandee WHERE consultation_id = ?";
        String insertSql = "INSERT INTO analyse_demandee (consultation_id, code_analyse) VALUES (?, ?)";
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            try (PreparedStatement del = con.prepareStatement(deleteSql)) {
                del.setLong(1, consultationId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = con.prepareStatement(insertSql)) {
                if (codes != null) {
                    Set<String> seen = new LinkedHashSet<>();
                    for (String raw : codes) {
                        if (raw == null) continue;
                        String c = raw.trim();
                        if (c.isEmpty() || seen.contains(c)) continue;
                        seen.add(c);
                        ins.setLong(1, consultationId);
                        ins.setString(2, c);
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }
            con.commit();
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ignored) {
                    // noop
                }
            }
            throw new RuntimeException("Erreur sauvegarde analyses demandées", e);
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (Exception ignored) {
                    // noop
                }
            }
        }
    }

    /**
     * Liste les analyses demandées pour les consultations d'un médecin à une date donnée.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code analyse_demandee} ad avec jointures
     * {@code consultation}, {@code rendez_vous}, {@code user} (patient),
     * filtres médecin et date, tri par heure et code. Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param date      date des rendez-vous
     * @return liste des vues analyse pour l'agenda
     */
    public List<AnalyseView> findByMedecinAndDate(Long medecinId, LocalDate date) {
        List<AnalyseView> result = new ArrayList<>();
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
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AnalyseView a = new AnalyseView();
                    a.consultationId = rs.getLong("consultation_id");
                    a.rendezVousId = rs.getLong("rendez_vous_id");
                    a.dateRendezVous = rs.getDate("date_rendez_vous").toLocalDate();
                    a.patientNomComplet = rs.getString("patient_nom");
                    a.codeAnalyse = rs.getString("code_analyse");
                    result.add(a);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}

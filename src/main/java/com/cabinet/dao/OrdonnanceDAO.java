package com.cabinet.dao;

import com.cabinet.model.MedicamentOrdonnance;
import com.cabinet.model.Ordonnance;
import com.cabinet.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès JDBC aux ordonnances et lignes médicamenteuses.
 *
 * <p>Tables concernées : {@code ordonnance}, {@code medicament_ordonnance}, avec jointures
 * {@code consultation}, {@code rendez_vous}, {@code user} pour les vues agenda.</p>
 */
public class OrdonnanceDAO {

    /**
     * Vue lecture seule d'une ordonnance pour l'affichage (liste du jour, etc.).
     */
    public static class OrdonnanceView {
        private Long id;
        private Long consultationId;
        private Long rendezVousId;
        private LocalDate dateRendezVous;
        private String patientNomComplet;
        private String statut;

        /** @return identifiant de l'ordonnance */
        public Long getId() { return id; }
        /** @return identifiant de la consultation liée */
        public Long getConsultationId() { return consultationId; }
        /** @return identifiant du rendez-vous lié */
        public Long getRendezVousId() { return rendezVousId; }
        /** @return date du rendez-vous associé */
        public LocalDate getDateRendezVous() { return dateRendezVous; }
        /** @return nom complet du patient */
        public String getPatientNomComplet() { return patientNomComplet; }
        /** @return statut de l'ordonnance (ex. PRESCRITE) */
        public String getStatut() { return statut; }
    }

    /**
     * Récupère l'identifiant d'ordonnance lié à une consultation, s'il existe.
     *
     * <p><strong>SQL :</strong> {@code SELECT id FROM ordonnance WHERE consultation_id = ?}.
     * Pas de transaction.</p>
     *
     * @param consultationId identifiant de la consultation
     * @return id ordonnance ou {@code null} si aucune ou en cas d'erreur
     */
    public Long findOrdonnanceIdByConsultationId(Long consultationId) {
        String sql = "SELECT id FROM ordonnance WHERE consultation_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, consultationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Liste les médicaments d'une ordonnance, triés par ordre de ligne.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code medicament_ordonnance}
     * avec {@code WHERE ordonnance_id = ?}, {@code ORDER BY ligne_ordre, id}.
     * Pas de transaction.</p>
     *
     * @param ordonnanceId identifiant de l'ordonnance
     * @return liste des lignes médicament
     */
    public List<MedicamentOrdonnance> findMedicamentsByOrdonnanceId(Long ordonnanceId) {
        List<MedicamentOrdonnance> result = new ArrayList<>();
        String sql = """
                SELECT id, ordonnance_id, nom, posologie, duree, ligne_ordre
                FROM medicament_ordonnance
                WHERE ordonnance_id = ?
                ORDER BY ligne_ordre, id
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, ordonnanceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MedicamentOrdonnance m = new MedicamentOrdonnance();
                    m.setId(rs.getLong("id"));
                    m.setOrdonnanceId(rs.getLong("ordonnance_id"));
                    m.setNom(rs.getString("nom"));
                    m.setPosologie(rs.getString("posologie"));
                    m.setDuree(rs.getString("duree"));
                    m.setLigneOrdre(rs.getInt("ligne_ordre"));
                    result.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Retourne l'ordonnance existante ou en crée une nouvelle au statut PRESCRITE.
     *
     * <p><strong>SQL :</strong> {@code SELECT id FROM ordonnance} puis éventuellement
     * {@code INSERT INTO ordonnance} (consultation_id, statut, pharmacie_id NULL).
     * Pas de transaction explicite sur l'insertion seule.</p>
     *
     * @param consultationId identifiant de la consultation
     * @return identifiant de l'ordonnance
     * @throws RuntimeException si l'insertion échoue
     */
    public Long ensureOrdonnance(Long consultationId) {
        Long existing = findOrdonnanceIdByConsultationId(consultationId);
        if (existing != null) {
            return existing;
        }
        String sql = "INSERT INTO ordonnance (consultation_id, statut, pharmacie_id) VALUES (?, ?, NULL)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, consultationId);
            ps.setString(2, Ordonnance.Statut.PRESCRITE.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur création ordonnance", e);
        }
        return null;
    }

    /**
     * Supprime l'ordonnance associée à une consultation (cascade médicaments selon schéma).
     *
     * <p><strong>SQL :</strong> {@code DELETE FROM ordonnance WHERE consultation_id = ?}.
     * Pas de transaction.</p>
     *
     * @param consultationId identifiant de la consultation
     * @throws RuntimeException en cas d'erreur JDBC
     */
    public void deleteOrdonnanceByConsultationId(Long consultationId) {
        String sql = "DELETE FROM ordonnance WHERE consultation_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, consultationId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erreur suppression ordonnance", e);
        }
    }

    /**
     * Remplace toutes les lignes médicament d'une ordonnance (suppression puis insertion batch).
     *
     * <p><strong>SQL (transaction) :</strong></p>
     * <ul>
     *   <li>{@code DELETE FROM medicament_ordonnance WHERE ordonnance_id = ?}</li>
     *   <li>{@code INSERT INTO medicament_ordonnance} en {@code executeBatch}</li>
     * </ul>
     * <p>{@code setAutoCommit(false)} ; {@code commit} ou {@code rollback}.</p>
     *
     * @param ordonnanceId identifiant de l'ordonnance
     * @param lignes       nouvelles lignes à enregistrer
     * @throws RuntimeException si la transaction échoue
     */
    public void replaceMedicaments(Long ordonnanceId, List<MedicamentOrdonnance> lignes) {
        String deleteSql = "DELETE FROM medicament_ordonnance WHERE ordonnance_id = ?";
        String insertSql = """
                INSERT INTO medicament_ordonnance (ordonnance_id, nom, posologie, duree, ligne_ordre)
                VALUES (?, ?, ?, ?, ?)
                """;
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            try (PreparedStatement del = con.prepareStatement(deleteSql)) {
                del.setLong(1, ordonnanceId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = con.prepareStatement(insertSql)) {
                int ord = 1;
                for (MedicamentOrdonnance m : lignes) {
                    ins.setLong(1, ordonnanceId);
                    ins.setString(2, m.getNom());
                    ins.setString(3, m.getPosologie());
                    ins.setString(4, m.getDuree());
                    ins.setInt(5, ord++);
                    ins.addBatch();
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
            throw new RuntimeException("Erreur sauvegarde médicaments", e);
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
     * Liste les ordonnances d'un médecin pour une date (vue enrichie patient / rendez-vous).
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code ordonnance} o avec jointures
     * {@code consultation}, {@code rendez_vous}, {@code user} (patient),
     * filtres {@code medecin_id} et {@code date_rendez_vous}. Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param date      date des rendez-vous
     * @return liste des vues ordonnance pour l'agenda
     */
    public List<OrdonnanceView> findByMedecinAndDate(Long medecinId, LocalDate date) {
        List<OrdonnanceView> result = new ArrayList<>();
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
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrdonnanceView o = new OrdonnanceView();
                    o.id = rs.getLong("id");
                    o.consultationId = rs.getLong("consultation_id");
                    o.rendezVousId = rs.getLong("rendez_vous_id");
                    o.dateRendezVous = rs.getDate("date_rendez_vous").toLocalDate();
                    o.patientNomComplet = rs.getString("patient_nom");
                    o.statut = rs.getString("statut");
                    result.add(o);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}

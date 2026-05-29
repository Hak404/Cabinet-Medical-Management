package com.cabinet.dao;

import com.cabinet.model.Consultation;
import com.cabinet.model.MedicamentOrdonnance;
import com.cabinet.model.Ordonnance;
import com.cabinet.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Accès JDBC aux consultations et agrégats liés (ordonnance, médicaments, analyses, rendez-vous).
 *
 * <p>La méthode {@link #saveFullConsultation} orchestre une transaction multi-tables :
 * {@code consultation}, {@code ordonnance}, {@code medicament_ordonnance}, {@code analyse_demandee},
 * {@code rendez_vous}.</p>
 */
public class ConsultationDAO {

    /**
     * Recherche une consultation par identifiant de rendez-vous.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code consultation}
     * ({@code WHERE rendez_vous_id = ?}). Pas de transaction.</p>
     *
     * @param rdvId identifiant du rendez-vous
     * @return la consultation trouvée, ou {@code null} si absente ou en cas d'erreur
     */
    public Consultation findByRendezVousId(Long rdvId) {
        String sql = "SELECT id, rendez_vous_id, diagnostic, remarque, created_at FROM consultation WHERE rendez_vous_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, rdvId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Consultation c = new Consultation();
                    c.setId(rs.getLong("id"));
                    c.setRendezVousId(rs.getLong("rendez_vous_id"));
                    c.setDiagnostic(rs.getString("diagnostic"));
                    c.setRemarque(rs.getString("remarque"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        c.setCreatedAt(createdAt.toInstant());
                    }
                    return c;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Insère une nouvelle consultation.
     *
     * <p><strong>SQL :</strong> {@code INSERT INTO consultation}
     * (rendez_vous_id, diagnostic, remarque) avec clé générée. Auto-commit.</p>
     *
     * @param consultation entité à enregistrer
     * @return la même entité avec {@code id} renseigné si succès
     * @throws RuntimeException en cas d'erreur JDBC
     */
    public Consultation save(Consultation consultation) {
    	String sql = "INSERT INTO consultation (rendez_vous_id, diagnostic, remarque, date_consultation) VALUES (?, ?, ?, CURDATE())";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, consultation.getRendezVousId());
            ps.setString(2, consultation.getDiagnostic());
            ps.setString(3, consultation.getRemarque());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) consultation.setId(keys.getLong(1));
            }
            return consultation;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'enregistrement de la consultation", e);
        }
    }

    /**
     * Met à jour le diagnostic et la remarque d'une consultation existante.
     *
     * <p><strong>SQL :</strong> {@code UPDATE consultation SET diagnostic = ?, remarque = ? WHERE id = ?}.
     * Auto-commit.</p>
     *
     * @param consultation entité avec identifiant et champs modifiables
     * @throws RuntimeException en cas d'erreur JDBC
     */
    public void update(Consultation consultation) {
        String sql = "UPDATE consultation SET diagnostic = ?, remarque = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, consultation.getDiagnostic());
            ps.setString(2, consultation.getRemarque());
            ps.setLong(3, consultation.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la consultation", e);
        }
    }

    /**
     * Sauvegarde atomique du formulaire complet de consultation (consultation, ordonnance,
     * médicaments, analyses, statut du rendez-vous).
     *
     * <p><strong>SQL (transaction, {@code setAutoCommit(false)}) :</strong></p>
     * <ul>
     *   <li>{@code SELECT id FROM consultation} ou {@code INSERT}/{@code UPDATE consultation}</li>
     *   <li>{@code SELECT}/{@code INSERT}/{@code DELETE} sur {@code ordonnance} et {@code medicament_ordonnance}</li>
     *   <li>{@code DELETE} puis {@code INSERT} batch sur {@code analyse_demandee}</li>
     *   <li>{@code UPDATE rendez_vous SET statut = 'TERMINE'}</li>
     * </ul>
     * <p>{@code commit} en succès, {@code rollback} en erreur.</p>
     *
     * @param rendezVousId identifiant du rendez-vous lié
     * @param diagnostic   texte du diagnostic
     * @param remarque     remarques complémentaires
     * @param medicaments  lignes d'ordonnance (peut être vide)
     * @param codesAnalyse codes d'analyses demandées (peut être vide)
     * @return identifiant de la consultation persistée
     * @throws RuntimeException si la transaction échoue
     */
    public Long saveFullConsultation(Long rendezVousId,
                                     String diagnostic,
                                     String remarque,
                                     List<MedicamentOrdonnance> medicaments,
                                     List<String> codesAnalyse) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            Long consultationId = findConsultationIdByRendezVousId(con, rendezVousId);
            if (consultationId == null) {
                consultationId = insertConsultation(con, rendezVousId, diagnostic, remarque);
            } else {
                updateConsultation(con, consultationId, diagnostic, remarque);
            }

            saveOrdonnanceAndMedicaments(con, consultationId, medicaments);
            replaceAnalyses(con, consultationId, codesAnalyse);
            updateRendezVousStatut(con, rendezVousId, "TERMINE");

            con.commit();
            return consultationId;
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ignored) {
                    // noop
                }
            }
            throw new RuntimeException("Erreur sauvegarde transactionnelle de la consultation", e);
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
     * Recherche l'identifiant consultation pour un rendez-vous (connexion transactionnelle).
     *
     * <p><strong>SQL :</strong> {@code SELECT id FROM consultation WHERE rendez_vous_id = ?}.</p>
     *
     * @param con         connexion avec transaction ouverte
     * @param rendezVousId identifiant du rendez-vous
     * @return id consultation ou {@code null}
     * @throws Exception en cas d'erreur JDBC
     */
    private Long findConsultationIdByRendezVousId(Connection con, Long rendezVousId) throws Exception {
        String sql = "SELECT id FROM consultation WHERE rendez_vous_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, rendezVousId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        return null;
    }

    /**
     * Insère une consultation dans une transaction en cours.
     *
     * <p><strong>SQL :</strong> {@code INSERT INTO consultation} avec clé générée.</p>
     *
     * @param con          connexion transactionnelle
     * @param rendezVousId identifiant du rendez-vous
     * @param diagnostic   diagnostic
     * @param remarque     remarque
     * @return identifiant généré
     * @throws Exception si l'insertion ou la récupération de clé échoue
     */
    private Long insertConsultation(Connection con, Long rendezVousId, String diagnostic, String remarque) throws Exception {
    	String sql = "INSERT INTO consultation (rendez_vous_id, diagnostic, remarque, date_consultation) VALUES (?, ?, ?, CURDATE())";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rendezVousId);
            ps.setString(2, diagnostic);
            ps.setString(3, remarque);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new IllegalStateException("Impossible de récupérer l'id consultation généré");
    }

    /**
     * Met à jour diagnostic et remarque dans une transaction en cours.
     *
     * <p><strong>SQL :</strong> {@code UPDATE consultation SET diagnostic = ?, remarque = ? WHERE id = ?}.</p>
     *
     * @param con            connexion transactionnelle
     * @param consultationId identifiant consultation
     * @param diagnostic     diagnostic
     * @param remarque       remarque
     * @throws Exception en cas d'erreur JDBC
     */
    private void updateConsultation(Connection con, Long consultationId, String diagnostic, String remarque) throws Exception {
        String sql = "UPDATE consultation SET diagnostic = ?, remarque = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, diagnostic);
            ps.setString(2, remarque);
            ps.setLong(3, consultationId);
            ps.executeUpdate();
        }
    }

    /**
     * Crée, met à jour ou supprime l'ordonnance et ses médicaments selon la liste fournie.
     *
     * <p><strong>SQL :</strong> combinaison de {@code SELECT}/{@code INSERT}/{@code DELETE}
     * sur {@code ordonnance} et {@code medicament_ordonnance} (voir méthodes privées appelées).</p>
     *
     * @param con            connexion transactionnelle
     * @param consultationId identifiant consultation
     * @param medicaments    lignes médicamenteuses
     * @throws Exception en cas d'erreur JDBC
     */
    private void saveOrdonnanceAndMedicaments(Connection con, Long consultationId, List<MedicamentOrdonnance> medicaments) throws Exception {
        Long ordonnanceId = findOrdonnanceIdByConsultationId(con, consultationId);
        boolean hasMedicaments = medicaments != null && !medicaments.isEmpty();

        if (!hasMedicaments) {
            if (ordonnanceId != null) {
                deleteMedicamentsByOrdonnanceId(con, ordonnanceId);
                deleteOrdonnanceById(con, ordonnanceId);
            }
            return;
        }

        if (ordonnanceId == null) {
            ordonnanceId = insertOrdonnance(con, consultationId);
        }
        deleteMedicamentsByOrdonnanceId(con, ordonnanceId);
        insertMedicaments(con, ordonnanceId, medicaments);
    }

    /**
     * Recherche l'identifiant ordonnance lié à une consultation (transaction).
     *
     * <p><strong>SQL :</strong> {@code SELECT id FROM ordonnance WHERE consultation_id = ?}.</p>
     *
     * @param con            connexion transactionnelle
     * @param consultationId identifiant consultation
     * @return id ordonnance ou {@code null}
     * @throws Exception en cas d'erreur JDBC
     */
    private Long findOrdonnanceIdByConsultationId(Connection con, Long consultationId) throws Exception {
        String sql = "SELECT id FROM ordonnance WHERE consultation_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, consultationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        return null;
    }

    /**
     * Insère une ordonnance prescrite pour une consultation.
     *
     * <p><strong>SQL :</strong> {@code INSERT INTO ordonnance}
     * (consultation_id, statut PRESCRITE, pharmacie_id NULL).</p>
     *
     * @param con            connexion transactionnelle
     * @param consultationId identifiant consultation
     * @return identifiant ordonnance généré
     * @throws Exception si la clé générée est absente
     */
    private Long insertOrdonnance(Connection con, Long consultationId) throws Exception {
        String sql = "INSERT INTO ordonnance (consultation_id, statut, pharmacie_id) VALUES (?, ?, NULL)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, consultationId);
            ps.setString(2, Ordonnance.Statut.PRESCRITE.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new IllegalStateException("Impossible de récupérer l'id ordonnance généré");
    }

    /**
     * Supprime toutes les lignes médicament d'une ordonnance.
     *
     * <p><strong>SQL :</strong> {@code DELETE FROM medicament_ordonnance WHERE ordonnance_id = ?}.</p>
     *
     * @param con          connexion transactionnelle
     * @param ordonnanceId identifiant ordonnance
     * @throws Exception en cas d'erreur JDBC
     */
    private void deleteMedicamentsByOrdonnanceId(Connection con, Long ordonnanceId) throws Exception {
        String sql = "DELETE FROM medicament_ordonnance WHERE ordonnance_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, ordonnanceId);
            ps.executeUpdate();
        }
    }

    /**
     * Supprime une ordonnance par identifiant.
     *
     * <p><strong>SQL :</strong> {@code DELETE FROM ordonnance WHERE id = ?}.</p>
     *
     * @param con          connexion transactionnelle
     * @param ordonnanceId identifiant ordonnance
     * @throws Exception en cas d'erreur JDBC
     */
    private void deleteOrdonnanceById(Connection con, Long ordonnanceId) throws Exception {
        String sql = "DELETE FROM ordonnance WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, ordonnanceId);
            ps.executeUpdate();
        }
    }

    /**
     * Insère les lignes médicament en lot (batch).
     *
     * <p><strong>SQL :</strong> {@code INSERT INTO medicament_ordonnance} en {@code executeBatch}.</p>
     *
     * @param con          connexion transactionnelle
     * @param ordonnanceId identifiant ordonnance
     * @param medicaments  lignes à insérer
     * @throws Exception en cas d'erreur JDBC
     */
    private void insertMedicaments(Connection con, Long ordonnanceId, List<MedicamentOrdonnance> medicaments) throws Exception {
        String sql = """
                INSERT INTO medicament_ordonnance (ordonnance_id, nom, posologie, duree, ligne_ordre)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int ligneOrdre = 1;
            for (MedicamentOrdonnance m : medicaments) {
                ps.setLong(1, ordonnanceId);
                ps.setString(2, m.getNom());
                ps.setString(3, m.getPosologie());
                ps.setString(4, m.getDuree());
                ps.setInt(5, ligneOrdre++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Remplace l'ensemble des analyses demandées pour une consultation.
     *
     * <p><strong>SQL :</strong> {@code DELETE FROM analyse_demandee WHERE consultation_id = ?},
     * puis {@code INSERT} batch sur {@code analyse_demandee} (codes dédupliqués).</p>
     *
     * @param con            connexion transactionnelle
     * @param consultationId identifiant consultation
     * @param codesAnalyse     codes d'analyses (peut être vide)
     * @throws Exception en cas d'erreur JDBC
     */
    private void replaceAnalyses(Connection con, Long consultationId, List<String> codesAnalyse) throws Exception {
        String deleteSql = "DELETE FROM analyse_demandee WHERE consultation_id = ?";
        try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
            ps.setLong(1, consultationId);
            ps.executeUpdate();
        }

        if (codesAnalyse == null || codesAnalyse.isEmpty()) {
            return;
        }

        String insertSql = "INSERT INTO analyse_demandee (consultation_id, code_analyse) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(insertSql)) {
            Set<String> seen = new LinkedHashSet<>();
            for (String raw : codesAnalyse) {
                if (raw == null) continue;
                String code = raw.trim();
                if (code.isEmpty() || !seen.add(code)) continue;
                ps.setLong(1, consultationId);
                ps.setString(2, code);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Met à jour le statut du rendez-vous lié (dans la transaction).
     *
     * <p><strong>SQL :</strong> {@code UPDATE rendez_vous SET statut = ? WHERE id = ?}.</p>
     *
     * @param con          connexion transactionnelle
     * @param rendezVousId identifiant rendez-vous
     * @param statut       valeur du statut (ex. TERMINE)
     * @throws Exception en cas d'erreur JDBC
     */
    private void updateRendezVousStatut(Connection con, Long rendezVousId, String statut) throws Exception {
        String sql = "UPDATE rendez_vous SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setLong(2, rendezVousId);
            ps.executeUpdate();
        }
    }

    /**
     * Compte les consultations terminées aujourd'hui pour un médecin.
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} avec jointure {@code consultation} /
     * {@code rendez_vous}, filtre {@code medecin_id} et {@code date_rendez_vous = CURDATE()}.
     * Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @return nombre de consultations du jour, ou {@code 0} en cas d'erreur
     */
    public long countCompletedTodayForMedecin(Long medecinId) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM consultation c
                INNER JOIN rendez_vous rv ON rv.id = c.rendez_vous_id
                WHERE rv.medecin_id = ?
                  AND rv.date_rendez_vous = CURDATE()
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Liste les consultations d'un médecin pour une date (avec infos rendez-vous et patient).
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code consultation}, jointures
     * {@code rendez_vous} et {@code user} (patient), filtre médecin et date.
     * Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param date      date des consultations
     * @return liste enrichie pour l'affichage agenda
     */
    public List<Consultation> findByMedecinAndDate(Long medecinId, LocalDate date) {
        List<Consultation> result = new ArrayList<>();
        String sql = """
                SELECT c.id, c.rendez_vous_id, c.diagnostic, c.remarque,
                       rv.date_rendez_vous, rv.start_time, rv.statut,
                       CONCAT(pu.nom, ' ', pu.prenom) AS patient_nom
                FROM consultation c
                INNER JOIN rendez_vous rv ON rv.id = c.rendez_vous_id
                INNER JOIN user pu ON pu.id = rv.patient_id
                WHERE rv.medecin_id = ? AND rv.date_rendez_vous = ?
                ORDER BY rv.start_time
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Consultation c = new Consultation();
                    c.setId(rs.getLong("id"));
                    c.setRendezVousId(rs.getLong("rendez_vous_id"));
                    c.setDiagnostic(rs.getString("diagnostic"));
                    c.setRemarque(rs.getString("remarque"));
                    c.setDateConsultation(rs.getDate("date_rendez_vous").toLocalDate());
                    c.setHeureConsultation(rs.getTime("start_time").toLocalTime());
                    c.setPatientNomComplet(rs.getString("patient_nom"));
                    String motif = c.getDiagnostic();
                    if (motif == null || motif.isBlank()) motif = c.getRemarque();
                    c.setMotif(motif == null ? "Sans motif" : motif);
                    c.setStatut(rs.getString("statut"));
                    result.add(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Agrège le nombre de consultations par date pour un médecin sur une période.
     *
     * <p><strong>SQL :</strong> {@code SELECT rv.date_rendez_vous, COUNT(*)} avec jointures
     * {@code consultation} / {@code rendez_vous}, {@code GROUP BY} date. Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param startDate date de début (incluse)
     * @param endDate   date de fin (incluse)
     * @return map date → nombre de consultations
     */
    public Map<LocalDate, Long> countByMedecinGroupedByDate(Long medecinId, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        String sql = """
                SELECT rv.date_rendez_vous, COUNT(*) AS total
                FROM consultation c
                INNER JOIN rendez_vous rv ON rv.id = c.rendez_vous_id
                WHERE rv.medecin_id = ?
                  AND rv.date_rendez_vous BETWEEN ? AND ?
                GROUP BY rv.date_rendez_vous
                ORDER BY rv.date_rendez_vous
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getDate("date_rendez_vous").toLocalDate(), rs.getLong("total"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return counts;
    }
}

package com.cabinet.dao;

import com.cabinet.model.Patient;
import com.cabinet.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès JDBC aux patients (tables {@code user} et {@code patient}, jointure avec {@code rendez_vous}).
 *
 * <p>Un patient est modélisé par un compte {@code user} (rôle PATIENT) et une extension
 * {@code patient}. La création utilise une transaction ; la suppression cible {@code user}
 * (cascade vers {@code patient} selon le schéma).</p>
 */
public class PatientDAO {

    /**
     * Recherche un patient par identifiant utilisateur.
     *
     * <p><strong>SQL :</strong> {@code SELECT} avec {@code INNER JOIN} entre {@code user} u
     * et {@code patient} p sur {@code u.id = p.id}, filtre {@code WHERE u.id = ?}.
     * Pas de transaction.</p>
     *
     * @param id identifiant du patient (clé primaire partagée user/patient)
     * @return le patient trouvé, ou {@code null} si absent ou en cas d'erreur
     */
    public Patient findById(Long id) {
        String sql = """
                SELECT u.id, u.nom, u.prenom, u.email, u.password, u.telephone, u.role, u.active,
                       p.cin, p.adresse, p.date_naissance
                FROM `user` u
                INNER JOIN patient p ON u.id = p.id
                WHERE u.id = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPatient(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Liste tous les patients, triés par nom puis prénom.
     *
     * <p><strong>SQL :</strong> {@code SELECT} avec {@code INNER JOIN} {@code user} / {@code patient},
     * {@code ORDER BY u.nom, u.prenom}. Pas de transaction.</p>
     *
     * @return liste des patients (vide si aucun ou en cas d'erreur)
     */
    public List<Patient> findAll() {
        List<Patient> result = new ArrayList<>();
        String sql = """
                SELECT u.id, u.nom, u.prenom, u.email, u.password, u.telephone, u.role, u.active,
                       p.cin, p.adresse, p.date_naissance
                FROM `user` u
                INNER JOIN patient p ON u.id = p.id
                ORDER BY u.nom, u.prenom
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapPatient(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Liste les patients ayant un rendez-vous actif (non annulé) chez un médecin à une date donnée.
     *
     * <p><strong>SQL :</strong> {@code SELECT DISTINCT} sur {@code patient}, {@code user},
     * {@code rendez_vous} ; jointures et filtres {@code medecin_id}, {@code date_rendez_vous},
     * {@code statut <> 'ANNULE'}. Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param date      date du rendez-vous
     * @return liste distincte des patients concernés
     */
    public List<Patient> findByMedecinAndDateWithActivity(Long medecinId, LocalDate date) {
        List<Patient> result = new ArrayList<>();
        String sql = """
                SELECT DISTINCT u.id, u.nom, u.prenom, u.email, u.password, u.telephone, u.role, u.active,
                                p.cin, p.adresse, p.date_naissance
                FROM patient p
                INNER JOIN `user` u ON u.id = p.id
                INNER JOIN rendez_vous rv ON rv.patient_id = p.id
                WHERE rv.medecin_id = ?
                  AND rv.date_rendez_vous = ?
                  AND rv.statut <> 'ANNULE'
                ORDER BY u.nom, u.prenom
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapPatient(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Compte le nombre total d'enregistrements dans la table {@code patient}.
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} sur {@code patient}. Pas de transaction.</p>
     *
     * @return nombre de patients, ou {@code 0} en cas d'erreur
     */
    public long countAll() {
        String sql = "SELECT COUNT(*) AS total FROM patient";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong("total");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Enregistre un nouveau patient (compte utilisateur + fiche patient).
     *
     * <p><strong>SQL (transaction) :</strong></p>
     * <ul>
     *   <li>{@code INSERT INTO user} (nom, prenom, email, password, telephone, role)</li>
     *   <li>{@code INSERT INTO patient} (id, cin, adresse, date_naissance) avec clé générée</li>
     * </ul>
     * <p>{@code setAutoCommit(false)} ; {@code commit} ou {@code rollback} selon le résultat.</p>
     *
     * @param p entité patient à créer
     * @return {@code null} en cas de succès ; sinon code d'erreur
     *         ({@code db_connection_failed}, {@code generated_key_missing}, {@code email_deja_utilise},
     *         {@code cin_deja_utilise}, {@code password_vide}, {@code contrainte_integrite},
     *         {@code sql_error_state_...}, {@code unexpected_error})
     */
    public String savePatient(Patient p) {
        String sqlUser = "INSERT INTO `user` (nom, prenom, email, password, telephone, role) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlPatient = "INSERT INTO patient (id, cin, adresse, date_naissance) VALUES (?, ?, ?, ?)";

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            if (con == null) {
                return "db_connection_failed";
            }
            con.setAutoCommit(false);

            try (PreparedStatement psUser = con.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, p.getNom());
                psUser.setString(2, p.getPrenom());
                psUser.setString(3, p.getEmail());
                psUser.setString(4, p.getPassword());
                psUser.setString(5, p.getTelephone());
                psUser.setString(6, p.getRole().name());
                psUser.executeUpdate();

                try (ResultSet keys = psUser.getGeneratedKeys()) {
                    if (!keys.next()) {
                        con.rollback();
                        return "generated_key_missing";
                    }

                    long userId = keys.getLong(1);
                    p.setId(userId);

                    try (PreparedStatement psPatient = con.prepareStatement(sqlPatient)) {
                        psPatient.setLong(1, userId);
                        psPatient.setString(2, p.getCin());
                        psPatient.setString(3, p.getAdresse());
                        psPatient.setDate(4, Date.valueOf(p.getDateNaissance()));
                        psPatient.executeUpdate();
                    }
                }
            }

            con.commit();
            return null;
        } catch (SQLIntegrityConstraintViolationException e) {
            rollbackQuietly(con);
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("uk_user_email") || msg.contains("for key 'uk_user_email'")) {
                return "email_deja_utilise";
            }
            if (msg.contains("uk_patient_cin") || msg.contains("for key 'uk_patient_cin'")) {
                return "cin_deja_utilise";
            }
            if (msg.contains("cannot be null") && msg.contains("password")) {
                return "password_vide";
            }
            return "contrainte_integrite";
        } catch (SQLException e) {
            rollbackQuietly(con);
            e.printStackTrace();
            return "sql_error_state_" + e.getSQLState() + "_code_" + e.getErrorCode();
        } catch (Exception e) {
            rollbackQuietly(con);
            e.printStackTrace();
            return "unexpected_error";
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Supprime un patient en supprimant son compte utilisateur.
     *
     * <p><strong>SQL :</strong> {@code DELETE FROM user WHERE id = ?}
     * (la ligne {@code patient} est supprimée en cascade selon le schéma FK).
     * Pas de transaction explicite.</p>
     *
     * @param patientId identifiant du patient (id user)
     */
    public void deleteById(Long patientId) {
        // FK patient -> user (ON DELETE CASCADE) dans ton schema.sql, mais on supprime explicitement l'user.
        String sql = "DELETE FROM `user` WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Mappe une ligne jointe {@code user}/{@code patient} vers un objet {@link Patient}.
     *
     * @param rs curseur positionné sur la ligne courante
     * @return entité patient remplie
     * @throws Exception si la lecture d'une colonne échoue
     */
    private Patient mapPatient(ResultSet rs) throws Exception {
        Patient patient = new Patient();
        patient.setId(rs.getLong("id"));
        patient.setNom(rs.getString("nom"));
        patient.setPrenom(rs.getString("prenom"));
        patient.setEmail(rs.getString("email"));
        patient.setPassword(rs.getString("password"));
        patient.setTelephone(rs.getString("telephone"));
        patient.setRole(com.cabinet.model.User.Role.valueOf(rs.getString("role")));
        patient.setActive(rs.getBoolean("active"));
        patient.setCin(rs.getString("cin"));
        patient.setAdresse(rs.getString("adresse"));
        patient.setDateNaissance(rs.getDate("date_naissance").toLocalDate());
        return patient;
    }

    /**
     * Annule la transaction en cours sans propager d'exception.
     *
     * @param con connexion JDBC, éventuellement {@code null}
     */
    private void rollbackQuietly(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (Exception ignored) {}
        }
    }
}

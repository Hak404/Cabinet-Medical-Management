package com.cabinet.dao;

import com.cabinet.model.Cabinet;
import com.cabinet.model.Medecin;
import com.cabinet.model.User;
import com.cabinet.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès JDBC aux cabinets médicaux (tables {@code cabinet}, {@code medecin}, {@code user}).
 *
 * <p>Gère la lecture et l'écriture des cabinets, les contrôles d'existence du médecin,
 * et la création atomique cabinet + médecin dans une transaction multi-tables.</p>
 */
public class CabinetDAO {

    private static final String SELECT_CABINET = """
            SELECT id, nom, adresse, duree_consultation_minutes AS duree, medecin_id
            FROM cabinet
            """;

    /**
     * Recherche un cabinet par identifiant.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code cabinet} avec {@code WHERE id = ?}.
     * Pas de transaction.</p>
     *
     * @param id identifiant du cabinet
     * @return le cabinet trouvé, ou {@code null} si absent ou en cas d'erreur
     */
    public Cabinet findById(Long id) {
        String sql = SELECT_CABINET + " WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCabinet(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Liste tous les cabinets, triés par nom.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code cabinet}, {@code ORDER BY nom}.
     * Pas de transaction.</p>
     *
     * @return liste des cabinets
     */
    public List<Cabinet> findAll() {
        List<Cabinet> cabinets = new ArrayList<>();
        String sql = SELECT_CABINET + " ORDER BY nom";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cabinets.add(mapCabinet(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cabinets;
    }

    /**
     * Compte le nombre total de cabinets.
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} sur {@code cabinet}. Pas de transaction.</p>
     *
     * @return nombre de cabinets, ou {@code 0} en cas d'erreur
     */
    public long countAll() {
        String sql = "SELECT COUNT(*) AS total FROM cabinet";
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
     * Ajoute un cabinet pour un médecin existant (après contrôles métier).
     *
     * <p><strong>SQL :</strong></p>
     * <ul>
     *   <li>{@code SELECT 1 FROM medecin} (existence du médecin)</li>
     *   <li>{@code SELECT 1 FROM cabinet WHERE medecin_id = ?} (unicité médecin/cabinet)</li>
     *   <li>{@code INSERT INTO cabinet} (nom, adresse, duree_consultation_minutes, medecin_id)</li>
     * </ul>
     * <p>Pas de transaction explicite sur une seule connexion ; auto-commit par défaut.</p>
     *
     * @param cabinet entité à insérer (medecinId obligatoire)
     * @return {@code null} si succès ; sinon code d'erreur
     *         ({@code medecinId_required}, {@code medecin_inexistant}, {@code medecin_deja_affecte},
     *         {@code email_deja_utilise}, {@code cabinet_nom_deja_existant}, codes SQL, etc.)
     */
    public String addCabinet(Cabinet cabinet) {
        if (cabinet.getMedecinId() == null) {
            return "medecinId_required";
        }
        try (Connection con = DBConnection.getConnection()) {
            if (!existsMedecin(con, cabinet.getMedecinId())) {
                return "medecin_inexistant";
            }
            if (existsCabinetForMedecin(con, cabinet.getMedecinId())) {
                return "medecin_deja_affecte";
            }
            return insertCabinet(con, cabinet);
        } catch (SQLIntegrityConstraintViolationException e) {
            return mapIntegrityError(e);
        } catch (SQLException e) {
            e.printStackTrace();
            return formatSqlError(e);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause instanceof SQLException sqlEx) {
                return formatSqlError(sqlEx);
            }
            return "runtime_error_" + safeMsg(e);
        } catch (Exception e) {
            e.printStackTrace();
            return "unexpected_error_" + safeMsg(e);
        }
    }

    /**
     * Supprime un cabinet par identifiant.
     *
     * <p><strong>SQL :</strong> {@code DELETE FROM cabinet WHERE id = ?}. Pas de transaction.</p>
     *
     * @param id identifiant du cabinet à supprimer
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM cabinet WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Crée un médecin et son cabinet associé dans une seule transaction.
     *
     * <p><strong>SQL (transaction) :</strong></p>
     * <ul>
     *   <li>{@code INSERT INTO user} (compte médecin, clé générée)</li>
     *   <li>{@code INSERT INTO medecin} (id, specialite, heure_debut, heure_fin)</li>
     *   <li>{@code INSERT INTO cabinet} (nom, adresse, duree, medecin_id)</li>
     * </ul>
     * <p>{@code setAutoCommit(false)} ; {@code commit} ou {@code rollback}.</p>
     *
     * @param cabinet entité cabinet (medecinId sera renseigné après création du user)
     * @param medecin données du médecin à créer
     * @return {@code null} si succès ; sinon code d'erreur métier ou {@code unexpected_error}
     */
    public String createCabinetWithMedecin(Cabinet cabinet, Medecin medecin) {
        String sqlUser = """
                INSERT INTO `user` (nom, prenom, email, password, telephone, role)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        String sqlMed = """
                INSERT INTO medecin (id, specialite, heure_debut, heure_fin)
                VALUES (?, ?, ?, ?)
                """;

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            long userId;
            try (PreparedStatement psUser = con.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, medecin.getNom());
                psUser.setString(2, medecin.getPrenom());
                psUser.setString(3, medecin.getEmail());
                psUser.setString(4, medecin.getPassword());
                psUser.setString(5, medecin.getTelephone());
                psUser.setString(6, User.Role.MEDECIN.name());
                psUser.executeUpdate();

                try (ResultSet keys = psUser.getGeneratedKeys()) {
                    if (!keys.next()) {
                        con.rollback();
                        return "generated_key_missing";
                    }
                    userId = keys.getLong(1);
                }
            }

            LocalTime hd = medecin.getHeureDebut() == null ? LocalTime.of(9, 0) : medecin.getHeureDebut();
            LocalTime hf = medecin.getHeureFin() == null ? LocalTime.of(17, 0) : medecin.getHeureFin();

            try (PreparedStatement psMed = con.prepareStatement(sqlMed)) {
                psMed.setLong(1, userId);
                psMed.setString(2, medecin.getSpecialite());
                psMed.setTime(3, Time.valueOf(hd));
                psMed.setTime(4, Time.valueOf(hf));
                psMed.executeUpdate();
            }

            cabinet.setMedecinId(userId);
            String errCab = insertCabinetTx(con, cabinet);
            if (errCab != null) {
                con.rollback();
                return errCab;
            }

            con.commit();
            return null;

        } catch (SQLIntegrityConstraintViolationException e) {
            rollbackQuietly(con);
            return mapIntegrityError(e);
        } catch (Exception e) {
            rollbackQuietly(con);
            e.printStackTrace();
            return "unexpected_error";
        } finally {
            closeQuietly(con);
        }
    }

    /**
     * Insère un cabinet sur une connexion existante (mode auto-commit).
     *
     * <p><strong>SQL :</strong> {@code INSERT INTO cabinet} avec récupération de la clé générée.</p>
     *
     * @param con     connexion JDBC active
     * @param cabinet entité à persister
     * @return {@code null} si succès
     * @throws Exception en cas d'erreur JDBC
     */
    private String insertCabinet(Connection con, Cabinet cabinet) throws Exception {
        String sql = """
                INSERT INTO cabinet (nom, adresse, duree_consultation_minutes, medecin_id)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cabinet.getNom());
            ps.setString(2, cabinet.getAdresse());
            ps.setInt(3, cabinet.getDureeConsultationMinutes());
            ps.setLong(4, cabinet.getMedecinId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    cabinet.setId(keys.getLong(1));
                }
            }
        }
        return null;
    }

    /**
     * Insère un cabinet dans le cadre d'une transaction en cours.
     *
     * <p><strong>SQL :</strong> identique à {@link #insertCabinet(Connection, Cabinet)}
     * mais sans {@code commit} (géré par l'appelant).</p>
     *
     * @param con     connexion avec {@code autoCommit = false}
     * @param cabinet entité à persister
     * @return {@code null} si succès
     * @throws Exception en cas d'erreur JDBC
     */
    private String insertCabinetTx(Connection con, Cabinet cabinet) throws Exception {
        String sql = """
                INSERT INTO cabinet (nom, adresse, duree_consultation_minutes, medecin_id)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cabinet.getNom());
            ps.setString(2, cabinet.getAdresse());
            ps.setInt(3, cabinet.getDureeConsultationMinutes());
            ps.setLong(4, cabinet.getMedecinId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    cabinet.setId(keys.getLong(1));
                }
            }
        }
        return null;
    }

    /**
     * Mappe une ligne {@code cabinet} vers un objet {@link Cabinet}.
     *
     * @param rs curseur positionné sur la ligne courante
     * @return entité cabinet remplie
     * @throws Exception si la lecture d'une colonne échoue
     */
    private Cabinet mapCabinet(ResultSet rs) throws Exception {
        Cabinet cabinet = new Cabinet();
        cabinet.setId(rs.getLong("id"));
        cabinet.setNom(rs.getString("nom"));
        cabinet.setAdresse(rs.getString("adresse"));
        cabinet.setDureeConsultationMinutes(rs.getInt("duree"));
        long mid = rs.getLong("medecin_id");
        cabinet.setMedecinId(rs.wasNull() ? null : mid);
        return cabinet;
    }

    /**
     * Vérifie l'existence d'un médecin par identifiant.
     *
     * <p><strong>SQL :</strong> {@code SELECT 1 FROM medecin WHERE id = ? LIMIT 1}.</p>
     *
     * @param con       connexion JDBC
     * @param medecinId identifiant du médecin
     * @return {@code true} si le médecin existe
     * @throws Exception en cas d'erreur JDBC
     */
    private boolean existsMedecin(Connection con, Long medecinId) throws Exception {
        String sql = "SELECT 1 FROM medecin WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Vérifie si un cabinet est déjà rattaché à ce médecin.
     *
     * <p><strong>SQL :</strong> {@code SELECT 1 FROM cabinet WHERE medecin_id = ? LIMIT 1}.</p>
     *
     * @param con       connexion JDBC
     * @param medecinId identifiant du médecin
     * @return {@code true} si un cabinet existe pour ce médecin
     * @throws Exception en cas d'erreur JDBC
     */
    private boolean existsCabinetForMedecin(Connection con, Long medecinId) throws Exception {
        String sql = "SELECT 1 FROM cabinet WHERE medecin_id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Traduit une violation d'intégrité SQL en code d'erreur métier.
     *
     * @param e exception de contrainte
     * @return code lisible par la couche présentation
     */
    private String mapIntegrityError(SQLIntegrityConstraintViolationException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("uk_user_email")) {
            return "email_deja_utilise";
        }
        if (msg.contains("uk_cabinet_nom") || msg.contains("for key 'uk_cabinet_nom'")) {
            return "cabinet_nom_deja_existant";
        }
        if (msg.contains("uk_cabinet_medecin") || (msg.contains("medecin_id") && msg.contains("duplicate"))) {
            return "medecin_deja_affecte";
        }
        return formatSqlError(e);
    }

    /**
     * Formate une {@link SQLException} en chaîne de diagnostic.
     *
     * @param e exception SQL
     * @return chaîne contenant l'état SQL, le code et le message
     */
    private String formatSqlError(SQLException e) {
        return "sql_error_state_" + e.getSQLState()
                + "_code_" + e.getErrorCode()
                + "_msg_" + safeMsg(e);
    }

    /**
     * Nettoie le message d'une exception pour l'affichage.
     *
     * @param t throwable source
     * @return message sur une ligne, ou {@code no_message}
     */
    private String safeMsg(Throwable t) {
        String m = t == null ? null : t.getMessage();
        if (m == null) {
            return "no_message";
        }
        return m.replaceAll("[\\r\\n\\t]+", " ").trim();
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
            } catch (Exception ignored) {
                // noop
            }
        }
    }

    /**
     * Réactive l'auto-commit et ferme la connexion.
     *
     * @param con connexion JDBC, éventuellement {@code null}
     */
    private void closeQuietly(Connection con) {
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

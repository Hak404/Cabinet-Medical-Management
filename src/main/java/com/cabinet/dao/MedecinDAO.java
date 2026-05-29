package com.cabinet.dao;

import com.cabinet.model.Medecin;
import com.cabinet.model.User;
import com.cabinet.util.DBConnection;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès JDBC aux médecins (tables {@code user}, {@code medecin}, jointure optionnelle {@code cabinet}).
 *
 * <p>La création d'un médecin insère d'abord un compte {@code user} puis la fiche {@code medecin}
 * dans une transaction. La suppression cible {@code user} (cascade selon le schéma).</p>
 */
public class MedecinDAO {

    /**
     * Recherche un médecin par identifiant, avec le cabinet associé s'il existe.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code user} u
     * {@code INNER JOIN medecin} m, {@code LEFT JOIN cabinet} c sur {@code c.medecin_id = m.id},
     * filtre {@code WHERE u.id = ?}. Pas de transaction.</p>
     *
     * @param id identifiant du médecin
     * @return le médecin trouvé, ou {@code null} si absent ou en cas d'erreur
     */
    public Medecin findById(Long id) {

        String sql = """
            SELECT u.id, u.nom, u.prenom, u.email, u.password, u.telephone, u.role, u.active,
                   m.specialite, m.heure_debut, m.heure_fin,
                   c.id AS cabinet_id, c.nom AS cabinet_nom
            FROM `user` u
            INNER JOIN medecin m ON u.id = m.id
            LEFT JOIN cabinet c ON c.medecin_id = m.id
            WHERE u.id = ?
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMedecin(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Liste tous les médecins avec leur cabinet éventuel.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code user}, {@code medecin},
     * {@code LEFT JOIN cabinet}. Pas de transaction.</p>
     *
     * @return liste des médecins (vide si aucun ou en cas d'erreur)
     */
    public List<Medecin> findAll() {

        List<Medecin> list = new ArrayList<>();

        String sql = """
            SELECT u.id, u.nom, u.prenom, u.email, u.password, u.telephone, u.role, u.active,
                   m.specialite, m.heure_debut, m.heure_fin,
                   c.id AS cabinet_id, c.nom AS cabinet_nom
            FROM `user` u
            INNER JOIN medecin m ON u.id = m.id
            LEFT JOIN cabinet c ON c.medecin_id = m.id
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapMedecin(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Compte le nombre d'enregistrements dans la table {@code medecin}.
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} sur {@code medecin}. Pas de transaction.</p>
     *
     * @return nombre de médecins, ou {@code 0} en cas d'erreur
     */
    public long countAll() {

        String sql = "SELECT COUNT(*) FROM medecin";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getLong(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Crée un nouveau médecin (compte utilisateur + fiche médecin).
     *
     * <p><strong>SQL (transaction) :</strong></p>
     * <ul>
     *   <li>{@code INSERT INTO user} (rôle MEDECIN, clé générée)</li>
     *   <li>{@code INSERT INTO medecin} (id, specialite, heure_debut, heure_fin)</li>
     * </ul>
     * <p>{@code setAutoCommit(false)} ; {@code commit} ou {@code rollback}.</p>
     *
     * @param medecin    données identité et spécialité
     * @param heureDebut heure de début de consultation
     * @param heureFin   heure de fin de consultation
     * @return {@code null} si succès ; sinon {@code generated_key_missing} ou {@code unexpected_error}
     */
    public String addMedecin(Medecin medecin, LocalTime heureDebut, LocalTime heureFin) {

        String sqlUser =
                "INSERT INTO `user` (nom, prenom, email, password, telephone, role) VALUES (?, ?, ?, ?, ?, ?)";

        String sqlMed =
                "INSERT INTO medecin (id, specialite, heure_debut, heure_fin) VALUES (?, ?, ?, ?)";

        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            long userId;

            // ===== USER =====
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

            // ===== MEDECIN =====
            try (PreparedStatement psMed = con.prepareStatement(sqlMed)) {

                psMed.setLong(1, userId);
                psMed.setString(2, medecin.getSpecialite());
                psMed.setTime(3, Time.valueOf(heureDebut));
                psMed.setTime(4, Time.valueOf(heureFin));

                psMed.executeUpdate(); // ✔️ مهم
            }

            con.commit();
            return null;

        } catch (Exception e) {
            rollbackQuietly(con);
            e.printStackTrace();
            return "unexpected_error";
        } finally {
            closeQuietly(con);
        }
    }

    /**
     * Supprime un médecin en supprimant son compte utilisateur.
     *
     * <p><strong>SQL :</strong> {@code DELETE FROM user WHERE id = ?}.
     * Pas de transaction explicite ; cascades selon le schéma FK.</p>
     *
     * @param id identifiant du médecin (id user)
     */
    public void deleteById(Long id) {

        String sql = "DELETE FROM `user` WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Mappe une ligne jointe {@code user}/{@code medecin}/{@code cabinet} vers un {@link Medecin}.
     *
     * @param rs curseur positionné sur la ligne courante
     * @return entité médecin remplie
     * @throws Exception si la lecture d'une colonne échoue
     */
    private Medecin mapMedecin(ResultSet rs) throws Exception {

        Medecin m = new Medecin();

        m.setId(rs.getLong("id"));
        m.setNom(rs.getString("nom"));
        m.setPrenom(rs.getString("prenom"));
        m.setEmail(rs.getString("email"));
        m.setPassword(rs.getString("password"));
        m.setTelephone(rs.getString("telephone"));
        m.setRole(User.Role.valueOf(rs.getString("role")));
        m.setActive(rs.getBoolean("active"));

        m.setSpecialite(rs.getString("specialite"));

        Time hd = rs.getTime("heure_debut");
        Time hf = rs.getTime("heure_fin");

        if (hd != null) m.setHeureDebut(hd.toLocalTime());
        if (hf != null) m.setHeureFin(hf.toLocalTime());

        long cid = rs.getLong("cabinet_id");
        m.setCabinetId(rs.wasNull() ? null : cid);
        m.setCabinetNom(rs.getString("cabinet_nom"));

        return m;
    }

    /**
     * Annule la transaction en cours sans propager d'exception.
     *
     * @param con connexion JDBC, éventuellement {@code null}
     */
    private void rollbackQuietly(Connection con) {
        if (con != null) {
            try { con.rollback(); } catch (Exception ignored) {}
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
            } catch (Exception ignored) {}
        }
    }
}

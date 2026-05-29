package com.cabinet.dao;

import com.cabinet.model.User;
import com.cabinet.util.DBConnection;
import com.cabinet.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Accès JDBC aux comptes utilisateurs de la table {@code user}.
 *
 * <p>Opérations en lecture seule sur la base ; pas de transaction multi-tables dans cette classe.
 * L'authentification combine une lecture SQL et une vérification applicative du mot de passe.</p>
 */
public class UserDAO {

    /**
     * Recherche un utilisateur par adresse e-mail.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur la table {@code user}
     * (colonnes id, nom, prenom, email, password, telephone, role, active),
     * filtre {@code WHERE email = ?}. Pas de transaction ; connexion auto-commit.</p>
     *
     * @param email adresse e-mail à rechercher
     * @return l'utilisateur trouvé, ou {@code null} si absent ou en cas d'erreur JDBC
     */
    public User findByEmail(String email) {
        String sql = """
                SELECT id, nom, prenom, email, password, telephone, role, active
                FROM `user`
                WHERE email = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Authentifie un utilisateur à partir de son e-mail et de son mot de passe en clair.
     *
     * <p><strong>SQL :</strong> aucune requête directe ; délègue à {@link #findByEmail(String)}
     * ({@code SELECT} sur {@code user}). La vérification du mot de passe est effectuée en mémoire
     * via {@link PasswordUtil#verifyPassword(String, String)}.</p>
     *
     * @param email         adresse e-mail du compte
     * @param plainPassword mot de passe en clair saisi à la connexion
     * @return l'utilisateur actif dont le mot de passe correspond, ou {@code null} sinon
     */
    public User authenticate(String email, String plainPassword) {
        User user = findByEmail(email);
        if (user == null || !user.isActive()) {
            return null;
        }
        return PasswordUtil.verifyPassword(plainPassword, user.getPassword()) ? user : null;
    }

    /**
     * Compte le nombre d'utilisateurs ayant un rôle donné.
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} sur la table {@code user}
     * avec {@code WHERE role = ?}. Pas de transaction.</p>
     *
     * @param role rôle métier à compter
     * @return le nombre d'utilisateurs pour ce rôle, ou {@code 0} en cas d'erreur
     */
    public long countByRole(User.Role role) {
        String sql = "SELECT COUNT(*) AS total FROM `user` WHERE role = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Mappe une ligne {@link ResultSet} vers un objet {@link User}.
     *
     * <p>Aucune opération SQL ; lecture des colonnes déjà extraites par une requête
     * {@code SELECT} sur {@code user}.</p>
     *
     * @param rs curseur positionné sur la ligne courante
     * @return entité utilisateur remplie
     * @throws Exception si la lecture d'une colonne ou la conversion du rôle échoue
     */
    private User mapUser(ResultSet rs) throws Exception {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setTelephone(rs.getString("telephone"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        user.setActive(rs.getBoolean("active"));
        return user;
    }
}

package com.cabinet.dao;

import com.cabinet.model.Secretaire;
import com.cabinet.model.User;
import com.cabinet.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

/**
 * Accès JDBC aux secrétaires (tables {@code user} et {@code secretaire}).
 *
 * <p>L'enregistrement d'un secrétaire s'effectue dans une transaction manuelle :
 * {@code INSERT} dans {@code user}, puis {@code INSERT} dans {@code secretaire} lié par clé étrangère.</p>
 */
public class SecretaireDAO {

    /**
     * Enregistre un nouveau secrétaire (compte utilisateur + fiche secrétaire).
     *
     * <p><strong>SQL (transaction) :</strong></p>
     * <ul>
     *   <li>{@code INSERT INTO user} (nom, prenom, email, password, telephone, role = SECRETAIRE)</li>
     *   <li>{@code INSERT INTO secretaire} (id, medecin_id, cabinet_id, bureau) avec l'id généré</li>
     * </ul>
     * <p>{@code setAutoCommit(false)} ; {@code commit} en succès, {@code rollback} en erreur.</p>
     *
     * @param secretaire entité à persister (champs user + medecinId, cabinetId, bureau)
     * @return {@code null} si succès ; sinon code d'erreur métier
     *         ({@code db_connection_failed}, {@code generated_key_missing},
     *         {@code email_deja_utilise}, {@code contrainte_integrite}, {@code unexpected_error})
     */
    public String saveSecretaire(Secretaire secretaire) {
        String sqlUser = "INSERT INTO `user` (nom, prenom, email, password, telephone, role) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlSec = "INSERT INTO secretaire (id, medecin_id, cabinet_id, bureau) VALUES (?, ?, ?, ?)";

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            if (con == null) return "db_connection_failed";
            con.setAutoCommit(false);

            try (PreparedStatement psUser = con.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, secretaire.getNom());
                psUser.setString(2, secretaire.getPrenom());
                psUser.setString(3, secretaire.getEmail());
                psUser.setString(4, secretaire.getPassword());
                psUser.setString(5, secretaire.getTelephone());
                psUser.setString(6, User.Role.SECRETAIRE.name());
                psUser.executeUpdate();

                try (ResultSet keys = psUser.getGeneratedKeys()) {
                    if (!keys.next()) {
                        con.rollback();
                        return "generated_key_missing";
                    }
                    long userId = keys.getLong(1);
                    try (PreparedStatement psSec = con.prepareStatement(sqlSec)) {
                        psSec.setLong(1, userId);
                        psSec.setLong(2, secretaire.getMedecinId());
                        psSec.setLong(3, secretaire.getCabinetId());
                        psSec.setString(4, secretaire.getBureau());
                        psSec.executeUpdate();
                    }
                }
            }
            con.commit();
            return null;
        } catch (SQLIntegrityConstraintViolationException e) {
            rollbackQuietly(con);
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("uk_user_email")) return "email_deja_utilise";
            return "contrainte_integrite";
        } catch (Exception e) {
            rollbackQuietly(con);
            e.printStackTrace();
            return "unexpected_error";
        } finally {
            closeQuietly(con);
        }
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

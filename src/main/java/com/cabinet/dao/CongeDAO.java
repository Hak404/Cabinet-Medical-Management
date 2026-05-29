package com.cabinet.dao;

import com.cabinet.model.Conge;
import com.cabinet.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès JDBC aux congés médecins (table {@code conge}).
 *
 * <p>Chaque enregistrement associe un médecin ({@code medecin_id}) à une date
 * ({@code date_conge}). Les opérations unitaires utilisent l'auto-commit par défaut.</p>
 */
public class CongeDAO {

    /**
     * Insère un jour de congé pour un médecin.
     *
     * <p><strong>SQL :</strong> {@code INSERT INTO conge} (date_conge, medecin_id).
     * Pas de transaction explicite ; contrainte d'unicité possible sur (medecin_id, date_conge).</p>
     *
     * @param conge entité contenant la date et l'identifiant du médecin
     * @throws RuntimeException si le congé existe déjà pour cette date
     *         ({@code SQLIntegrityConstraintViolationException}) ou en cas d'erreur JDBC
     */
    public void save(Conge conge) {
        String sql = "INSERT INTO conge (date_conge, medecin_id) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(conge.getDateConge()));
            ps.setLong(2, conge.getMedecinId());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new RuntimeException("Conge deja enregistre pour cette date", e);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'insertion du conge", e);
        }
    }

    /**
     * Liste tous les congés d'un médecin, triés par date croissante.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code conge}
     * ({@code WHERE medecin_id = ?}, {@code ORDER BY date_conge}). Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @return liste des congés ; jamais {@code null}
     * @throws RuntimeException en cas d'erreur JDBC
     */
    public List<Conge> findByMedecin(long medecinId) {
        List<Conge> conges = new ArrayList<>();
        String sql = """
                SELECT id, date_conge, medecin_id
                FROM conge
                WHERE medecin_id = ?
                ORDER BY date_conge ASC
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Conge conge = new Conge();
                    conge.setId(rs.getLong("id"));
                    conge.setDateConge(rs.getDate("date_conge").toLocalDate());
                    conge.setMedecinId(rs.getLong("medecin_id"));
                    conges.add(conge);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture des conges", e);
        }
        return conges;
    }

    /**
     * Supprime un congé par son identifiant.
     *
     * <p><strong>SQL :</strong> {@code DELETE FROM conge WHERE id = ?}. Pas de transaction.</p>
     *
     * @param id identifiant du congé à supprimer
     * @throws RuntimeException en cas d'erreur JDBC
     */
    public void delete(long id) {
        String sql = "DELETE FROM conge WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression du conge", e);
        }
    }

    /**
     * Indique si le médecin est en congé à la date donnée.
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} sur {@code conge}
     * avec {@code WHERE medecin_id = ? AND date_conge = ?}. Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param date        date à vérifier
     * @return {@code true} si au moins un congé existe pour ce couple médecin/date,
     *         {@code false} sinon ou en cas d'erreur (erreur loguée, pas d'exception)
     */
    public boolean isMedecinInConge(long medecinId, java.time.LocalDate date) {
        String sql = "SELECT COUNT(*) FROM conge WHERE medecin_id = ? AND date_conge = ?";
        try (Connection con = com.cabinet.util.DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, medecinId);
            ps.setDate(2, java.sql.Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

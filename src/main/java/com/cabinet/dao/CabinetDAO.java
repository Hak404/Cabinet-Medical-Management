package com.cabinet.dao;

import com.cabinet.model.Cabinet;
import com.cabinet.model.Medecin;
import com.cabinet.model.User;
import com.cabinet.util.DBConnection;
import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation de CabinetDAO utilisant BaseDAO.
 */
public class CabinetDAO extends BaseDAO {

    private final RowMapper<Cabinet> cabinetMapper = rs -> {
        Cabinet c = new Cabinet();
        c.setId(rs.getLong("id"));
        c.setNom(rs.getString("nom"));
        c.setAdresse(rs.getString("adresse"));
        c.setDureeConsultationMinutes(rs.getInt("duree_consultation_minutes"));
        c.setMedecinId(rs.getLong("medecin_id"));
        return c;
    };

    public List<Cabinet> findAll() {
        String sql = "SELECT * FROM cabinet ORDER BY nom";
        return queryList(sql, cabinetMapper);
    }

    public Optional<Cabinet> findById(Long id) {
        String sql = "SELECT * FROM cabinet WHERE id = ?";
        return queryOne(sql, cabinetMapper, id);
    }

    public long countAll() {
        String sql = "SELECT COUNT(*) FROM cabinet";
        return queryOne(sql, rs -> rs.getLong(1)).orElse(0L);
    }

    public boolean deleteById(Long id) {
        String sql = "DELETE FROM cabinet WHERE id = ?";
        return update(sql, id) > 0;
    }

    public String createCabinetWithMedecin(Cabinet cabinet, Medecin medecin) {
        String sqlUser = "INSERT INTO user (nom, prenom, email, password, telephone, role) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlMed = "INSERT INTO medecin (id, specialite, heure_debut, heure_fin) VALUES (?, ?, ?, ?)";
        String sqlCab = "INSERT INTO cabinet (nom, adresse, duree_consultation_minutes, medecin_id) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Optional<Long> userId = insert(con, sqlUser, medecin.getNom(), medecin.getPrenom(), 
                        medecin.getEmail(), medecin.getPassword(), medecin.getTelephone(), User.Role.MEDECIN.name());
                
                if (userId.isEmpty()) {
                    con.rollback();
                    return "error_user_creation";
                }

                update(con, sqlMed, userId.get(), medecin.getSpecialite(), 
                        Time.valueOf(medecin.getHeureDebut()), Time.valueOf(medecin.getHeureFin()));
                
                update(con, sqlCab, cabinet.getNom(), cabinet.getAdresse(), 
                        cabinet.getDureeConsultationMinutes(), userId.get());
                
                con.commit();
                return null;
            } catch (Exception e) {
                con.rollback();
                if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                    return "email_deja_utilise";
                }
                return "error_db";
            }
        } catch (SQLException e) {
            return "error_db";
        }
    }
}

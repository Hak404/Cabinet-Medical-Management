package com.cabinet.dao;

import com.cabinet.model.Patient;
import com.cabinet.model.User;
import com.cabinet.util.DBConnection;
import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation de PatientDAO utilisant BaseDAO.
 */
public class PatientDAO extends BaseDAO {

    private final RowMapper<Patient> patientMapper = rs -> {
        Patient p = new Patient();
        p.setId(rs.getLong("id"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setEmail(rs.getString("email"));
        p.setTelephone(rs.getString("telephone"));
        p.setCin(rs.getString("cin"));
        p.setAdresse(rs.getString("adresse"));
        p.setDateNaissance(rs.getDate("date_naissance").toLocalDate());
        p.setRole(User.Role.PATIENT);
        return p;
    };

    public List<Patient> findAll() {
        String sql = """
            SELECT u.*, p.cin, p.adresse, p.date_naissance 
            FROM user u 
            JOIN patient p ON u.id = p.id 
            WHERE u.role = 'PATIENT' 
            ORDER BY u.nom, u.prenom
            """;
        return queryList(sql, patientMapper);
    }

    public Optional<Patient> findById(Long id) {
        String sql = """
            SELECT u.*, p.cin, p.adresse, p.date_naissance 
            FROM user u 
            JOIN patient p ON u.id = p.id 
            WHERE u.id = ?
            """;
        return queryOne(sql, patientMapper, id);
    }

    public long countAll() {
        String sql = "SELECT COUNT(*) FROM patient";
        return queryOne(sql, rs -> rs.getLong(1)).orElse(0L);
    }

    public String savePatient(Patient patient) {
        String sqlUser = "INSERT INTO user (nom, prenom, email, password, telephone, role) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlPatient = "INSERT INTO patient (id, cin, adresse, date_naissance) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Optional<Long> userId = insert(con, sqlUser, patient.getNom(), patient.getPrenom(), 
                        patient.getEmail(), patient.getPassword(), patient.getTelephone(), User.Role.PATIENT.name());
                
                if (userId.isEmpty()) {
                    con.rollback();
                    return "error_user_creation";
                }

                update(con, sqlPatient, userId.get(), patient.getCin(), patient.getAdresse(), Date.valueOf(patient.getDateNaissance()));
                
                con.commit();
                return null;
            } catch (Exception e) {
                con.rollback();
                if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                    return "email_deja_utilise";
                }
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error_db";
        }
    }

    public void deletePatient(Long id) {
        // Suppression en cascade gérée par la base ou manuellement
        update("DELETE FROM user WHERE id = ?", id);
    }

    public void deleteById(Long id) {
        deletePatient(id);
    }

    public List<Patient> findByMedecinAndDateWithActivity(Long medecinId, java.time.LocalDate date) {
        String sql = """
            SELECT DISTINCT u.*, p.cin, p.adresse, p.date_naissance 
            FROM user u 
            JOIN patient p ON u.id = p.id 
            JOIN rendez_vous rv ON u.id = rv.patient_id
            WHERE rv.medecin_id = ? AND rv.date_rendez_vous = ?
            """;
        return queryList(sql, patientMapper, medecinId, java.sql.Date.valueOf(date));
    }
}

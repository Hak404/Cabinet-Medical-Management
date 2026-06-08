package com.cabinet.dao;

import com.cabinet.model.Medecin;
import com.cabinet.model.User;
import com.cabinet.util.PasswordUtil;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation de MedecinDAO utilisant BaseDAO.
 */
public class MedecinDAO extends BaseDAO {

    private final RowMapper<Medecin> medecinMapper = rs -> {
        Medecin m = new Medecin();
        m.setId(rs.getLong("id"));
        m.setNom(rs.getString("nom"));
        m.setPrenom(rs.getString("prenom"));
        m.setEmail(rs.getString("email"));
        m.setTelephone(rs.getString("telephone"));
        m.setSpecialite(rs.getString("specialite"));
        m.setCabinetId(rs.getLong("cabinet_id"));
        m.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        m.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        m.setRole(User.Role.MEDECIN);
        return m;
    };

    public List<Medecin> findAll() {
        String sql = """
            SELECT u.*, m.specialite, m.heure_debut, m.heure_fin, c.id AS cabinet_id 
            FROM user u 
            JOIN medecin m ON u.id = m.id 
            LEFT JOIN cabinet c ON m.id = c.medecin_id
            WHERE u.role = 'MEDECIN' 
            ORDER BY u.nom, u.prenom
            """;
        return queryList(sql, medecinMapper);
    }

    public Optional<Medecin> findById(Long id) {
        String sql = """
            SELECT u.*, m.specialite, m.heure_debut, m.heure_fin, c.id AS cabinet_id 
            FROM user u 
            JOIN medecin m ON u.id = m.id 
            LEFT JOIN cabinet c ON m.id = c.medecin_id
            WHERE u.id = ?
            """;
        return queryOne(sql, medecinMapper, id);
    }

    public long countAll() {
        String sql = "SELECT COUNT(*) FROM medecin";
        return queryOne(sql, rs -> rs.getLong(1)).orElse(0L);
    }

    public Medecin authenticate(String email, String password) {
        String sql = """
            SELECT u.*, m.specialite, m.heure_debut, m.heure_fin, c.id AS cabinet_id 
            FROM user u 
            JOIN medecin m ON u.id = m.id 
            LEFT JOIN cabinet c ON m.id = c.medecin_id
            WHERE u.email = ? AND u.role = 'MEDECIN'
            """;
        Optional<Medecin> opt = queryOne(sql, medecinMapper, email);
        if (opt.isPresent() && PasswordUtil.checkPassword(password, opt.get().getPassword())) {
            return opt.get();
        }
        return null;
    }

    public void deleteMedecin(Long id) {
        update("DELETE FROM user WHERE id = ?", id);
    }

    public void deleteById(Long id) {
        deleteMedecin(id);
    }
}

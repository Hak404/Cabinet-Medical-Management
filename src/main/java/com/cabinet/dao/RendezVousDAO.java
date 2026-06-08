package com.cabinet.dao;

import com.cabinet.model.RendezVous;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation de RendezVousDAO utilisant BaseDAO.
 */
public class RendezVousDAO extends BaseDAO {

    private final RowMapper<RendezVous> rdvMapper = rs -> {
        RendezVous r = new RendezVous();
        r.setId(rs.getLong("id"));
        r.setPatientId(rs.getLong("patient_id"));
        r.setMedecinId(rs.getLong("medecin_id"));
        r.setCabinetId(rs.getLong("cabinet_id"));
        r.setDateRendezVous(rs.getDate("date_rendez_vous").toLocalDate());
        r.setStartTime(rs.getTime("start_time").toLocalTime());
        r.setEndTime(rs.getTime("end_time").toLocalTime());
        r.setStatut(RendezVous.Statut.valueOf(rs.getString("statut")));
        
        // Mapping optionnel des noms pour les vues jointes
        try { r.setPatientNomComplet(rs.getString("patient_nom")); } catch (SQLException ignored) {}
        try { r.setMedecinNomComplet(rs.getString("medecin_nom")); } catch (SQLException ignored) {}
        try { r.setCabinetNom(rs.getString("cabinet_nom")); } catch (SQLException ignored) {}
        
        return r;
    };

    public long countToday() {
        String sql = "SELECT COUNT(*) FROM rendez_vous WHERE date_rendez_vous = CURDATE()";
        return queryOne(sql, rs -> rs.getLong(1)).orElse(0L);
    }

    public List<RendezVous> findByPatientId(Long patientId) {
        String sql = """
            SELECT r.*, CONCAT(u.nom, ' ', u.prenom) as medecin_nom, c.nom as cabinet_nom
            FROM rendez_vous r
            JOIN user u ON r.medecin_id = u.id
            JOIN cabinet c ON r.cabinet_id = c.id
            WHERE r.patient_id = ?
            ORDER BY r.date_rendez_vous DESC, r.start_time DESC
            """;
        return queryList(sql, rdvMapper, patientId);
    }

    public Optional<RendezVous> findById(Long id) {
        String sql = """
            SELECT r.*, 
                   CONCAT(up.nom, ' ', up.prenom) as patient_nom,
                   CONCAT(um.nom, ' ', um.prenom) as medecin_nom,
                   c.nom as cabinet_nom
            FROM rendez_vous r
            JOIN user up ON r.patient_id = up.id
            JOIN user um ON r.medecin_id = um.id
            JOIN cabinet c ON r.cabinet_id = c.id
            WHERE r.id = ?
            """;
        return queryOne(sql, rdvMapper, id);
    }

    public boolean updateStatut(Long id, RendezVous.Statut statut) {
        String sql = "UPDATE rendez_vous SET statut = ? WHERE id = ?";
        return update(sql, statut.name(), id) > 0;
    }

    public boolean hasRendezVousOnSameDay(Long patientId, Long cabinetId, LocalDate date) {
        String sql = "SELECT COUNT(*) FROM rendez_vous WHERE patient_id = ? AND cabinet_id = ? AND date_rendez_vous = ? AND statut != 'ANNULE'";
        return queryOne(sql, rs -> rs.getLong(1), patientId, cabinetId, java.sql.Date.valueOf(date)).orElse(0L) > 0;
    }

    public java.util.List<RendezVous> findByMedecinAndDate(Long medecinId, LocalDate date) {
        String sql = """
            SELECT r.*, CONCAT(u.nom, ' ', u.prenom) as patient_nom, c.nom as cabinet_nom
            FROM rendez_vous r
            JOIN user u ON r.patient_id = u.id
            JOIN cabinet c ON r.cabinet_id = c.id
            WHERE r.medecin_id = ? AND r.date_rendez_vous = ?
            ORDER BY r.start_time
            """;
        return queryList(sql, rdvMapper, medecinId, java.sql.Date.valueOf(date));
    }

    public RendezVous findNextForMedecin(Long medecinId) {
        String sql = """
            SELECT r.*, CONCAT(u.nom, ' ', u.prenom) as patient_nom, c.nom as cabinet_nom
            FROM rendez_vous r
            JOIN user u ON r.patient_id = u.id
            JOIN cabinet c ON r.cabinet_id = c.id
            WHERE r.medecin_id = ? AND r.date_rendez_vous = CURDATE() 
              AND r.start_time > CURTIME() AND r.statut = 'CONFIRME'
            ORDER BY r.start_time LIMIT 1
            """;
        return queryOne(sql, rdvMapper, medecinId).orElse(null);
    }

    public java.util.Map<LocalDate, Long> countByMedecinGroupedByDate(Long medecinId, LocalDate start, LocalDate end) {
        String sql = """
            SELECT date_rendez_vous, COUNT(*) as total
            FROM rendez_vous
            WHERE medecin_id = ? AND date_rendez_vous BETWEEN ? AND ?
            GROUP BY date_rendez_vous
            """;
        java.util.Map<LocalDate, Long> counts = new java.util.HashMap<>();
        queryList(sql, rs -> {
            counts.put(rs.getDate("date_rendez_vous").toLocalDate(), rs.getLong("total"));
            return null;
        }, medecinId, java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));
        return counts;
    }

    public List<RendezVous> findByPatient(Long patientId) {
        return findByPatientId(patientId);
    }

    public boolean hasConflict(Long medecinId, LocalDate date, java.time.LocalTime start, java.time.LocalTime end) {
        String sql = """
            SELECT COUNT(*) FROM rendez_vous 
            WHERE medecin_id = ? AND date_rendez_vous = ? 
              AND statut != 'ANNULE'
              AND ((start_time < ? AND end_time > ?) OR (start_time < ? AND end_time > ?))
            """;
        return queryOne(sql, rs -> rs.getLong(1), medecinId, java.sql.Date.valueOf(date), 
                        java.sql.Time.valueOf(end), java.sql.Time.valueOf(start),
                        java.sql.Time.valueOf(start), java.sql.Time.valueOf(end)).orElse(0L) > 0;
    }

    public boolean save(RendezVous r) {
        if (r.getId() == null) {
            String sql = "INSERT INTO rendez_vous (patient_id, medecin_id, cabinet_id, date_rendez_vous, start_time, end_time, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
            Optional<Long> id = insert(sql, r.getPatientId(), r.getMedecinId(), r.getCabinetId(), 
                                      java.sql.Date.valueOf(r.getDateRendezVous()), 
                                      java.sql.Time.valueOf(r.getStartTime()), 
                                      java.sql.Time.valueOf(r.getEndTime()), 
                                      r.getStatut().name());
            if (id.isPresent()) {
                r.setId(id.get());
                return true;
            }
            return false;
        } else {
            String sql = "UPDATE rendez_vous SET statut = ? WHERE id = ?";
            return update(sql, r.getStatut().name(), r.getId()) > 0;
        }
    }
}

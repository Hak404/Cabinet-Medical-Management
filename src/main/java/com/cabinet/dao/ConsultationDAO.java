package com.cabinet.dao;

import com.cabinet.model.Consultation;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Implémentation de ConsultationDAO utilisant BaseDAO.
 */
public class ConsultationDAO extends BaseDAO {

    private final RowMapper<Consultation> consultMapper = rs -> {
        Consultation c = new Consultation();
        c.setId(rs.getLong("id"));
        c.setRendezVousId(rs.getLong("rendez_vous_id"));
        c.setDiagnostic(rs.getString("diagnostic"));
        c.setRemarque(rs.getString("remarque"));
        java.sql.Timestamp ts = rs.getTimestamp("date_consultation");
        if (ts != null) {
            c.setDateConsultation(ts.toLocalDateTime().toLocalDate());
        }
        return c;
    };

    public Optional<Consultation> findByRendezVousId(Long rdvId) {
        String sql = "SELECT * FROM consultation WHERE rendez_vous_id = ?";
        return queryOne(sql, consultMapper, rdvId);
    }

    public Optional<Long> save(Connection con, Consultation c) throws SQLException {
        if (c.getId() == null) {
            String sql = "INSERT INTO consultation (rendez_vous_id, diagnostic, remarque) VALUES (?, ?, ?)";
            return insert(con, sql, c.getRendezVousId(), c.getDiagnostic(), c.getRemarque());
        } else {
            String sql = "UPDATE consultation SET diagnostic = ?, remarque = ? WHERE id = ?";
            update(con, sql, c.getDiagnostic(), c.getRemarque(), c.getId());
            return Optional.of(c.getId());
        }
    }

    public java.util.Map<java.time.LocalDate, Long> countByMedecinGroupedByDate(Long medecinId, java.time.LocalDate start, java.time.LocalDate end) {
        String sql = """
                SELECT DATE(rv.date_rendez_vous) as date_consult, COUNT(c.id) as total
                FROM consultation c
                INNER JOIN rendez_vous rv ON c.rendez_vous_id = rv.id
                WHERE rv.medecin_id = ? AND rv.date_rendez_vous BETWEEN ? AND ?
                GROUP BY DATE(rv.date_rendez_vous)
                """;
        java.util.Map<java.time.LocalDate, Long> counts = new java.util.HashMap<>();
        queryList(sql, rs -> {
            counts.put(rs.getDate("date_consult").toLocalDate(), rs.getLong("total"));
            return null;
        }, medecinId, java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));
        return counts;
    }

    public java.util.List<Consultation> findByMedecinAndDate(Long medecinId, java.time.LocalDate date) {
        String sql = """
                SELECT c.*, rv.date_rendez_vous as date_consultation, 
                       CONCAT(u.nom, ' ', u.prenom) as patient_nom
                FROM consultation c
                INNER JOIN rendez_vous rv ON c.rendez_vous_id = rv.id
                INNER JOIN user u ON rv.patient_id = u.id
                WHERE rv.medecin_id = ? AND rv.date_rendez_vous = ?
                """;
        return queryList(sql, rs -> {
            Consultation c = consultMapper.map(rs);
            try { c.setPatientNomComplet(rs.getString("patient_nom")); } catch (Exception ignored) {}
            return c;
        }, medecinId, java.sql.Date.valueOf(date));
    }
}

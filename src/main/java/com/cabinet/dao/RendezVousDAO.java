package com.cabinet.dao;

import com.cabinet.model.RendezVous;
import com.cabinet.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Accès JDBC aux rendez-vous (table {@code rendez_vous}, jointures {@code cabinet}, {@code user}).
 *
 * <p>Fournit la lecture enrichie (noms cabinet/médecin/patient), l'insertion, la mise à jour du statut
 * et des requêtes de contrôle de conflits ou de doublons. La plupart des lectures sont en auto-commit ;
 * {@link #save} et {@link #updateStatut} lèvent une {@link RuntimeException} en cas d'échec.</p>
 */
public class RendezVousDAO {

    /**
     * Recherche un rendez-vous par identifiant avec libellés associés.
     *
     * <p><strong>SQL :</strong> {@code SELECT} sur {@code rendez_vous} rv avec
     * {@code INNER JOIN cabinet}, {@code user} (médecin et patient), {@code WHERE rv.id = ?}.
     * Pas de transaction.</p>
     *
     * @param id identifiant du rendez-vous
     * @return le rendez-vous trouvé, ou {@code null} si absent ou en cas d'erreur
     */
    public RendezVous findById(Long id) {
        String sql = """
                SELECT rv.id, rv.cabinet_id, rv.medecin_id, rv.patient_id, rv.date_rendez_vous,
                       rv.start_time, rv.end_time, rv.statut,
                       c.nom AS cabinet_nom,
                       CONCAT(mu.nom, ' ', mu.prenom) AS medecin_nom,
                       CONCAT(pu.nom, ' ', pu.prenom) AS patient_nom
                FROM rendez_vous rv
                INNER JOIN cabinet c ON c.id = rv.cabinet_id
                INNER JOIN user mu ON mu.id = rv.medecin_id
                INNER JOIN user pu ON pu.id = rv.patient_id
                WHERE rv.id = ?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRendezVous(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Liste les rendez-vous d'un médecin pour une date donnée.
     *
     * <p><strong>SQL :</strong> {@code SELECT} joint sur {@code rendez_vous}, {@code cabinet}, {@code user},
     * filtres {@code medecin_id} et {@code date_rendez_vous}, tri par {@code start_time}.
     * Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param date      date des rendez-vous
     * @return liste ordonnée par heure de début
     */
    public List<RendezVous> findByMedecinAndDate(Long medecinId, LocalDate date) {
        List<RendezVous> result = new ArrayList<>();
        String sql = """
                SELECT rv.id, rv.cabinet_id, rv.medecin_id, rv.patient_id, rv.date_rendez_vous,
                       rv.start_time, rv.end_time, rv.statut,
                       c.nom AS cabinet_nom,
                       CONCAT(mu.nom, ' ', mu.prenom) AS medecin_nom,
                       CONCAT(pu.nom, ' ', pu.prenom) AS patient_nom
                FROM rendez_vous rv
                INNER JOIN cabinet c ON c.id = rv.cabinet_id
                INNER JOIN user mu ON mu.id = rv.medecin_id
                INNER JOIN user pu ON pu.id = rv.patient_id
                WHERE rv.medecin_id = ? AND rv.date_rendez_vous = ?
                ORDER BY rv.start_time
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRendezVous(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Liste les rendez-vous d'un médecin sur une plage de dates inclusive.
     *
     * <p><strong>SQL :</strong> {@code SELECT} joint, {@code WHERE date_rendez_vous BETWEEN ? AND ?},
     * tri par date puis heure. Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param startDate date de début (incluse)
     * @param endDate   date de fin (incluse)
     * @return liste des rendez-vous sur la période
     */
    public List<RendezVous> findByMedecinBetweenDates(Long medecinId, LocalDate startDate, LocalDate endDate) {
        List<RendezVous> result = new ArrayList<>();
        String sql = """
                SELECT rv.id, rv.cabinet_id, rv.medecin_id, rv.patient_id, rv.date_rendez_vous,
                       rv.start_time, rv.end_time, rv.statut,
                       c.nom AS cabinet_nom,
                       CONCAT(mu.nom, ' ', mu.prenom) AS medecin_nom,
                       CONCAT(pu.nom, ' ', pu.prenom) AS patient_nom
                FROM rendez_vous rv
                INNER JOIN cabinet c ON c.id = rv.cabinet_id
                INNER JOIN user mu ON mu.id = rv.medecin_id
                INNER JOIN user pu ON pu.id = rv.patient_id
                WHERE rv.medecin_id = ?
                  AND rv.date_rendez_vous BETWEEN ? AND ?
                ORDER BY rv.date_rendez_vous, rv.start_time
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRendezVous(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Compte les rendez-vous prévus pour la date du jour (serveur applicatif).
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} sur {@code rendez_vous}
     * avec {@code WHERE date_rendez_vous = ?} (date courante). Pas de transaction.</p>
     *
     * @return nombre de rendez-vous aujourd'hui, ou {@code 0} en cas d'erreur
     */
    public long countToday() {
        String sql = "SELECT COUNT(*) AS total FROM rendez_vous WHERE date_rendez_vous = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Retourne le prochain rendez-vous futur d'un médecin (hors statut ANNULE).
     *
     * <p><strong>SQL :</strong> {@code SELECT} joint avec filtres date/heure courantes,
     * {@code statut <> ANNULE}, {@code ORDER BY date, start_time LIMIT 1}. Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @return le prochain rendez-vous, ou {@code null} si aucun
     */
    public RendezVous findNextForMedecin(Long medecinId) {
        String sql = """
                SELECT rv.id, rv.cabinet_id, rv.medecin_id, rv.patient_id, rv.date_rendez_vous,
                       rv.start_time, rv.end_time, rv.statut,
                       c.nom AS cabinet_nom,
                       CONCAT(mu.nom, ' ', mu.prenom) AS medecin_nom,
                       CONCAT(pu.nom, ' ', pu.prenom) AS patient_nom
                FROM rendez_vous rv
                INNER JOIN cabinet c ON c.id = rv.cabinet_id
                INNER JOIN user mu ON mu.id = rv.medecin_id
                INNER JOIN user pu ON pu.id = rv.patient_id
                WHERE rv.medecin_id = ?
                  AND (rv.date_rendez_vous > ? OR (rv.date_rendez_vous = ? AND rv.start_time >= ?))
                  AND rv.statut <> ?
                ORDER BY rv.date_rendez_vous, rv.start_time
                LIMIT 1
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(today));
            ps.setDate(3, Date.valueOf(today));
            ps.setTime(4, Time.valueOf(now));
            ps.setString(5, RendezVous.Statut.ANNULE.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRendezVous(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Insère un nouveau rendez-vous et renseigne son identifiant généré.
     *
     * <p><strong>SQL :</strong> {@code INSERT INTO rendez_vous}
     * (cabinet_id, medecin_id, patient_id, date_rendez_vous, start_time, end_time, statut)
     * avec clé générée. Pas de transaction explicite ; auto-commit.</p>
     *
     * @param rendezVous entité à enregistrer
     * @return la même entité avec {@code id} renseigné si l'insertion réussit
     * @throws RuntimeException en cas d'erreur JDBC
     */
    public RendezVous save(RendezVous rendezVous) {
        String sql = """
                INSERT INTO rendez_vous
                (cabinet_id, medecin_id, patient_id, date_rendez_vous, start_time, end_time, statut)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rendezVous.getCabinetId());
            ps.setLong(2, rendezVous.getMedecinId());
            ps.setLong(3, rendezVous.getPatientId());
            ps.setDate(4, Date.valueOf(rendezVous.getDateRendezVous()));
            ps.setTime(5, Time.valueOf(rendezVous.getStartTime()));
            ps.setTime(6, Time.valueOf(rendezVous.getEndTime()));
            ps.setString(7, rendezVous.getStatut().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    rendezVous.setId(keys.getLong(1));
                }
            }
            return rendezVous;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du rendez-vous", e);
        }
    }

    /**
     * Met à jour le statut d'un rendez-vous existant.
     *
     * <p><strong>SQL :</strong> {@code UPDATE rendez_vous SET statut = ? WHERE id = ?}.
     * Pas de transaction explicite.</p>
     *
     * @param rdvId  identifiant du rendez-vous
     * @param statut nouveau statut
     * @throws RuntimeException en cas d'erreur JDBC
     */
    public void updateStatut(Long rdvId, RendezVous.Statut statut) {
        String sql = "UPDATE rendez_vous SET statut = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setLong(2, rdvId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du statut du rendez-vous", e);
        }
    }

    /**
     * Agrège le nombre de rendez-vous par date pour un médecin sur une période (hors annulés).
     *
     * <p><strong>SQL :</strong> {@code SELECT date_rendez_vous, COUNT(*)} sur {@code rendez_vous}
     * avec {@code GROUP BY date_rendez_vous}, filtres médecin, plage de dates, {@code statut <> ANNULE}.
     * Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param startDate date de début (incluse)
     * @param endDate   date de fin (incluse)
     * @return map date → effectif, ordre chronologique
     */
    public Map<LocalDate, Long> countByMedecinGroupedByDate(Long medecinId, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        String sql = """
                SELECT date_rendez_vous, COUNT(*) AS total
                FROM rendez_vous
                WHERE medecin_id = ?
                  AND date_rendez_vous BETWEEN ? AND ?
                  AND statut <> ?
                GROUP BY date_rendez_vous
                ORDER BY date_rendez_vous
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            ps.setString(4, RendezVous.Statut.ANNULE.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getDate("date_rendez_vous").toLocalDate(), rs.getLong("total"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return counts;
    }

    /**
     * Liste tous les rendez-vous d'un patient, triés par date et heure.
     *
     * <p><strong>SQL :</strong> {@code SELECT} joint sur {@code rendez_vous}, {@code cabinet}, {@code user},
     * filtre {@code patient_id = ?}. Pas de transaction.</p>
     *
     * @param patientId identifiant du patient
     * @return historique des rendez-vous du patient
     */
    public List<RendezVous> findByPatient(Long patientId) {
        List<RendezVous> result = new ArrayList<>();
        String sql = """
                SELECT rv.id, rv.cabinet_id, rv.medecin_id, rv.patient_id, rv.date_rendez_vous,
                       rv.start_time, rv.end_time, rv.statut,
                       c.nom AS cabinet_nom,
                       CONCAT(mu.nom, ' ', mu.prenom) AS medecin_nom,
                       CONCAT(pu.nom, ' ', pu.prenom) AS patient_nom
                FROM rendez_vous rv
                INNER JOIN cabinet c ON c.id = rv.cabinet_id
                INNER JOIN user mu ON mu.id = rv.medecin_id
                INNER JOIN user pu ON pu.id = rv.patient_id
                WHERE rv.patient_id = ?
                ORDER BY rv.date_rendez_vous, rv.start_time
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, patientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRendezVous(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Détecte un chevauchement horaire avec un autre rendez-vous du même médecin (hors annulés).
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} sur {@code rendez_vous} avec conditions
     * de chevauchement ({@code start_time < fin} et {@code end_time > début}). Pas de transaction.</p>
     *
     * @param medecinId identifiant du médecin
     * @param date      date du créneau
     * @param start     heure de début proposée
     * @param end       heure de fin proposée
     * @return {@code true} si un conflit existe, {@code false} sinon ou en cas d'erreur
     */
    public boolean hasConflict(Long medecinId, LocalDate date, LocalTime start, LocalTime end) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM rendez_vous
                WHERE medecin_id = ?
                  AND date_rendez_vous = ?
                  AND statut <> ?
                  AND start_time < ?
                  AND end_time > ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, medecinId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, RendezVous.Statut.ANNULE.name());
            ps.setTime(4, Time.valueOf(end));
            ps.setTime(5, Time.valueOf(start));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Mappe une ligne jointe vers un objet {@link RendezVous}.
     *
     * @param rs curseur positionné sur la ligne courante
     * @return entité rendez-vous remplie
     * @throws Exception si la lecture d'une colonne échoue
     */
    private RendezVous mapRendezVous(ResultSet rs) throws Exception {
        RendezVous rendezVous = new RendezVous();
        rendezVous.setId(rs.getLong("id"));
        rendezVous.setCabinetId(rs.getLong("cabinet_id"));
        rendezVous.setCabinetNom(rs.getString("cabinet_nom"));
        rendezVous.setMedecinId(rs.getLong("medecin_id"));
        rendezVous.setMedecinNomComplet(rs.getString("medecin_nom"));
        rendezVous.setPatientId(rs.getLong("patient_id"));
        rendezVous.setPatientNomComplet(rs.getString("patient_nom"));
        rendezVous.setDateRendezVous(rs.getDate("date_rendez_vous").toLocalDate());
        rendezVous.setStartTime(rs.getTime("start_time").toLocalTime());
        rendezVous.setEndTime(rs.getTime("end_time").toLocalTime());
        rendezVous.setStatut(RendezVous.Statut.valueOf(rs.getString("statut")));
        return rendezVous;
    }

    /**
     * Indique si le patient a déjà un rendez-vous en attente ou confirmé.
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} sur {@code rendez_vous}
     * avec {@code patient_id = ?} et {@code statut IN ('EN_ATTENTE', 'CONFIRME')}.
     * Pas de transaction.</p>
     *
     * @param patientId identifiant du patient
     * @return {@code true} si au moins un tel rendez-vous existe
     */
    public boolean hasExistingRendezVous(Long patientId) {
        String sql = "SELECT COUNT(*) AS total FROM rendez_vous WHERE patient_id = ? AND statut IN ('EN_ATTENTE', 'CONFIRME')";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Vérifie si le patient a déjà un rendez-vous (non annulé) le même jour dans le même cabinet.
     *
     * <p><strong>SQL :</strong> {@code SELECT COUNT(*)} sur {@code rendez_vous}
     * avec {@code patient_id}, {@code cabinet_id}, {@code date_rendez_vous}
     * et {@code statut <> 'ANNULE'}. Pas de transaction.</p>
     *
     * @param patientId identifiant du patient
     * @param cabinetId identifiant du cabinet
     * @param date      date du rendez-vous proposé
     * @return {@code true} si un doublon existe, {@code false} sinon ou en cas d'erreur
     */
    public boolean hasRendezVousOnSameDay(Long patientId, Long cabinetId, LocalDate date) {
        // On cherche s'il existe déjà un RDV (non annulé) pour ce patient, ce cabinet et cette date
        String sql = "SELECT COUNT(*) FROM rendez_vous " +
                     "WHERE patient_id = ? AND cabinet_id = ? AND date_rendez_vous = ? " +
                     "AND statut <> 'ANNULE'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, patientId);
            ps.setLong(2, cabinetId);
            ps.setDate(3, java.sql.Date.valueOf(date));

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

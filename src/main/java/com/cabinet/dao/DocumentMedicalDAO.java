package com.cabinet.dao;

import com.cabinet.model.DocumentMedical;
import com.cabinet.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès JDBC à la table {@code documents} (fichiers PDF médicaux).
 */
public class DocumentMedicalDAO {

    /**
     * Insère les métadonnées d'un document PDF.
     *
     * @param doc entité à persister (id renseigné après succès)
     * @return document avec identifiant généré
     * @throws RuntimeException en cas d'erreur SQL
     */
    public DocumentMedical save(DocumentMedical doc) {
        String sql = """
                INSERT INTO documents (patient_id, medecin_id, consultation_id, type_document,
                    titre, file_name, file_path, date_creation)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime created = doc.getDateCreation() != null ? doc.getDateCreation() : LocalDateTime.now();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, doc.getPatientId());
            ps.setLong(2, doc.getMedecinId());
            ps.setLong(3, doc.getConsultationId());
            ps.setString(4, doc.getTypeDocument().name());
            ps.setString(5, doc.getTitre());
            ps.setString(6, doc.getFileName());
            ps.setString(7, doc.getFilePath());
            ps.setTimestamp(8, Timestamp.valueOf(created));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    doc.setId(keys.getLong(1));
                }
            }
            doc.setDateCreation(created);
            return doc;
        } catch (Exception e) {
            throw new RuntimeException("Erreur enregistrement document médical", e);
        }
    }

    /**
     * Liste tous les documents d'un patient, du plus récent au plus ancien.
     */
    public List<DocumentMedical> findByPatient(Long patientId) {
        String sql = """
                SELECT id, patient_id, medecin_id, consultation_id, type_document,
                       titre, file_name, file_path, date_creation
                FROM documents
                WHERE patient_id = ?
                ORDER BY date_creation DESC, id DESC
                """;
        return queryList(sql, ps -> ps.setLong(1, patientId));
    }

    /**
     * Recherche un document par identifiant.
     */
    public DocumentMedical findById(Long id) {
        String sql = """
                SELECT id, patient_id, medecin_id, consultation_id, type_document,
                       titre, file_name, file_path, date_creation
                FROM documents
                WHERE id = ?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Supprime un document (métadonnées en base ; le fichier doit être supprimé séparément).
     */
    public void delete(Long id) {
        String sql = "DELETE FROM documents WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erreur suppression document médical id=" + id, e);
        }
    }

    /**
     * Liste les documents liés à une consultation.
     */
    public List<DocumentMedical> findByConsultation(Long consultationId) {
        String sql = """
                SELECT id, patient_id, medecin_id, consultation_id, type_document,
                       titre, file_name, file_path, date_creation
                FROM documents
                WHERE consultation_id = ?
                ORDER BY type_document, date_creation DESC
                """;
        return queryList(sql, ps -> ps.setLong(1, consultationId));
    }

    private interface PreparedStatementBinder {
        void bind(PreparedStatement ps) throws Exception;
    }

    private List<DocumentMedical> queryList(String sql, PreparedStatementBinder binder) {
        List<DocumentMedical> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private DocumentMedical map(ResultSet rs) throws Exception {
        DocumentMedical d = new DocumentMedical();
        d.setId(rs.getLong("id"));
        d.setPatientId(rs.getLong("patient_id"));
        d.setMedecinId(rs.getLong("medecin_id"));
        d.setConsultationId(rs.getLong("consultation_id"));
        String type = rs.getString("type_document");
        d.setTypeDocument(DocumentMedical.TypeDocument.valueOf(type));
        d.setTitre(rs.getString("titre"));
        d.setFileName(rs.getString("file_name"));
        d.setFilePath(rs.getString("file_path"));
        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) {
            d.setDateCreation(ts.toLocalDateTime());
        }
        return d;
    }
}

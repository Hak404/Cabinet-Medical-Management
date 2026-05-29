-- Migration : ajouter la table documents sur une base existante
-- Exécuter : mysql -u root -p cabinet_medical < schema_migration_documents.sql

USE `cabinet_medical`;

CREATE TABLE IF NOT EXISTS documents (
  id BIGINT NOT NULL AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  medecin_id BIGINT NOT NULL,
  consultation_id BIGINT NOT NULL,
  type_document VARCHAR(30) NOT NULL,
  titre VARCHAR(255) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_documents_patient (patient_id),
  KEY ix_documents_consultation (consultation_id),
  KEY ix_documents_medecin (medecin_id),
  CONSTRAINT fk_documents_patient
    FOREIGN KEY (patient_id) REFERENCES patient(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_documents_medecin
    FOREIGN KEY (medecin_id) REFERENCES medecin(id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_documents_consultation
    FOREIGN KEY (consultation_id) REFERENCES consultation(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

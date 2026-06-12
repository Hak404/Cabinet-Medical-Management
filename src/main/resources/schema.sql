-- Script SQL complet (MySQL)
-- Crée toutes les tables nécessaires + PK/FK/UNIQUE + contraintes de base.

DROP DATABASE IF EXISTS `cabinet_medical`;
CREATE DATABASE IF NOT EXISTS `cabinet_medical`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `cabinet_medical`;

-- =========================
-- USER (héritage User -> acteurs)
-- =========================
CREATE TABLE `user` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  email VARCHAR(190) NOT NULL,
  password VARCHAR(255) NOT NULL,
  telephone VARCHAR(30) NOT NULL,
  role VARCHAR(20) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 0,
  verification_code VARCHAR(6),
  verification_expiry TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_email (email),
  KEY ix_user_role (role)
) ENGINE=InnoDB;

CREATE TABLE admin (
  id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_admin_user
    FOREIGN KEY (id) REFERENCES `user`(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- Compte admin par défaut : exécuter data/seed_admin.sql après création des tables
-- (email admin@cabinet.com / mot de passe Admin123)

-- =========================
-- MEDECIN (avant cabinet : FK cabinet -> medecin)
-- =========================
CREATE TABLE medecin (
  id BIGINT NOT NULL,
  specialite VARCHAR(120) NOT NULL,
  heure_debut TIME NOT NULL,
  heure_fin TIME NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT chk_medecin_hours CHECK (heure_debut < heure_fin),
  CONSTRAINT fk_medecin_user
    FOREIGN KEY (id) REFERENCES `user`(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================
-- CABINET (1 médecin = 1 cabinet)
-- =========================
CREATE TABLE cabinet (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nom VARCHAR(120) NOT NULL,
  adresse VARCHAR(255),
  duree_consultation_minutes INT NOT NULL DEFAULT 30,
  medecin_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cabinet_nom (nom),
  UNIQUE KEY uk_cabinet_medecin (medecin_id),
  CONSTRAINT fk_cabinet_medecin
    FOREIGN KEY (medecin_id) REFERENCES medecin(id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =========================
-- PATIENT
-- =========================
CREATE TABLE patient (
  id BIGINT NOT NULL,
  cin VARCHAR(20) NOT NULL,
  adresse VARCHAR(255) NOT NULL,
  date_naissance DATE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_patient_cin (cin),
  CONSTRAINT fk_patient_user
    FOREIGN KEY (id) REFERENCES `user`(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================
-- SECRETAIRE
-- =========================
CREATE TABLE secretaire (
  id BIGINT NOT NULL,
  medecin_id BIGINT NOT NULL,
  cabinet_id BIGINT NOT NULL,
  bureau VARCHAR(40),
  PRIMARY KEY (id),
  KEY ix_secretaire_cabinet (cabinet_id),
  CONSTRAINT fk_secretaire_user
    FOREIGN KEY (id) REFERENCES `user`(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_secretaire_medecin
    FOREIGN KEY (medecin_id) REFERENCES medecin(id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_secretaire_cabinet
    FOREIGN KEY (cabinet_id) REFERENCES cabinet(id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =========================
-- PHARMACIE (partenaire)
-- =========================
CREATE TABLE pharmacie (
  id BIGINT NOT NULL,
  cabinet_id BIGINT NULL,
  adresse VARCHAR(255),
  PRIMARY KEY (id),
  KEY ix_pharmacie_cabinet (cabinet_id),
  CONSTRAINT fk_pharmacie_user
    FOREIGN KEY (id) REFERENCES `user`(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_pharmacie_cabinet
    FOREIGN KEY (cabinet_id) REFERENCES cabinet(id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================
-- CONGE (dates d'absence médecin)
-- =========================
CREATE TABLE conge (
  id BIGINT NOT NULL AUTO_INCREMENT,
  medecin_id BIGINT NOT NULL,
  date_conge DATE NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_conge_medecin_date (medecin_id, date_conge),
  KEY ix_conge_medecin (medecin_id),
  CONSTRAINT fk_conge_medecin
    FOREIGN KEY (medecin_id) REFERENCES medecin(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================
-- RENDEZ_VOUS
-- =========================
CREATE TABLE rendez_vous (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cabinet_id BIGINT NOT NULL,
  medecin_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  date_rendez_vous DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  -- EN_ATTENTE | CONFIRME | EN_COURS | TERMINE | ANNULE (annulation patient/médecin, pas de DELETE)
  statut VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rdv_medecin_date_start (medecin_id, date_rendez_vous, start_time),
  KEY ix_rdv_patient (patient_id),
  KEY ix_rdv_cabinet_date (cabinet_id, date_rendez_vous),
  KEY ix_rdv_medecin_date (medecin_id, date_rendez_vous),
  CONSTRAINT chk_rdv_time CHECK (start_time < end_time),
  CONSTRAINT fk_rdv_cabinet
    FOREIGN KEY (cabinet_id) REFERENCES cabinet(id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_rdv_medecin
    FOREIGN KEY (medecin_id) REFERENCES medecin(id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_rdv_patient
    FOREIGN KEY (patient_id) REFERENCES patient(id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =========================
-- CONSULTATION (1 rendez-vous = 1 consultation)
-- =========================
CREATE TABLE consultation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  rendez_vous_id BIGINT NOT NULL,
  diagnostic VARCHAR(2000),
  remarque VARCHAR(2000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_consultation_rdv (rendez_vous_id),
  CONSTRAINT fk_consultation_rdv
    FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous(id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =========================
-- ORDONNANCE + medicament_ordonnance (lignes)
-- =========================
CREATE TABLE ordonnance (
  id BIGINT NOT NULL AUTO_INCREMENT,
  consultation_id BIGINT NOT NULL,
  statut VARCHAR(20) NOT NULL DEFAULT 'PRESCRITE',
  pharmacie_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ordonnance_consultation (consultation_id),
  KEY ix_ordonnance_pharmacie (pharmacie_id),
  CONSTRAINT fk_ordonnance_consultation
    FOREIGN KEY (consultation_id) REFERENCES consultation(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_ordonnance_pharmacie
    FOREIGN KEY (pharmacie_id) REFERENCES pharmacie(id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE medicament_ordonnance (
  id BIGINT NOT NULL AUTO_INCREMENT,
  ordonnance_id BIGINT NOT NULL,
  nom VARCHAR(200) NOT NULL,
  posologie VARCHAR(350) NOT NULL,
  duree VARCHAR(120) NOT NULL,
  ligne_ordre INT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY ix_medicament_ordonnance_ord (ordonnance_id),
  CONSTRAINT fk_medicament_ordonnance
    FOREIGN KEY (ordonnance_id) REFERENCES ordonnance(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================
-- ANALYSES DEMANDÉES (codes par consultation)
-- =========================
CREATE TABLE analyse_demandee (
  id BIGINT NOT NULL AUTO_INCREMENT,
  consultation_id BIGINT NOT NULL,
  code_analyse VARCHAR(80) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_analyse_dem_cons_code (consultation_id, code_analyse),
  KEY ix_analyse_dem_consultation (consultation_id),
  CONSTRAINT fk_analyse_dem_consultation
    FOREIGN KEY (consultation_id) REFERENCES consultation(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================
-- DOCUMENTS MÉDICAUX (PDF générés après consultation)
-- =========================
CREATE TABLE documents (
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

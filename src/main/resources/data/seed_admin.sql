-- Compte administrateur par défaut (Cabinet Medical)
-- Exécuter sur la base existante : mysql -u root -p cabinet_medical < src/main/resources/data/seed_admin.sql
--
-- Identifiants de connexion :
--   Email    : admin@cabinet.com
--   Password : Admin123
--
-- Mot de passe hashé avec BCrypt (même algorithme que PasswordUtil / UserDAO).

USE `cabinet_medical`;

-- 1) Utilisateur (rôle ADMIN dans la table user)
INSERT INTO `user` (nom, prenom, email, password, telephone, role, active)
SELECT
    'Admin',
    'Principal',
    'admin@cabinet.com',
    '$2a$10$Bjx7okaiK9ZksJiMnmm0eOrD8H.1gV5rzpXRQ94GR3JcZL7MAjF0W',
    '0600000000',
    'ADMIN',
    1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `user` WHERE email = 'admin@cabinet.com'
);

-- 2) Ligne admin (FK vers user.id — héritage du modèle Admin)
INSERT INTO admin (id)
SELECT u.id
FROM `user` u
WHERE u.email = 'admin@cabinet.com'
  AND NOT EXISTS (
      SELECT 1 FROM admin a WHERE a.id = u.id
  );

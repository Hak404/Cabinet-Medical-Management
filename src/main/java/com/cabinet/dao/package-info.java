/**
 * Couche d'accès aux données (DAO) — persistance JDBC vers MySQL.
 *
 * <p>Chaque classe DAO correspond à une ou plusieurs tables de la base. Les méthodes ouvrent
 * une connexion via {@link com.cabinet.util.DBConnection}, utilisent des
 * {@link java.sql.PreparedStatement} pour des requêtes paramétrées, mappent les lignes
 * {@link java.sql.ResultSet} vers des entités {@link com.cabinet.model}, et ferment les ressources
 * dans des blocs try-with-resources ou {@code finally}.</p>
 *
 * <p>Les opérations transactionnelles (par ex. insertion utilisateur + patient) utilisent
 * {@code setAutoCommit(false)} avec {@code commit} / {@code rollback} manuels.</p>
 *
 * @see com.cabinet.package-info
 */
package com.cabinet.dao;

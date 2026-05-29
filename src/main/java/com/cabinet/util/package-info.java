/**
 * Utilitaires partagés : connexion base de données, sécurité, email, session et règles métier.
 *
 * <p>Ce package regroupe des classes d'aide sans accès HTTP direct (sauf via les filtres
 * {@link com.cabinet.filter}). Elles sont invoquées par les servlets (contrôleurs), les services
 * et les DAO dans la chaîne MVC.</p>
 *
 * <h2>Place dans l'architecture MVC</h2>
 * <p>Couche transversale entre contrôleurs, services et persistance — ne constitue ni la vue (JSP)
 * ni le modèle métier ({@link com.cabinet.model}), mais fournit les briques techniques
 * (JDBC, BCrypt, SMTP, session HTTP).</p>
 *
 * <h2>Flux de données</h2>
 * <pre>
 * JSP → Servlet → Service → DAO → {@link com.cabinet.util.DBConnection} → MySQL
 * </pre>
 *
 * @see com.cabinet.util.DBConnection
 * @see com.cabinet.util.PasswordUtil
 * @see com.cabinet.util.SessionUtil
 * @since 1.0
 */
package com.cabinet.util;

package com.cabinet.util;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Fournisseur de connexions JDBC vers la base MySQL {@code cabinet_medical}.
 *
 * <p><b>Rôle :</b> point d'entrée unique pour l'accès à la base de données depuis les DAO.</p>
 * <p><b>Objectif :</b> charger le pilote MySQL et ouvrir une connexion configurée (UTF-8, fuseau UTC).</p>
 * <p><b>Place MVC :</b> couche d'infrastructure sous les DAO — chaîne typique :
 * JSP → Servlet → Service → DAO → {@link #getConnection()} → MySQL.</p>
 *
 * @see com.cabinet.dao
 * @since 1.0
 */
public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/cabinet_medical"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8"
            + "&connectionCollation=utf8mb4_unicode_ci";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    /**
     * Ouvre une nouvelle connexion JDBC vers MySQL.
     *
     * @return connexion active prête pour les requêtes SQL
     * @throws RuntimeException si le pilote est introuvable ou si la connexion échoue
     *         (jamais {@code null}, afin d'éviter des NPE masquées côté DAO)
     */
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (Exception e) {
            // IMPORTANT: ne jamais retourner null (sinon NPE masquée côté DAO)
            throw new RuntimeException("Erreur de connexion à la base de données (URL=" + URL + ", user=" + USERNAME + ")", e);
        }
    }
}

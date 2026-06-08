package com.cabinet.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Fournisseur de connexions JDBC utilisant le pool HikariCP.
 * Optimisé pour la production.
 */
public final class DBConnection {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        
        // Configuration de base
        config.setJdbcUrl("jdbc:mysql://localhost:3306/cabinet_medical?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername("root");
        config.setPassword("");
        
        // Optimisations HikariCP
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000); // 5 minutes
        config.setConnectionTimeout(20000); // 20 secondes
        
        // Optimisations MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
    }

    private DBConnection() {}

    /**
     * @return Une connexion issue du pool.
     * @throws SQLException Si le pool est saturé ou la base injoignable.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static <T> T executeInTransaction(TransactionCallable<T> callable) {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                T result = callable.execute(con);
                con.commit();
                return result;
            } catch (Exception e) {
                con.rollback();
                throw new RuntimeException("Erreur lors de la transaction", e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur de connexion lors de la transaction", e);
        }
    }

    @FunctionalInterface
    public interface TransactionCallable<T> {
        T execute(Connection con) throws Exception;
    }

    /**
     * Ferme proprement le pool au déploiement de l'application.
     */
    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}

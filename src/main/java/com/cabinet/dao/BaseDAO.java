package com.cabinet.dao;


import com.cabinet.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe de base pour les DAOs, centralisant la gestion JDBC.
 * Supporte les transactions via l'injection optionnelle d'une Connection.
 */
public abstract class BaseDAO {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection con = DBConnection.getConnection()) {
            return queryList(con, sql, mapper, params);
        } catch (SQLException e) {
            handleSQLException(e, sql);
            return new ArrayList<>();
        }
    }

    protected <T> List<T> queryList(Connection con, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
            }
        }
        return result;
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection con = DBConnection.getConnection()) {
            return queryOne(con, sql, mapper, params);
        } catch (SQLException e) {
            handleSQLException(e, sql);
            return Optional.empty();
        }
    }

    protected <T> Optional<T> queryOne(Connection con, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapper.map(rs));
                }
            }
        }
        return Optional.empty();
    }

    protected int update(String sql, Object... params) {
        try (Connection con = DBConnection.getConnection()) {
            return update(con, sql, params);
        } catch (SQLException e) {
            handleSQLException(e, sql);
            return 0;
        }
    }

    protected int update(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParameters(ps, params);
            return ps.executeUpdate();
        }
    }

    protected Optional<Long> insert(String sql, Object... params) {
        try (Connection con = DBConnection.getConnection()) {
            return insert(con, sql, params);
        } catch (SQLException e) {
            handleSQLException(e, sql);
            return Optional.empty();
        }
    }

    protected Optional<Long> insert(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return Optional.of(rs.getLong(1));
                }
            }
        }
        return Optional.empty();
    }

    private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BaseDAO.class);

    protected void handleSQLException(SQLException e, String sql) {
        LOGGER.error("Erreur SQL lors de l'exécution de : {}", sql, e);
        throw new RuntimeException("Erreur d'accès à la base de données", e);
    }
}

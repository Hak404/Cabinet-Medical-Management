package com.cabinet.dao;

import com.cabinet.model.User;
import java.util.Optional;

/**
 * Implémentation de UserDAO utilisant BaseDAO.
 */
public class UserDAO extends BaseDAO {

    private final RowMapper<User> userMapper = rs -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setTelephone(rs.getString("telephone"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        user.setActive(rs.getBoolean("active"));
        return user;
    };

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ? AND active = true";
        return queryOne(sql, userMapper, email);
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        return queryOne(sql, userMapper, id);
    }

    public boolean updatePassword(Long userId, String hashedPassword) {
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        return update(sql, hashedPassword, userId) > 0;
    }

    public com.cabinet.model.User authenticate(String email, String password) {
        String sql = "SELECT * FROM user WHERE email = ? AND active = true";
        Optional<com.cabinet.model.User> opt = queryOne(sql, userMapper, email);
        if (opt.isPresent() && com.cabinet.util.PasswordUtil.checkPassword(password, opt.get().getPassword())) {
            return opt.get();
        }
        return null;
    }
}

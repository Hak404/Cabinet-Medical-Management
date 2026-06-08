package com.cabinet.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaire de hashage et vérification des mots de passe via BCrypt.
 *
 * <p><b>Rôle :</b> sécuriser le stockage des identifiants avant persistance en base.</p>
 * <p><b>Objectif :</b> produire un hash irréversible et comparer un mot de passe en clair
 * au hash enregistré dans la colonne {@code user.password}.</p>
 * <p><b>Place MVC :</b> couche utilitaire invoquée par {@link com.cabinet.service.AuthService}
 * et les DAO lors de l'inscription ou de l'authentification — sans lien HTTP direct.</p>
 *
 * @see com.cabinet.service.AuthService
 * @since 1.0
 */
public final class PasswordUtil {

    /**
     * Calcule le hash BCrypt d'un mot de passe en clair.
     *
     * @param plainPassword mot de passe saisi par l'utilisateur
     * @return hash BCrypt prêt à être stocké en base, ou {@code null} si le mot de passe est absent ou vide
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            return null;
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Vérifie qu'un mot de passe en clair correspond au hash stocké.
     *
     * @param plainPassword mot de passe saisi à la connexion
     * @param passwordHash hash BCrypt lu depuis la base de données
     * @return {@code true} si la correspondance est valide, {@code false} sinon (y compris si un paramètre est {@code null})
     */
    public static boolean verifyPassword(String plainPassword, String passwordHash) {
        if (plainPassword == null || passwordHash == null) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, passwordHash);
    }

    /** Alias pour verifyPassword utilisé dans certains DAOs. */
    public static boolean checkPassword(String plainPassword, String passwordHash) {
        return verifyPassword(plainPassword, passwordHash);
    }

    /** Constructeur privé — classe utilitaire statique. */
    private PasswordUtil() {}
}

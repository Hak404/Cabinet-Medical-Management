package com.cabinet.model;

/**
 * Profil administrateur système, extension de {@link User} mappée sur la table {@code user}
 * avec le rôle {@link Role#ADMIN}.
 */
public class Admin extends User {

    /** Construit un administrateur avec le rôle {@link Role#ADMIN} par défaut. */
    public Admin() {
        setRole(Role.ADMIN);
    }
}

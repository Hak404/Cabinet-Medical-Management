package com.cabinet.util;

/**
 * Constantes des noms d'attributs de session HTTP.
 *
 * <p><b>Rôle :</b> centraliser les clés utilisées dans {@code HttpSession} pour éviter
 * les chaînes magiques dans les servlets et les JSP.</p>
 * <p><b>Objectif :</b> garantir une cohérence entre {@link SessionUtil}, les contrôleurs
 * et les vues qui lisent l'utilisateur connecté.</p>
 * <p><b>Place MVC :</b> couche utilitaire transverse — alimente la gestion de session
 * après authentification (filtre {@link com.cabinet.filter.AuthFilter}, servlets de login).</p>
 *
 * @see SessionUtil
 * @since 1.0
 */
public final class SessionConstants {

    /** Objet {@link com.cabinet.model.User} complet (mot de passe exclu après liaison). */
    public static final String ATTR_USER = "user";

    /** Identifiant numérique de l'utilisateur connecté. */
    public static final String ATTR_USER_ID = "userId";

    /** Adresse email de l'utilisateur connecté. */
    public static final String ATTR_USER_EMAIL = "userEmail";

    /** Nom du rôle ({@link com.cabinet.model.User.Role#name()}) sous forme de chaîne. */
    public static final String ATTR_ROLE = "role";

    /** Constructeur privé — classe de constantes. */
    private SessionConstants() {
    }
}

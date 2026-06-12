package com.cabinet.util;

import com.cabinet.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Gestion de la session HTTP utilisateur après authentification.
 *
 * <p><b>Rôle :</b> lier, lire et invalider l'utilisateur connecté dans {@code HttpSession}.</p>
 * <p><b>Objectif :</b> centraliser la création de session post-login, les redirections
 * vers le tableau de bord par rôle et la déconnexion.</p>
 * <p><b>Place MVC :</b> utilitaire transverse utilisé par les servlets de login/logout
 * et {@link com.cabinet.filter.AuthFilter} — la vue JSP lit les attributs via
 * {@link SessionConstants}.</p>
 *
 * @see SessionConstants
 * @see com.cabinet.filter.AuthFilter
 * @since 1.0
 */
public final class SessionUtil {

    /** Constructeur privé — classe utilitaire statique. */
    private SessionUtil() {
    }

    /**
     * Enregistre l'utilisateur authentifié dans la session (mot de passe effacé de l'objet).
     *
     * @param session session HTTP courante
     * @param user utilisateur authentifié à stocker ; ignoré si {@code session} ou {@code user} est {@code null}
     */
    public static void bindUser(HttpSession session, User user) {
        if (session == null || user == null) {
            return;
        }
        user.setPassword(null);
        session.setAttribute(SessionConstants.ATTR_USER, user);
        session.setAttribute(SessionConstants.ATTR_USER_ID, user.getId());
        session.setAttribute(SessionConstants.ATTR_USER_EMAIL, user.getEmail());
        session.setAttribute(SessionConstants.ATTR_ROLE, user.getRole().name());
        
        // Génération du jeton CSRF pour la session
        CsrfTokenUtil.getTokenFromSession(session);
    }

    /**
     * Récupère l'utilisateur stocké en session.
     *
     * @param session session HTTP (peut être {@code null})
     * @return l'utilisateur connecté, ou {@code null} si absent ou session invalide
     */
    public static User getAuthenticatedUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionConstants.ATTR_USER);
        if (value instanceof User) {
            return (User) value;
        }
        return null;
    }

    /**
     * Indique si une session contient un utilisateur authentifié.
     *
     * @param session session HTTP
     * @return {@code true} si un utilisateur valide est présent
     */
    public static boolean isAuthenticated(HttpSession session) {
        return getAuthenticatedUser(session) != null;
    }

    /**
     * Invalide la session HTTP (déconnexion).
     *
     * @param session session à invalider ; aucune action si {@code null}
     */
    public static void invalidate(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Redirige le navigateur vers la page de connexion.
     *
     * @param request requête HTTP (pour le context path)
     * @param response réponse HTTP
     * @param sessionExpired {@code true} pour ajouter le paramètre {@code timeout=1}
     * @throws IOException en cas d'échec d'envoi de la redirection
     */
    public static void redirectToLogin(HttpServletRequest request, HttpServletResponse response,
                                     boolean sessionExpired) throws IOException {
        String target = request.getContextPath() + "/login";
        if (sessionExpired) {
            target += "?timeout=1";
        }
        response.sendRedirect(target);
    }

    /**
     * Redirige vers le tableau de bord correspondant au rôle de l'utilisateur.
     *
     * @param user utilisateur connecté
     * @param request requête HTTP
     * @param response réponse HTTP
     * @throws IOException en cas d'échec de redirection
     */
    public static void redirectToRoleHome(User user, HttpServletRequest request,
                                          HttpServletResponse response) throws IOException {
        if (user == null || user.getRole() == null) {
            redirectToLogin(request, response, false);
            return;
        }
        response.sendRedirect(request.getContextPath() + dashboardPath(user.getRole()));
    }

    /**
     * Retourne le chemin URL du tableau de bord selon le rôle.
     *
     * @param role rôle de l'utilisateur
     * @return chemin relatif (ex. {@code /patient/dashboard}), ou {@code /login} si le rôle est inconnu
     */
    public static String dashboardPath(User.Role role) {
        if (role == null) {
            return "/login";
        }
        switch (role) {
            case ADMIN:
                return "/admin/dashboard";
            case MEDECIN:
                return "/medecin/dashboard";
            case PATIENT:
                return "/patient/dashboard";
            case SECRETAIRE:
                return "/secretaire/dashboard";
            case PHARMACIE:
                return "/pharmacie/dashboard";
            default:
                return "/login";
        }
    }
}

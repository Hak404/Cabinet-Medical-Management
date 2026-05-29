package com.cabinet.filter;

import com.cabinet.model.User;
import com.cabinet.util.SessionUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filtre d'authentification et d'autorisation par rôle sur les URL applicatives.
 *
 * <p><b>Rôle :</b> imposer une session valide et le bon rôle avant d'atteindre les servlets
 * et JSP protégées ({@code /admin/}, {@code /patient/}, etc.).</p>
 * <p><b>Objectif :</b> centraliser la protection des zones métier, bloquer l'accès direct
 * non autorisé et rediriger les utilisateurs déjà connectés depuis {@code /login}.</p>
 * <p><b>Place MVC :</b> barrière de sécurité entre le navigateur et les contrôleurs —
 * s'appuie sur {@link SessionUtil} ; ne remplace pas la logique métier des servlets.</p>
 *
 * @see SessionUtil
 * @see com.cabinet.controller
 * @since 1.0
 */
public class AuthFilter implements Filter {

    /**
     * Intercepte chaque requête : chemins publics, login, ou contrôle rôle/ session.
     *
     * @param req requête servlet
     * @param res réponse servlet
     * @param chain chaîne vers le filtre ou servlet suivant
     * @throws IOException en cas d'échec de redirection
     * @throws ServletException en cas d'erreur en aval
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }

        if (isPublicPath(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        User user = SessionUtil.getAuthenticatedUser(session);

        if (isLoginPath(path)) {
            if (user != null) {
                SessionUtil.redirectToRoleHome(user, request, response);
                return;
            }
            chain.doFilter(req, res);
            return;
        }

        User.Role requiredRole = requiredRoleForPath(path);
        if (requiredRole == null) {
            chain.doFilter(req, res);
            return;
        }

        if (user == null) {
            SessionUtil.redirectToLogin(request, response, session != null);
            return;
        }

        if (user.getRole() != requiredRole) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }

        chain.doFilter(req, res);
    }

    /**
     * Indique si l'URL est accessible sans authentification (accueil, inscription, assets).
     *
     * @param path chemin relatif à l'application (sans context path)
     * @return {@code true} si aucune session n'est requise
     */
    private boolean isPublicPath(String path) {
        if ("/".equals(path) || "/index.jsp".equals(path)) {
            return true;
        }
        if (path.startsWith("/logout")) {
            return true;
        }
        if (path.startsWith("/login")) {
            return false;
        }
        if (path.equals("/register.jsp") || path.startsWith("/register")) {
            return true;
        }
        if (path.startsWith("/patient/register")) {
            return true;
        }
        if (path.equals("/error.jsp")) {
            return true;
        }
        if (path.startsWith("/css/") || path.startsWith("/images/") || path.startsWith("/js/")) {
            return true;
        }
        if (path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".ico")
                || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".webp")) {
            return true;
        }
        return false;
    }

    /**
     * Indique si le chemin correspond à la page de connexion.
     *
     * @param path chemin relatif
     * @return {@code true} pour {@code /login} ou {@code /login.jsp}
     */
    private boolean isLoginPath(String path) {
        return path.equals("/login") || path.equals("/login.jsp");
    }

    /**
     * Déduit le rôle requis à partir du préfixe d'URL.
     *
     * @param path chemin relatif (ex. {@code /patient/dashboard})
     * @return rôle attendu, ou {@code null} si la zone n'est pas protégée par rôle
     */
    private User.Role requiredRoleForPath(String path) {
        if (path.startsWith("/admin/") || path.startsWith("/admin")) {
            return User.Role.ADMIN;
        }
        if (path.startsWith("/medecin/")) {
            return User.Role.MEDECIN;
        }
        if (path.startsWith("/patient/")) {
            return User.Role.PATIENT;
        }
        if (path.startsWith("/secretaire/")) {
            return User.Role.SECRETAIRE;
        }
        if (path.startsWith("/pharmacie/")) {
            return User.Role.PHARMACIE;
        }
        return null;
    }
}

package com.cabinet.controller;

import com.cabinet.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Contrôleur MVC de déconnexion.
 *
 * <p>Mappé sur {@code @WebServlet("/logout")}. Invalide la session HTTP via
 * {@link com.cabinet.util.SessionUtil} et redirige vers {@code /login} avec un message de succès.</p>
 *
 * @see LoginServlet
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Déconnecte l'utilisateur (GET).
     *
     * @param request  requête HTTP
     * @param response réponse HTTP (redirection vers la page de connexion)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        performLogout(request, response);
    }

    /**
     * Déconnecte l'utilisateur (POST).
     *
     * @param request  requête HTTP
     * @param response réponse HTTP (redirection vers la page de connexion)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        performLogout(request, response);
    }

    private void performLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        SessionUtil.invalidate(session);

        response.sendRedirect(request.getContextPath() + "/login?success=deconnexion");
    }
}

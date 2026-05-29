package com.cabinet.controller;

import com.cabinet.model.User;
import com.cabinet.service.AuthService;
import com.cabinet.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Contrôleur MVC de connexion utilisateur.
 *
 * <p>Mappé sur {@code @WebServlet("/login")}. Utilise {@link AuthService} pour valider
 * les identifiants, lie l'utilisateur à la session HTTP via {@link com.cabinet.util.SessionUtil},
 * puis redirige vers l'accueil du rôle ou affiche {@code /login.jsp}.</p>
 *
 * @see LogoutServlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    /**
     * Affiche le formulaire de connexion ou redirige si une session valide existe déjà.
     *
     * @param request  requête HTTP (paramètres éventuels de message flash)
     * @param response réponse HTTP (forward vers la JSP ou redirection)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User existing = SessionUtil.getAuthenticatedUser(session);
        if (existing != null) {
            SessionUtil.redirectToRoleHome(existing, request, response);
            return;
        }

        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    /**
     * Traite la soumission email/mot de passe : authentification puis création de session.
     *
     * @param request  requête HTTP ({@code email}, {@code password})
     * @param response réponse HTTP (redirection vers login ou accueil du rôle)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        User user = authService.login(email, password);

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=identifiants_incorrects");
            return;
        }

        HttpSession session = request.getSession(true);
        SessionUtil.bindUser(session, user);
        SessionUtil.redirectToRoleHome(user, request, response);
    }
}

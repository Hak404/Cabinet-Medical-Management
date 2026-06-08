package com.cabinet.controller;

import com.cabinet.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Contrôleur MVC du tableau de bord secrétaire.
 *
 * <p>Mappé sur {@code @WebServlet("/secretaire/dashboard")}. Vérifie le rôle SECRETAIRE en
 * session et transfère vers {@code /secretaire/dashboardSecretaire.jsp}.</p>
 */
@WebServlet("/secretaire/dashboard")
public class DashboardSecretaireServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Affiche le tableau de bord secrétaire.
     *
     * @param request  requête HTTP
     * @param response réponse HTTP (forward JSP ou redirections login/forbidden)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");
        if (user.getRole() != User.Role.SECRETAIRE) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }
        request.getRequestDispatcher("/secretaire/dashboardSecretaire.jsp").forward(request, response);
    }
}


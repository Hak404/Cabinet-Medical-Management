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
 * Contrôleur MVC du tableau de bord pharmacie.
 *
 * <p>Mappé sur {@code @WebServlet("/pharmacie/dashboard")}. Vérifie le rôle PHARMACIE en
 * session et transfère vers {@code /pharmacie/dashboardPharmacie.jsp}.</p>
 */
@WebServlet("/pharmacie/dashboard")
public class DashboardPharmacieServlet extends HttpServlet {

    /**
     * Affiche le tableau de bord pharmacie.
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
        if (user.getRole() != User.Role.PHARMACIE) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }
        request.getRequestDispatcher("/pharmacie/dashboardPharmacie.jsp").forward(request, response);
    }
}


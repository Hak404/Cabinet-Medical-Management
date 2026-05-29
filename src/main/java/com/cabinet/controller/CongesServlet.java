package com.cabinet.controller;

import com.cabinet.dao.CongeDAO;
import com.cabinet.model.Conge;
import com.cabinet.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur MVC de gestion des jours de congé du médecin.
 *
 * <p>Mappé sur {@code @WebServlet("/medecin/conges")}. En GET, liste les congés via
 * {@link CongeDAO} et transfère vers {@code /medecin/conges.jsp}. En POST, ajoute ou supprime
 * un congé puis redirige.</p>
 */
@WebServlet("/medecin/conges")
public class CongesServlet extends HttpServlet {

    private final CongeDAO congeDAO = new CongeDAO();

    /**
     * Affiche la liste des congés du médecin connecté.
     *
     * @param request  requête HTTP
     * @param response réponse HTTP (forward JSP ou redirections)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        User user = getAuthorizedMedecin(request, response);
        if (user == null) return;

        List<Conge> conges = congeDAO.findByMedecin(user.getId());
        request.setAttribute("conges", conges);
        request.getRequestDispatcher("/medecin/conges.jsp").forward(request, response);
    }

    /**
     * Ajoute un congé ou supprime un congé existant ({@code action=delete}).
     *
     * @param request  requête HTTP ({@code dateConge}, {@code id}, {@code action})
     * @param response réponse HTTP (redirection congés ou dashboard)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        User user = getAuthorizedMedecin(request, response);
        if (user == null) return;

        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            long congeId = parseLong(request.getParameter("id"));
            if (congeId > 0) congeDAO.delete(congeId);
            response.sendRedirect(request.getContextPath() + "/medecin/conges?deleted=1");
            return;
        }

        String dateCongeRaw = request.getParameter("dateConge");
        if (dateCongeRaw != null && !dateCongeRaw.isBlank()) {
            Conge conge = new Conge();
            conge.setDateConge(LocalDate.parse(dateCongeRaw));
            conge.setMedecinId(user.getId());
            congeDAO.save(conge);
        }

        response.sendRedirect(request.getContextPath() + "/medecin/dashboard?congeAdded=1");
    }

    private User getAuthorizedMedecin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        User user = (User) session.getAttribute("user");
        if (user.getRole() != User.Role.MEDECIN) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return null;
        }
        return user;
    }

    private long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception e) {
            return -1L;
        }
    }
}

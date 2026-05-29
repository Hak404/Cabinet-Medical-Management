package com.cabinet.controller;

import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.RendezVous;
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
 * Contrôleur MVC du tableau de bord patient.
 *
 * <p>Mappé sur {@code @WebServlet("/patient/dashboard")}. Charge les rendez-vous via
 * {@link RendezVousDAO} et transfère vers {@code /patient/dashboardPatient.jsp}.</p>
 */
@WebServlet("/patient/dashboard")
public class DashboardPatientServlet extends HttpServlet {

    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();

    /**
     * Affiche la liste des rendez-vous et indicateurs du patient connecté.
     *
     * @param request  requête HTTP (messages flash {@code error}/{@code success})
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
        if (user.getRole() != User.Role.PATIENT) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }

        Long patientId = user.getId();

        List<RendezVous> rendezVousList = rendezVousDAO.findByPatient(patientId);
        long rdvToday = rendezVousList.stream()
                .filter(r -> LocalDate.now().equals(r.getDateRendezVous()))
                .count();
        long rdvEnAttente = rendezVousList.stream()
                .filter(r -> r.getStatut() == RendezVous.Statut.EN_ATTENTE)
                .count();

        request.setAttribute("rendezVousList", rendezVousList);
        request.setAttribute("rdvToday", rdvToday);
        request.setAttribute("rdvEnAttente", rdvEnAttente);
        request.setAttribute("flashError", request.getParameter("error"));
        request.setAttribute("flashSuccess", request.getParameter("success"));

        request.getRequestDispatcher("/patient/dashboardPatient.jsp").forward(request, response);
    }
}


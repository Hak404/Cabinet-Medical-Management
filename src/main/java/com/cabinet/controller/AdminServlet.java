package com.cabinet.controller;

import com.cabinet.dao.CabinetDAO;
import com.cabinet.dao.MedecinDAO;
import com.cabinet.dao.PatientDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.User;
import com.cabinet.util.CsrfTokenUtil;
import com.cabinet.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Contrôleur du tableau de bord administrateur.
 */
@WebServlet("/admin/dashboard")
public class AdminServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final CabinetDAO cabinetDAO = new CabinetDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = SessionUtil.getAuthenticatedUser(request.getSession(false));
        if (user == null || user.getRole() != User.Role.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Générer le jeton CSRF pour les actions admin (delete, add cabinet, etc.)
        CsrfTokenUtil.getToken(request);

        request.setAttribute("totalPatients", patientDAO.countAll());
        request.setAttribute("totalDoctors", medecinDAO.countAll());
        request.setAttribute("appointmentsToday", rendezVousDAO.countToday());
        request.setAttribute("totalCabinets", cabinetDAO.countAll());

        request.setAttribute("medecins", medecinDAO.findAll());
        request.setAttribute("patients", patientDAO.findAll());
        request.setAttribute("cabinets", cabinetDAO.findAll());

        request.getRequestDispatcher("/admin/dashboardAdmin.jsp").forward(request, response);
    }
}

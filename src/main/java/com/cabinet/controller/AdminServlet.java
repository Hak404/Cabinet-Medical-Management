package com.cabinet.controller;

import com.cabinet.dao.CabinetDAO;
import com.cabinet.dao.MedecinDAO;
import com.cabinet.dao.PatientDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.Cabinet;
import com.cabinet.model.Medecin;
import com.cabinet.model.Patient;
import com.cabinet.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Contrôleur MVC du tableau de bord administrateur.
 *
 * <p>Mappé sur {@code @WebServlet("/admin/dashboard")}. Agrège statistiques et listes via
 * {@link PatientDAO}, {@link MedecinDAO}, {@link RendezVousDAO} et {@link CabinetDAO},
 * puis transfère vers {@code /admin/dashboardAdmin.jsp}.</p>
 */
@WebServlet("/admin/dashboard")
public class AdminServlet extends HttpServlet {

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final CabinetDAO cabinetDAO = new CabinetDAO();

    /**
     * Affiche le tableau de bord admin (compteurs et listes patients, médecins, cabinets).
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
        if (user.getRole() != User.Role.ADMIN) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }

        request.setAttribute("totalPatients", patientDAO.countAll());
        request.setAttribute("totalDoctors", medecinDAO.countAll());
        request.setAttribute("appointmentsToday", rendezVousDAO.countToday());

        List<Medecin> medecins = medecinDAO.findAll();
        List<Patient> patients = patientDAO.findAll();
        List<Cabinet> cabinets = cabinetDAO.findAll();
        request.setAttribute("medecins", medecins);
        request.setAttribute("patients", patients);
        request.setAttribute("cabinets", cabinets);

        request.getRequestDispatcher("/admin/dashboardAdmin.jsp").forward(request, response);
    }
}


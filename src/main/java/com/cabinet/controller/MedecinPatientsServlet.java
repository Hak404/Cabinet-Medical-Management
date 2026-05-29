package com.cabinet.controller;

import com.cabinet.dao.MedecinDAO;
import com.cabinet.dao.PatientDAO;
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
import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur MVC de la liste des patients suivis par le médecin.
 *
 * <p>Mappé sur {@code @WebServlet("/medecin/patients")}. Interroge {@link PatientDAO} et
 * {@link MedecinDAO}, puis transfère vers {@code /medecin/patients.jsp}.</p>
 */
@WebServlet("/medecin/patients")
public class MedecinPatientsServlet extends HttpServlet {
    private final PatientDAO patientDAO = new PatientDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();

    /**
     * Affiche les patients ayant une activité à la date sélectionnée.
     *
     * @param request  requête HTTP ({@code selectedDate})
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
        if (user.getRole() != User.Role.MEDECIN) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }
        LocalDate selectedDate = parseDate(request.getParameter("selectedDate"));
        List<Patient> patients = patientDAO.findByMedecinAndDateWithActivity(user.getId(), selectedDate);
        Medecin medecin = medecinDAO.findById(user.getId());
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("currentCabinetId", medecin != null ? medecin.getCabinetId() : null);
        request.setAttribute("patients", patients);
        request.getRequestDispatcher("/medecin/patients.jsp").forward(request, response);
    }

    private LocalDate parseDate(String raw) {
        try {
            return (raw == null || raw.isBlank()) ? LocalDate.now() : LocalDate.parse(raw);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}


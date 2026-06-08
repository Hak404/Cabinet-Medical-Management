package com.cabinet.controller;

import com.cabinet.dao.ConsultationDAO;
import com.cabinet.dao.MedecinDAO;
import com.cabinet.model.Consultation;
import com.cabinet.model.Medecin;
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
 * Contrôleur MVC de la liste des consultations du médecin.
 *
 * <p>Mappé sur {@code @WebServlet("/medecin/consultations")}. Interroge
 * {@link ConsultationDAO} et {@link MedecinDAO}, puis transfère vers
 * {@code /medecin/consultations.jsp}.</p>
 */
@WebServlet("/medecin/consultations")
public class ConsultationsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();

    /**
     * Affiche les consultations du médecin pour la date sélectionnée.
     *
     * @param request  requête HTTP ({@code selectedDate} ou {@code date})
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

        LocalDate selectedDate;
        try {
            String dateParam = request.getParameter("selectedDate");
            if (dateParam == null || dateParam.isBlank()) dateParam = request.getParameter("date");
            selectedDate = (dateParam == null || dateParam.isBlank()) ? LocalDate.now() : LocalDate.parse(dateParam);
        } catch (Exception e) {
            selectedDate = LocalDate.now();
        }

        List<Consultation> consultations = consultationDAO.findByMedecinAndDate(user.getId(), selectedDate);
        Medecin medecin = medecinDAO.findById(user.getId()).orElse(null);;
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("currentCabinetId", medecin != null ? medecin.getCabinetId() : null);
        request.setAttribute("consultations", consultations);
        request.getRequestDispatcher("/medecin/consultations.jsp").forward(request, response);
    }
}


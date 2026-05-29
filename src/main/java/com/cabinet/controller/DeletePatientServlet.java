package com.cabinet.controller;

import com.cabinet.dao.PatientDAO;
import com.cabinet.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Contrôleur MVC de suppression de patient (administrateur).
 *
 * <p>Mappé sur {@code @WebServlet("/admin/patients/delete")}. Supprime l'enregistrement via
 * {@link PatientDAO} et redirige vers le tableau de bord admin.</p>
 */
@WebServlet("/admin/patients/delete")
public class DeletePatientServlet extends HttpServlet {

    private final PatientDAO patientDAO = new PatientDAO();

    /**
     * Supprime un patient identifié par {@code id}.
     *
     * @param request  requête HTTP (paramètre {@code id})
     * @param response réponse HTTP (redirection dashboard)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        User u = (User) session.getAttribute("user");
        if (u.getRole() != User.Role.ADMIN) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }

        try {
            Long id = Long.parseLong(request.getParameter("id"));
            patientDAO.deleteById(id);
            response.sendRedirect(request.getContextPath() + "/admin/dashboard?success=patient_deleted");
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=patient_delete_failed");
        }
    }
}


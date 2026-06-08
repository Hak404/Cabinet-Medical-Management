package com.cabinet.controller;

import com.cabinet.dao.MedecinDAO;
import com.cabinet.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Contrôleur MVC de suppression de médecin (administrateur).
 *
 * <p>Mappé sur {@code @WebServlet("/admin/medecins/delete")}. Supprime l'enregistrement via
 * {@link MedecinDAO} et redirige vers le tableau de bord admin.</p>
 */
@WebServlet("/admin/medecins/delete")
public class DeleteMedecinServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final MedecinDAO medecinDAO = new MedecinDAO();

    /**
     * Supprime un médecin identifié par {@code id}.
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
            medecinDAO.deleteById(id);
            response.sendRedirect(request.getContextPath() + "/admin/dashboard?success=medecin_deleted");
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=medecin_delete_failed");
        }
    }
}


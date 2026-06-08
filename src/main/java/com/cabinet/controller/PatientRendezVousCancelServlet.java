package com.cabinet.controller;

import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.RendezVous;
import com.cabinet.model.User;
import com.cabinet.util.RendezVousCancellationPolicy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Contrôleur MVC d'annulation de rendez-vous par le patient.
 *
 * <p>Mappé sur {@code @WebServlet("/patient/rdv/cancel")}. Vérifie la propriété du RDV et la
 * politique {@link com.cabinet.util.RendezVousCancellationPolicy}, puis met à jour le statut
 * via {@link RendezVousDAO} (ANNULE, sans suppression). Redirige vers le dashboard patient.</p>
 */
@WebServlet("/patient/rdv/cancel")
public class PatientRendezVousCancelServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();

    /**
     * Annule un rendez-vous (soumission POST, paramètre {@code id} ou {@code rdvId}).
     *
     * @param request  requête HTTP
     * @param response réponse HTTP (redirection dashboard avec message)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleCancel(request, response);
    }

    /**
     * Annule un rendez-vous (accès GET pour compatibilité lien).
     *
     * @param request  requête HTTP
     * @param response réponse HTTP (redirection dashboard avec message)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleCancel(request, response);
    }

    private void handleCancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");

        String dashboard = request.getContextPath() + "/patient/dashboard";

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

        Long rdvId = parseLong(request.getParameter("id"));
        if (rdvId == null) {
            rdvId = parseLong(request.getParameter("rdvId"));
        }

        if (rdvId == null) {
            response.sendRedirect(dashboard + "?error=" + encode("Identifiant de rendez-vous invalide."));
            return;
        }

        RendezVous rdv = rendezVousDAO.findById(rdvId).orElse(null);;
        if (rdv == null) {
            response.sendRedirect(dashboard + "?error=" + encode("Rendez-vous introuvable."));
            return;
        }

        if (!user.getId().equals(rdv.getPatientId())) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }

        RendezVousCancellationPolicy.Evaluation evaluation = RendezVousCancellationPolicy.evaluate(rdv);
        if (!evaluation.isAllowed()) {
            response.sendRedirect(dashboard + "?error=" + encode(evaluation.getMessage()));
            return;
        }

        rendezVousDAO.updateStatut(rdvId, RendezVous.Statut.ANNULE);
        response.sendRedirect(dashboard + "?success=" + encode("Votre rendez-vous a été annulé avec succès."));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String encode(String message) {
        if (message == null) {
            return "";
        }
        return URLEncoder.encode(message, StandardCharsets.UTF_8);
    }
}

package com.cabinet.controller;

import com.cabinet.dao.MedecinDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.Medecin;
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
 * Contrôleur MVC de gestion des rendez-vous côté médecin.
 *
 * <p>Mappé sur {@code @WebServlet("/medecin/rendezvous")}. En GET, liste les RDV via
 * {@link RendezVousDAO} et transfère vers {@code /medecin/rendezvous.jsp}. En POST, met à jour
 * le statut (confirmer, annuler, remettre en attente).</p>
 */
@WebServlet("/medecin/rendezvous")
public class MedecinRendezVousServlet extends HttpServlet {
    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();

    /**
     * Affiche les rendez-vous du médecin pour la date sélectionnée.
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
        List<RendezVous> rdvs = rendezVousDAO.findByMedecinAndDate(user.getId(), selectedDate);
        Medecin medecin = medecinDAO.findById(user.getId());
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("currentCabinetId", medecin != null ? medecin.getCabinetId() : null);
        request.setAttribute("rdvs", rdvs);
        request.getRequestDispatcher("/medecin/rendezvous.jsp").forward(request, response);
    }

    /**
     * Modifie le statut d'un rendez-vous ({@code action}: cancel, confirm, attente).
     *
     * @param request  requête HTTP ({@code rdvId}, {@code action}, {@code selectedDate})
     * @param response réponse HTTP (redirection liste rendez-vous)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
        String action = request.getParameter("action");
        Long rdvId = parseLong(request.getParameter("rdvId"));
        String selectedDate = request.getParameter("selectedDate");
        if (rdvId != null && action != null) {
            RendezVous rdv = rendezVousDAO.findById(rdvId);
            if (rdv != null && user.getId().equals(rdv.getMedecinId())) {
                if ("cancel".equals(action)) {
                    rendezVousDAO.updateStatut(rdvId, RendezVous.Statut.ANNULE);
                } else if ("confirm".equals(action)) {
                    rendezVousDAO.updateStatut(rdvId, RendezVous.Statut.CONFIRME);
                } else if ("attente".equals(action)) {
                    rendezVousDAO.updateStatut(rdvId, RendezVous.Statut.EN_ATTENTE);
                }
            }
        }
        response.sendRedirect(request.getContextPath() + "/medecin/rendezvous?selectedDate=" + (selectedDate == null ? "" : selectedDate));
    }

    private LocalDate parseDate(String raw) {
        try {
            return (raw == null || raw.isBlank()) ? LocalDate.now() : LocalDate.parse(raw);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private Long parseLong(String v) {
        try {
            return (v == null || v.isBlank()) ? null : Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }
}


package com.cabinet.controller;

import com.cabinet.dao.ConsultationDAO;
import com.cabinet.dao.CongeDAO;
import com.cabinet.dao.MedecinDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.Conge;
import com.cabinet.model.Medecin;
import com.cabinet.model.RendezVous;
import com.cabinet.model.User;
import com.cabinet.util.CsrfTokenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Contrôleur MVC du tableau de bord médecin.
 *
 * <p>Mappé sur {@code @WebServlet("/medecin/dashboard")}. Agrège rendez-vous, consultations et
 * congés via {@link RendezVousDAO}, {@link ConsultationDAO}, {@link MedecinDAO} et
 * {@link CongeDAO}, puis transfère vers {@code /medecin/dashboardMedecin.jsp}.</p>
 */
@WebServlet("/medecin/dashboard")
public class MedecinServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final CongeDAO congeDAO = new CongeDAO();

    /**
     * Affiche le planning du jour ou de la semaine sélectionnée pour le médecin connecté.
     *
     * @param request  requête HTTP ({@code selectedDate} optionnel)
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

        // Générer le jeton CSRF pour le tableau de bord médecin
        CsrfTokenUtil.getToken(request);

        LocalDate selectedDate = parseDate(request.getParameter("selectedDate"));
        Long medecinId = user.getId();
        List<RendezVous> todayRdv = rendezVousDAO.findByMedecinAndDate(medecinId, selectedDate);
        long completed = todayRdv.stream().filter(r -> r.getStatut() == RendezVous.Statut.TERMINE).count();
        long remaining = Math.max(0, todayRdv.size() - completed);
        RendezVous next = selectedDate.equals(LocalDate.now()) ? rendezVousDAO.findNextForMedecin(medecinId) : null;
        Medecin medecin = medecinDAO.findById(medecinId).orElse(null);

        request.setAttribute("todayPatientsCount", todayRdv.size());
        request.setAttribute("completedConsultations", completed);
        request.setAttribute("remainingPatients", remaining);
        request.setAttribute("nextRdv", next);
        request.setAttribute("todayRdvList", todayRdv);
        request.setAttribute("weekDays", buildWeekDays(medecinId, selectedDate));
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("currentCabinetId", medecin != null ? medecin.getCabinetId() : null);

        request.getRequestDispatcher("/medecin/dashboardMedecin.jsp").forward(request, response);
    }

    private List<WeekDaySummary> buildWeekDays(Long medecinId, LocalDate selectedDate) {
        LocalDate anchor = selectedDate == null ? LocalDate.now() : selectedDate;
        LocalDate start = anchor.minusDays(anchor.getDayOfWeek().getValue() - 1L);
        LocalDate end = start.plusDays(6);
        Map<LocalDate, Long> rdvCounts = rendezVousDAO.countByMedecinGroupedByDate(medecinId, start, end);
        Map<LocalDate, Long> consultationCounts = consultationDAO.countByMedecinGroupedByDate(medecinId, start, end);

        List<Conge> congesMedecin = congeDAO.findByMedecin(medecinId);
        Set<LocalDate> conges = new HashSet<>();
        for (Conge conge : congesMedecin) {
            if (conge.getDateConge() != null) {
                conges.add(conge.getDateConge());
            }
        }

        List<WeekDaySummary> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            WeekDaySummary s = new WeekDaySummary();
            s.date = d;
            s.dayLabel = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            s.rendezVousCount = rdvCounts.getOrDefault(d, 0L);
            s.consultationCount = consultationCounts.getOrDefault(d, 0L);
            s.repos = conges.contains(d);
            s.selected = d.equals(anchor);
            days.add(s);
        }
        return days;
    }

    private LocalDate parseDate(String raw) {
        try {
            return (raw == null || raw.isBlank()) ? LocalDate.now() : LocalDate.parse(raw);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    public static class WeekDaySummary {
        private LocalDate date;
        private String dayLabel;
        private long rendezVousCount;
        private long consultationCount;
        private boolean repos;
        private boolean selected;

        public LocalDate getDate() { return date; }
        public String getDayLabel() { return dayLabel; }
        public long getRendezVousCount() { return rendezVousCount; }
        public long getConsultationCount() { return consultationCount; }
        public boolean isRepos() { return repos; }
        public boolean isSelected() { return selected; }
    }
}


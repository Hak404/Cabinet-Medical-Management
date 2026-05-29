package com.cabinet.controller;

import com.cabinet.dao.AnalyseDemandeeDAO;
import com.cabinet.dao.ConsultationDAO;
import com.cabinet.dao.DocumentMedicalDAO;
import com.cabinet.dao.OrdonnanceDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.Consultation;
import com.cabinet.model.MedicamentOrdonnance;
import com.cabinet.model.RendezVous;
import com.cabinet.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contrôleur MVC de saisie et enregistrement d'une consultation médicale.
 *
 * <p>Mappé sur {@code @WebServlet("/medecin/consultation")}. En GET, charge le rendez-vous,
 * la consultation, l'ordonnance et les analyses via {@link RendezVousDAO},
 * {@link ConsultationDAO}, {@link OrdonnanceDAO} et {@link AnalyseDemandeeDAO}, puis transfère
 * vers {@code /medecin/consultation.jsp}. En POST, persiste le dossier complet via
 * {@link ConsultationDAO#saveFullConsultation}.</p>
 */
@WebServlet("/medecin/consultation")
public class ConsultationServlet extends HttpServlet {

    /**
     * Codes autorisés (doivent correspondre aux valeurs des checkboxes JSP).
     */
    /** Codes et libellés d'analyses (partagés avec la génération PDF). */
    public static final LinkedHashMap<String, String> ANALYSES_DISPONIBLES = new LinkedHashMap<>();

    static {
        ANALYSES_DISPONIBLES.put("NFS", "Numération formule sanguine (NFS)");
        ANALYSES_DISPONIBLES.put("GLYCEMIE_A_JEUN", "Glycémie à jeun");
        ANALYSES_DISPONIBLES.put("BILAN_LIPIDIQUE", "Bilan lipidique");
        ANALYSES_DISPONIBLES.put("FONCTION_RENALE", "Fonction rénale (créatinine, DFG…)");
        ANALYSES_DISPONIBLES.put("BILAN_HEPATIQUE", "Bilan hépatique");
        ANALYSES_DISPONIBLES.put("TSH", "TSH");
        ANALYSES_DISPONIBLES.put("IONOGRAMME", "Ionogramme sanguin");
        ANALYSES_DISPONIBLES.put("CRP", "CRP");
        ANALYSES_DISPONIBLES.put("GROUPAGE_RHESUS", "Groupe / Rhésus");
        ANALYSES_DISPONIBLES.put("BU_SCHISTO", "Bilan urinaire (+ schisto si indiqué)");
    }

    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final OrdonnanceDAO ordonnanceDAO = new OrdonnanceDAO();
    private final AnalyseDemandeeDAO analyseDemandeeDAO = new AnalyseDemandeeDAO();
    private final DocumentMedicalDAO documentMedicalDAO = new DocumentMedicalDAO();

    /**
     * Affiche le formulaire de consultation pour un rendez-vous donné ({@code rdvId}).
     *
     * @param request  requête HTTP ({@code rdvId}, {@code selectedDate})
     * @param response réponse HTTP (forward JSP ou redirections)
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

        Long rdvId = parseLong(request.getParameter("rdvId"));
        String selectedDate = request.getParameter("selectedDate");
        if (rdvId == null) {
            response.sendRedirect(request.getContextPath() + "/medecin/dashboard");
            return;
        }

        RendezVous rdv = rendezVousDAO.findById(rdvId);
        if (rdv == null || !user.getId().equals(rdv.getMedecinId())) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }

        
        Consultation consultation = consultationDAO.findByRendezVousId(rdvId);

        if (consultation == null) {
            consultation = new Consultation();
            consultation.setRendezVousId(rdvId);
        }
        
        if (rdv.getStatut() != RendezVous.Statut.TERMINE) {
            rendezVousDAO.updateStatut(rdvId, RendezVous.Statut.EN_COURS);
            rdv.setStatut(RendezVous.Statut.EN_COURS);
        }
        List<MedicamentOrdonnance> medicaments = List.of();
        Set<String> demandesCodes = Set.of();

        if (consultation.getId() != null) {
            Long ordonnanceId = ordonnanceDAO.findOrdonnanceIdByConsultationId(consultation.getId());
            if (ordonnanceId != null) {
                medicaments = ordonnanceDAO.findMedicamentsByOrdonnanceId(ordonnanceId);
            }
            demandesCodes = analyseDemandeeDAO.findCodesByConsultationId(consultation.getId());
        }

        Map<String, Boolean> analyseCochees = buildAnalyseChecks(demandesCodes);

        request.setAttribute("rdv", rdv);
        request.setAttribute("consultation", consultation);
        request.setAttribute("medicaments", medicaments);
        request.setAttribute("analyseCochees", analyseCochees);
        request.setAttribute("analysesDisponibles", ANALYSES_DISPONIBLES);
        request.setAttribute("selectedDate", selectedDate);

        if (consultation.getId() != null) {
            request.setAttribute("documentsConsultation", documentMedicalDAO.findByConsultation(consultation.getId()));
        } else {
            request.setAttribute("documentsConsultation", List.of());
        }

        request.getRequestDispatcher("/medecin/consultation.jsp").forward(request, response);
    }

    /**
     * Enregistre diagnostic, remarques, médicaments et codes d'analyses demandées.
     *
     * @param request  requête HTTP (données consultation et listes répétées)
     * @param response réponse HTTP (redirection vers la même consultation)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
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

        request.setCharacterEncoding("UTF-8");

        Long rdvId = parseLong(request.getParameter("rdvId"));
        String selectedDate = request.getParameter("selectedDate");
        if (rdvId == null) {
            response.sendRedirect(request.getContextPath() + "/medecin/dashboard?error=rdv_invalid");
            return;
        }

        RendezVous rdv = rendezVousDAO.findById(rdvId);
        if (rdv == null || !user.getId().equals(rdv.getMedecinId())) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }

        String diagnostic = trimToNull(request.getParameter("diagnostic"));
        String remarque = trimToNull(request.getParameter("remarque"));
        List<MedicamentOrdonnance> medicamentsSoumis = parseMedicaments(request);

        try {
            List<String> codesAnalyse = sanitizeAnalyseCodes(request.getParameterValues("codeAnalyse"));
            consultationDAO.saveFullConsultation(rdvId, diagnostic, remarque, medicamentsSoumis, codesAnalyse);

            response.sendRedirect(request.getContextPath() + "/medecin/consultation?rdvId=" + rdvId + "&success=1&selectedDate=" + (selectedDate == null ? "" : selectedDate));
        } catch (Exception e) {
            e.printStackTrace(); 
            response.sendRedirect(request.getContextPath() + "/medecin/consultation?rdvId=" + rdvId + "&error=save_failed&selectedDate=" + (selectedDate == null ? "" : selectedDate));
        }
    }

    private Long parseLong(String v) {
        try {
            if (v == null || v.isBlank()) return null;
            return Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Map<String, Boolean> buildAnalyseChecks(Set<String> demandesCodes) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        Set<String> d = demandesCodes != null ? demandesCodes : Set.of();
        for (String code : ANALYSES_DISPONIBLES.keySet()) {
            map.put(code, d.contains(code));
        }
        return map;
    }

    private List<MedicamentOrdonnance> parseMedicaments(HttpServletRequest request) {
        String[] noms = request.getParameterValues("medicament_nom");
        String[] posos = request.getParameterValues("medicament_posologie");
        String[] durees = request.getParameterValues("medicament_duree");
        if (noms == null || noms.length == 0) {
            return List.of();
        }
        List<MedicamentOrdonnance> list = new ArrayList<>();
        for (int i = 0; i < noms.length; i++) {
            String nom = trimString(noms[i]);
            if (nom == null || nom.isEmpty()) {
                continue;
            }
            MedicamentOrdonnance m = new MedicamentOrdonnance();
            m.setNom(nom);
            m.setPosologie(trimEmptyToDash(posos, i));
            m.setDuree(trimEmptyToDash(durees, i));
            list.add(m);
        }
        return list;
    }

    private static String trimString(String s) {
        return s == null ? null : s.trim();
    }

    private static String trimAt(String[] arr, int i) {
        if (arr == null || i >= arr.length) return null;
        return arr[i] == null ? null : arr[i].trim();
    }

    private static String trimEmptyToDash(String[] arr, int i) {
        String v = trimAt(arr, i);
        return (v == null || v.isEmpty()) ? "—" : v;
    }

    private List<String> sanitizeAnalyseCodes(String[] raw) {
        if (raw == null || raw.length == 0) {
            return List.of();
        }
        Set<String> allowed = ANALYSES_DISPONIBLES.keySet();
        List<String> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            String c = s.trim();
            if (!allowed.contains(c) || seen.contains(c)) continue;
            seen.add(c);
            out.add(c);
        }
        return out;
    }
}


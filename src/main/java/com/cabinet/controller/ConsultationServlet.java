package com.cabinet.controller;

import com.cabinet.dao.ConsultationDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.Consultation;
import com.cabinet.model.MedicamentOrdonnance;
import com.cabinet.model.RendezVous;
import com.cabinet.model.User;
import com.cabinet.service.ConsultationService;
import com.cabinet.util.CsrfTokenUtil;
import com.cabinet.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur gérant la saisie des consultations médicales.
 */
@WebServlet("/medecin/consultation")
public class ConsultationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final LinkedHashMap<String, String> ANALYSES_DISPONIBLES = new LinkedHashMap<>();
    static {
        ANALYSES_DISPONIBLES.put("NFS", "Numération formule sanguine (NFS)");
        ANALYSES_DISPONIBLES.put("GLYCEMIE_A_JEUN", "Glycémie à jeun");
        ANALYSES_DISPONIBLES.put("BILAN_LIPIDIQUE", "Bilan lipidique");
        ANALYSES_DISPONIBLES.put("FONCTION_RENALE", "Fonction rénale");
        ANALYSES_DISPONIBLES.put("BILAN_HEPATIQUE", "Bilan hépatique");
        ANALYSES_DISPONIBLES.put("TSH", "TSH");
        ANALYSES_DISPONIBLES.put("IONOGRAMME", "Ionogramme sanguin");
        ANALYSES_DISPONIBLES.put("CRP", "CRP");
        ANALYSES_DISPONIBLES.put("GROUPAGE_RHESUS", "Groupe / Rhésus");
        ANALYSES_DISPONIBLES.put("BU_SCHISTO", "Bilan urinaire");
    }

    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final ConsultationService consultationService = new ConsultationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = SessionUtil.getAuthenticatedUser(request.getSession(false));
        if (user == null || user.getRole() != User.Role.MEDECIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Générer le jeton CSRF pour le formulaire de consultation
        CsrfTokenUtil.getToken(request);

        Long rdvId = parseLong(request.getParameter("rdvId"));
        if (rdvId == null) {
            response.sendRedirect(request.getContextPath() + "/medecin/dashboard");
            return;
        }

        Optional<RendezVous> rdvOpt = rendezVousDAO.findById(rdvId);
        if (rdvOpt.isEmpty() || !user.getId().equals(rdvOpt.get().getMedecinId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        RendezVous rdv = rdvOpt.get();
        Consultation consultation = consultationDAO.findByRendezVousId(rdvId).orElse(new Consultation());
        
        request.setAttribute("rdv", rdv);
        request.setAttribute("consultation", consultation);
        request.setAttribute("analysesDisponibles", ANALYSES_DISPONIBLES);
        
        request.getRequestDispatcher("/medecin/consultation.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = SessionUtil.getAuthenticatedUser(request.getSession(false));
        if (user == null || user.getRole() != User.Role.MEDECIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Long rdvId = parseLong(request.getParameter("rdvId"));
        if (rdvId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID de rendez-vous invalide");
            return;
        }

        // Validation stricte du diagnostic (longueur max par exemple)
        String diagnostic = request.getParameter("diagnostic");
        String remarque = request.getParameter("remarque");
        if (diagnostic != null && diagnostic.length() > 5000) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Diagnostic trop long");
            return;
        }

        List<MedicamentOrdonnance> medicaments = parseMedicaments(request);
        List<String> codesAnalyse = sanitizeAnalyseCodes(request.getParameterValues("codeAnalyse"));

        try {
            consultationService.saveFullConsultation(rdvId, diagnostic, remarque, medicaments, codesAnalyse);
            response.sendRedirect(request.getContextPath() + "/medecin/consultation?rdvId=" + rdvId + "&success=1");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/medecin/consultation?rdvId=" + rdvId + "&error=save_failed");
        }
    }

    private Long parseLong(String v) {
        try { return (v == null || v.isBlank()) ? null : Long.parseLong(v.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private List<MedicamentOrdonnance> parseMedicaments(HttpServletRequest request) {
        String[] noms = request.getParameterValues("medicament_nom");
        String[] posos = request.getParameterValues("medicament_posologie");
        String[] durees = request.getParameterValues("medicament_duree");
        List<MedicamentOrdonnance> list = new ArrayList<>();
        if (noms != null) {
            for (int i = 0; i < noms.length; i++) {
                if (noms[i] != null && !noms[i].isBlank()) {
                    MedicamentOrdonnance m = new MedicamentOrdonnance();
                    m.setNom(noms[i].trim());
                    m.setPosologie(posos != null && i < posos.length ? posos[i].trim() : "");
                    m.setDuree(durees != null && i < durees.length ? durees[i].trim() : "");
                    list.add(m);
                }
            }
        }
        return list;
    }

    private List<String> sanitizeAnalyseCodes(String[] raw) {
        List<String> out = new ArrayList<>();
        if (raw != null) {
            for (String s : raw) {
                if (s != null && ANALYSES_DISPONIBLES.containsKey(s.trim())) {
                    out.add(s.trim());
                }
            }
        }
        return out;
    }
}

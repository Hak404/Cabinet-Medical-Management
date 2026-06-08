package com.cabinet.controller;

import com.cabinet.model.User;
import com.cabinet.service.MedicalDocumentService;
import com.cabinet.util.EmailSendResult;
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
 * Génère les PDF médicaux (ordonnance, analyses, compte rendu) après enregistrement de la consultation.
 */
@WebServlet("/medecin/consultation/generate-documents")
public class GenerateMedicalDocumentsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final MedicalDocumentService medicalDocumentService = new MedicalDocumentService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String ctx = request.getContextPath();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(ctx + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");
        if (user.getRole() != User.Role.MEDECIN) {
            response.sendRedirect(ctx + "/error.jsp?error=forbidden");
            return;
        }

        Long rdvId = parseLong(request.getParameter("rdvId"));
        String selectedDate = request.getParameter("selectedDate");
        if (rdvId == null) {
            response.sendRedirect(ctx + "/medecin/dashboard?error=rdv_invalid");
            return;
        }

        MedicalDocumentService.GenerationResult result =
                medicalDocumentService.generateForConsultation(rdvId, user.getId());

        StringBuilder redirect = new StringBuilder(ctx)
                .append("/medecin/consultation?rdvId=").append(rdvId);
        if (selectedDate != null && !selectedDate.isBlank()) {
            redirect.append("&selectedDate=").append(URLEncoder.encode(selectedDate, StandardCharsets.UTF_8));
        }

        if (!result.isSuccess()) {
            redirect.append("&docError=").append(URLEncoder.encode(
                    result.getErrorMessage() != null ? result.getErrorMessage() : "generation_echouee",
                    StandardCharsets.UTF_8));
        } else {
            redirect.append("&docSuccess=1&docCount=").append(result.getDocuments().size());
            EmailSendResult email = result.getEmailResult();
            if (email != null && !email.wasDelivered()) {
                redirect.append("&docEmailWarning=1");
            }
        }

        response.sendRedirect(redirect.toString());
    }

    private Long parseLong(String v) {
        try {
            if (v == null || v.isBlank()) {
                return null;
            }
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return null;
        }
    }
}

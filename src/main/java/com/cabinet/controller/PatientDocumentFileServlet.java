package com.cabinet.controller;

import com.cabinet.dao.DocumentMedicalDAO;
import com.cabinet.model.DocumentMedical;
import com.cabinet.model.User;
import com.cabinet.util.DocumentStorageUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Téléchargement ou affichage inline d'un PDF (uniquement le patient propriétaire).
 */
@WebServlet("/patient/documents/file")
public class PatientDocumentFileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final DocumentMedicalDAO documentMedicalDAO = new DocumentMedicalDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String ctx = request.getContextPath();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(ctx + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");
        if (user.getRole() != User.Role.PATIENT) {
            response.sendRedirect(ctx + "/error.jsp?error=forbidden");
            return;
        }

        Long docId = parseLong(request.getParameter("id"));
        if (docId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Document invalide");
            return;
        }

        DocumentMedical doc = documentMedicalDAO.findById(docId);
        if (doc == null || !user.getId().equals(doc.getPatientId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès refusé");
            return;
        }

        Path file;
        try {
            file = DocumentStorageUtil.resolveStoredPath(doc.getFilePath());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Fichier introuvable");
            return;
        }

        if (!Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Fichier introuvable sur le serveur");
            return;
        }

        boolean download = "1".equals(request.getParameter("download"))
                || "true".equalsIgnoreCase(request.getParameter("download"));

        String fileName = doc.getFileName() != null ? doc.getFileName() : "document.pdf";
        response.setContentType("application/pdf");
        if (download) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + sanitizeFileName(fileName) + "\"");
        } else {
            response.setHeader("Content-Disposition", "inline; filename=\"" + sanitizeFileName(fileName) + "\"");
        }
        response.setContentLengthLong(Files.size(file));

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file, out);
            out.flush();
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
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

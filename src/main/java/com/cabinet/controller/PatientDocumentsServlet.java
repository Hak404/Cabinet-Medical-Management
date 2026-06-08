package com.cabinet.controller;

import com.cabinet.dao.DocumentMedicalDAO;
import com.cabinet.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Liste des documents médicaux du patient connecté.
 */
@WebServlet("/patient/documents")
public class PatientDocumentsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final DocumentMedicalDAO documentMedicalDAO = new DocumentMedicalDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
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

        request.setAttribute("documents", documentMedicalDAO.findByPatient(user.getId()));
        request.getRequestDispatcher("/patient/documents.jsp").forward(request, response);
    }
}

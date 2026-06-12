package com.cabinet.controller;

import com.cabinet.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Servlet de validation du code de vérification email après inscription.
 */
@WebServlet("/verify-registration")
public class VerifyRegistrationServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String ctx = request.getContextPath();
        HttpSession session = request.getSession(false);
        
        String email = (session != null) ? (String) session.getAttribute("pending_verification_email") : null;
        String code = request.getParameter("otp_input");

        if (email == null || code == null) {
            response.sendRedirect(ctx + "/register.jsp?error=session_expiree");
            return;
        }

        String error = authService.verifyRegistration(email, code);

        if (error == null) {
            // Succès
            session.removeAttribute("pending_verification_email");
            response.sendRedirect(ctx + "/login.jsp?success=compte_active");
        } else {
            // Erreur
            response.sendRedirect(ctx + "/register_verify_otp.jsp?error=" + error);
        }
    }
}

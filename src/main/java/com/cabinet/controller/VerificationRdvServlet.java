package com.cabinet.controller;

import com.cabinet.model.User;
import com.cabinet.util.CsrfTokenUtil;
import com.cabinet.util.RdvOtpConstants;
import com.cabinet.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Affiche la page de saisie du code OTP rendez-vous (accès contrôlé).
 */
@WebServlet("/patient/verification-rdv")
public class VerificationRdvServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String ctx = request.getContextPath();

        HttpSession session = request.getSession(false);
        User user = SessionUtil.getAuthenticatedUser(session);
        if (user == null || user.getRole() != User.Role.PATIENT) {
            response.sendRedirect(ctx + "/login");
            return;
        }

        // Générer le jeton CSRF pour les formulaires de vérification OTP
        CsrfTokenUtil.getToken(request);

        boolean rdvOtpPending = ConfirmOTPServlet.FLOW_RDV.equals(session.getAttribute(ConfirmOTPServlet.SESSION_OTP_FLOW))
                && session.getAttribute(RdvOtpConstants.TEMP_CABINET_ID) != null;
        if (!rdvOtpPending) {
            response.sendRedirect(ctx + "/patient/rendezvous");
            return;
        }

        request.getRequestDispatcher("/patient/verification-rdv.jsp").forward(request, response);
    }
}

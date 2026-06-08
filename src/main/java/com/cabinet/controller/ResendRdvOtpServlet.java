package com.cabinet.controller;

import com.cabinet.model.User;
import com.cabinet.util.EmailSendResult;
import com.cabinet.util.EmailUtil;
import com.cabinet.util.RdvOtpConstants;
import com.cabinet.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * Renvoi du code OTP pour confirmation de rendez-vous (session en cours).
 */
@WebServlet("/patient/rendezvous/resend-otp")
public class ResendRdvOtpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String ctx = request.getContextPath();

        HttpSession session = request.getSession(false);
        User user = SessionUtil.getAuthenticatedUser(session);
        if (user == null || user.getRole() != User.Role.PATIENT) {
            response.sendRedirect(ctx + "/login");
            return;
        }

        if (!ConfirmOTPServlet.FLOW_RDV.equals(session.getAttribute(ConfirmOTPServlet.SESSION_OTP_FLOW))) {
            response.sendRedirect(ctx + "/patient/rendezvous");
            return;
        }

        if (session.getAttribute(RdvOtpConstants.TEMP_CABINET_ID) == null) {
            response.sendRedirect(ctx + "/patient/rendezvous?error=session_expire");
            return;
        }

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        ConfirmOTPServlet.storeOtpInSession(session, otpCode);

        EmailSendResult emailResult = EmailUtil.sendOtpForAppointment(user.getEmail(), otpCode);
        session.removeAttribute(RdvOtpConstants.ATTR_EMAIL_DELIVERY_WARNING);
        if (!emailResult.isSuccess() || emailResult.isSkipped()) {
            session.setAttribute(RdvOtpConstants.ATTR_EMAIL_DELIVERY_WARNING,
                    emailResult.getMessage() != null ? emailResult.getMessage() : "smtp_desactive");
        }

        String redirect = ctx + "/patient/verification-rdv?resent=1";
        if (session.getAttribute(RdvOtpConstants.ATTR_EMAIL_DELIVERY_WARNING) != null) {
            redirect += "&warning=envoi_email";
        }
        response.sendRedirect(redirect);
    }
}

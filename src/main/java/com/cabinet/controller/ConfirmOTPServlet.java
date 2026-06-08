package com.cabinet.controller;

import com.cabinet.model.User;
import com.cabinet.service.RendezVousService;
import com.cabinet.util.RdvOtpConstants;
import com.cabinet.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Confirmation du code OTP envoyé par email (flux rendez-vous patient).
 */
@WebServlet("/patient/confirm-otp")
public class ConfirmOTPServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final String SESSION_OTP_FLOW = RdvOtpConstants.SESSION_OTP_FLOW;
    public static final String FLOW_RDV = RdvOtpConstants.FLOW_RDV;

    private final RendezVousService rendezVousService = new RendezVousService();

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

        String flow = (String) session.getAttribute(SESSION_OTP_FLOW);
        if (!FLOW_RDV.equals(flow)) {
            response.sendRedirect(ctx + "/patient/rendezvous");
            return;
        }

        if (isOtpExpired(session)) {
            session.removeAttribute(RdvOtpConstants.ATTR_EMAIL_OTP);
            session.removeAttribute(RdvOtpConstants.ATTR_OTP_EXPIRES_AT);
            response.sendRedirect(ctx + "/patient/verification-rdv?error=expired");
            return;
        }

        String userOtp = request.getParameter("otp_input");
        String sessionOtp = (String) session.getAttribute(RdvOtpConstants.ATTR_EMAIL_OTP);

        if (!matchesOtp(userOtp, sessionOtp)) {
            response.sendRedirect(ctx + "/patient/verification-rdv?error=1");
            return;
        }

        confirmRdv(session, response, ctx, user);
    }

    private void confirmRdv(HttpSession session, HttpServletResponse response, String ctx, User user)
            throws IOException {
        try {
            Long cabId = (Long) session.getAttribute(RdvOtpConstants.TEMP_CABINET_ID);
            Long medId = (Long) session.getAttribute(RdvOtpConstants.TEMP_MEDECIN_ID);
            LocalDate date = (LocalDate) session.getAttribute(RdvOtpConstants.TEMP_DATE);
            LocalTime time = (LocalTime) session.getAttribute(RdvOtpConstants.TEMP_START_TIME);

            if (cabId == null || medId == null || date == null || time == null) {
                clearRdvOtpSession(session);
                response.sendRedirect(ctx + "/patient/rendezvous?error=" + encode("session_invalide"));
                return;
            }

            rendezVousService.bookAppointment(user.getId(), cabId, medId, date, time);
            clearRdvOtpSession(session);
            response.sendRedirect(ctx + "/patient/dashboard?success=RDV_Confirme");

        } catch (Exception e) {
            clearRdvOtpSession(session);
            response.sendRedirect(ctx + "/patient/rendezvous?error=" + encode(
                    e.getMessage() != null ? e.getMessage() : "rdv_failed"));
        }
    }

    private static boolean matchesOtp(String userOtp, String sessionOtp) {
        return userOtp != null && sessionOtp != null && userOtp.trim().equals(sessionOtp.trim());
    }

    private static boolean isOtpExpired(HttpSession session) {
        Object raw = session.getAttribute(RdvOtpConstants.ATTR_OTP_EXPIRES_AT);
        if (!(raw instanceof Long expiresAt)) {
            return true;
        }
        return Instant.now().toEpochMilli() > expiresAt;
    }

    static void clearRdvOtpSession(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(RdvOtpConstants.ATTR_EMAIL_OTP);
        session.removeAttribute(RdvOtpConstants.ATTR_OTP_EXPIRES_AT);
        session.removeAttribute(SESSION_OTP_FLOW);
        session.removeAttribute(RdvOtpConstants.TEMP_CABINET_ID);
        session.removeAttribute(RdvOtpConstants.TEMP_MEDECIN_ID);
        session.removeAttribute(RdvOtpConstants.TEMP_DATE);
        session.removeAttribute(RdvOtpConstants.TEMP_START_TIME);
        session.removeAttribute(RdvOtpConstants.ATTR_EMAIL_DELIVERY_WARNING);
    }

    static void storeOtpInSession(HttpSession session, String otpCode) {
        session.setAttribute(RdvOtpConstants.ATTR_EMAIL_OTP, otpCode);
        long expiresAt = Instant.now().plusSeconds(RdvOtpConstants.OTP_VALIDITY_MINUTES * 60L).toEpochMilli();
        session.setAttribute(RdvOtpConstants.ATTR_OTP_EXPIRES_AT, expiresAt);
        session.setAttribute(SESSION_OTP_FLOW, FLOW_RDV);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

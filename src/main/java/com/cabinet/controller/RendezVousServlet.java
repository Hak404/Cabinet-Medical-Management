package com.cabinet.controller;

import com.cabinet.dao.CabinetDAO;
import com.cabinet.dao.MedecinDAO;
import com.cabinet.dao.RendezVousDAO;
import com.cabinet.model.Cabinet;
import com.cabinet.model.Medecin;
import com.cabinet.model.User;
import com.cabinet.service.RendezVousService;
import com.cabinet.util.EmailSendResult;
import com.cabinet.util.EmailUtil;
import com.cabinet.util.HolidayUtil;
import com.cabinet.util.RdvOtpConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Contrôleur MVC de prise de rendez-vous côté patient (avec vérification OTP par email).
 */
@WebServlet("/patient/rendezvous")
public class RendezVousServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RendezVousServlet.class.getName());

    private final CabinetDAO cabinetDAO = new CabinetDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final RendezVousService rendezVousService = new RendezVousService();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Long cabinetId = parseLong(request.getParameter("cabinetId"));
        Long medecinId = parseLong(request.getParameter("medecinId"));

        LocalDate date;
        String dateStr = request.getParameter("date");
        try {
            date = (dateStr == null || dateStr.isBlank()) ? LocalDate.now() : LocalDate.parse(dateStr);
        } catch (Exception e) {
            date = LocalDate.now();
        }

        List<RendezVousService.SlotAvailability> slots = new ArrayList<>();

        if (HolidayUtil.isClosed(date)) {
            request.setAttribute("infoMsg", "Le cabinet est fermé ce jour-là (Jour férié ou Weekend).");
        } else if (cabinetId != null && medecinId != null) {
            slots = rendezVousService.getAvailableSlots(cabinetId, medecinId, date);
        }

        List<Cabinet> cabinets = cabinetDAO.findAll();
        List<Medecin> medecins = medecinDAO.findAll();

        request.setAttribute("cabinets", cabinets);
        request.setAttribute("medecins", medecins);
        request.setAttribute("slots", slots);
        request.setAttribute("date", date);
        request.setAttribute("selectedCabinetId", cabinetId);
        request.setAttribute("selectedMedecinId", medecinId);

        request.getRequestDispatcher("/patient/rendezvous.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        Long patientId = user.getId();
        String ctx = request.getContextPath();

        try {
            Long cabinetId = parseLong(request.getParameter("cabinetId"));
            Long medecinId = parseLong(request.getParameter("medecinId"));
            LocalDate date = LocalDate.parse(request.getParameter("date"));
            LocalTime startTime = LocalTime.parse(request.getParameter("startTime"));

            if (HolidayUtil.isClosed(date)) {
                String errorMsg = "Impossible de réserver : ce jour est férié ou un weekend.";
                response.sendRedirect(ctx + "/patient/rendezvous?error=" + encode(errorMsg));
                return;
            }

            if (rendezVousDAO.hasRendezVousOnSameDay(patientId, cabinetId, date)) {
                String errorMsg = "Vous avez déjà un rendez-vous prévu dans ce cabinet pour cette journée.";
                response.sendRedirect(ctx + "/patient/rendezvous?error=" + encode(errorMsg));
                return;
            }

            // Préparation OTP — le RDV n'est pas encore enregistré en base
            session.setAttribute(RdvOtpConstants.TEMP_CABINET_ID, cabinetId);
            session.setAttribute(RdvOtpConstants.TEMP_MEDECIN_ID, medecinId);
            session.setAttribute(RdvOtpConstants.TEMP_DATE, date);
            session.setAttribute(RdvOtpConstants.TEMP_START_TIME, startTime);

            String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
            ConfirmOTPServlet.storeOtpInSession(session, otpCode);

            LOG.info(() -> "OTP RDV généré pour patient " + patientId + ", email=" + user.getEmail());

            EmailSendResult emailResult = EmailUtil.sendOtpForAppointment(user.getEmail(), otpCode);
            session.removeAttribute(RdvOtpConstants.ATTR_EMAIL_DELIVERY_WARNING);
            if (!emailResult.isSuccess()) {
                session.setAttribute(RdvOtpConstants.ATTR_EMAIL_DELIVERY_WARNING, emailResult.getMessage());
            } else if (emailResult.isSkipped()) {
                session.setAttribute(RdvOtpConstants.ATTR_EMAIL_DELIVERY_WARNING, "smtp_desactive");
            }

            String redirect = ctx + "/patient/verification-rdv";
            if (session.getAttribute(RdvOtpConstants.ATTR_EMAIL_DELIVERY_WARNING) != null) {
                redirect += "?warning=envoi_email";
            }
            response.sendRedirect(redirect);

        } catch (Exception e) {
            ConfirmOTPServlet.clearRdvOtpSession(session);
            response.sendRedirect(ctx + "/patient/rendezvous?error=" + encode(e.getMessage()));
        }
    }

    private Long parseLong(String v) {
        try {
            if (v == null || v.isBlank()) {
                return null;
            }
            return Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }

    private String encode(String s) {
        if (s == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s.replace(" ", "%20");
        }
    }
}

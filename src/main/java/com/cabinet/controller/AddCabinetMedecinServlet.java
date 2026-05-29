package com.cabinet.controller;

import com.cabinet.dao.CabinetDAO;
import com.cabinet.model.Cabinet;
import com.cabinet.model.Medecin;
import com.cabinet.model.User;
import com.cabinet.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

/**
 * Contrôleur MVC de création simultanée d'un cabinet et de son médecin titulaire.
 *
 * <p>Mappé sur {@code @WebServlet("/admin/cabinets-medecins/add")}. Construit les entités
 * {@link Cabinet} et {@link Medecin} à partir du formulaire et appelle
 * {@link CabinetDAO#createCabinetWithMedecin}. Redirige vers {@code /admin/dashboard}.</p>
 */
@WebServlet("/admin/cabinets-medecins/add")
public class AddCabinetMedecinServlet extends HttpServlet {

    private final CabinetDAO cabinetDAO = new CabinetDAO();

    /**
     * Enregistre un cabinet et un médecin associé en une seule transaction métier.
     *
     * @param request  requête HTTP (champs cabinet et médecin)
     * @param response réponse HTTP (redirection dashboard avec succès ou erreur)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        // 🔴 check login
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        User u = (User) session.getAttribute("user");

        // 🔴 check role admin
        if (u.getRole() != User.Role.ADMIN) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }

        try {

            // ================= CABINET =================
            Cabinet cabinet = new Cabinet();
            cabinet.setNom(request.getParameter("cabinetNom"));
            cabinet.setAdresse(request.getParameter("cabinetAdresse"));
            cabinet.setDureeConsultationMinutes(
                    Integer.parseInt(request.getParameter("dureeConsultation"))
            );

            // ================= MEDECIN =================
            Medecin medecin = new Medecin();
            medecin.setNom(request.getParameter("nom"));
            medecin.setPrenom(request.getParameter("prenom"));
            medecin.setEmail(request.getParameter("email"));
            medecin.setTelephone(request.getParameter("telephone"));
            medecin.setPassword(
                    PasswordUtil.hashPassword(request.getParameter("password"))
            );
            medecin.setSpecialite(request.getParameter("specialite"));


            medecin.setHeureDebut(
                    LocalTime.parse(request.getParameter("heureDebut"))
            );
            medecin.setHeureFin(
                    LocalTime.parse(request.getParameter("heureFin"))
            );

            // ================= CREATE =================
            String err = cabinetDAO.createCabinetWithMedecin(cabinet, medecin);

            if (err == null) {
                response.sendRedirect(
                        request.getContextPath() + "/admin/dashboard?success=cabinet_medecin_added"
                );
            } else {
                response.sendRedirect(
                        request.getContextPath() + "/admin/dashboard?error="
                                + URLEncoder.encode(err, StandardCharsets.UTF_8)
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(
                    request.getContextPath() + "/admin/dashboard?error=invalid_form"
            );
        }
    }
}
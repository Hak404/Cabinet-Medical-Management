package com.cabinet.controller;

import com.cabinet.dao.PatientDAO;
import com.cabinet.model.Patient;
import com.cabinet.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Contrôleur MVC d'ajout de patient par l'administrateur.
 *
 * <p>Mappé sur {@code @WebServlet("/admin/patients/add")}. Persiste un patient via
 * {@link PatientDAO} et redirige vers {@code /admin/dashboard} avec code de succès ou d'erreur.</p>
 */
@WebServlet("/admin/patients/add")
public class AddPatientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final PatientDAO patientDAO = new PatientDAO();

    /**
     * Crée un patient à partir du formulaire admin.
     *
     * @param request  requête HTTP (données patient et mot de passe)
     * @param response réponse HTTP (redirection dashboard)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setCharacterEncoding("UTF-8");

        try {
            String nom = request.getParameter("nom");
            String prenom = request.getParameter("prenom");
            String email = request.getParameter("email");
            String telephone = request.getParameter("telephone");
            String password = request.getParameter("password");
            String cin = request.getParameter("cin");
            String adresse = request.getParameter("adresse");
            LocalDate dateNaissance = LocalDate.parse(request.getParameter("dateNaissance"));

            Patient p = new Patient();
            p.setNom(nom);
            p.setPrenom(prenom);
            p.setEmail(email);
            p.setTelephone(telephone);
            p.setPassword(PasswordUtil.hashPassword(password));
            p.setCin(cin);
            p.setAdresse(adresse);
            p.setDateNaissance(dateNaissance);

            String err = patientDAO.savePatient(p);
            if (err == null) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?success=patient_added");
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=" + err);
            }
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=patient_invalid");
        }
    }
}


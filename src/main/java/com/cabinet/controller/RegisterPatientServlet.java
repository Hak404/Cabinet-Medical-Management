package com.cabinet.controller;

import com.cabinet.dao.UserDAO;
import com.cabinet.service.AuthService;
import com.cabinet.util.CsrfTokenUtil;
import com.cabinet.util.EmailSendResult;
import com.cabinet.util.EmailUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur MVC d'inscription d'un nouveau patient.
 *
 * <p>Mappé sur {@code @WebServlet("/patient/register")}. Affiche {@code /register.jsp},
 * vérifie l'unicité de l'email via {@link UserDAO}, enregistre le compte via
 * {@link AuthService}, puis envoie un email de confirmation ({@link com.cabinet.util.EmailUtil}).
 * L'échec d'envoi n'annule pas la création du compte.</p>
 */
@WebServlet("/patient/register")
public class RegisterPatientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RegisterPatientServlet.class.getName());

    private final UserDAO userDAO = new UserDAO();
    private final AuthService authService = new AuthService();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Affiche le formulaire d'inscription patient.
     *
     * @param request  requête HTTP
     * @param response réponse HTTP (forward vers {@code /register.jsp})
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        // Générer le jeton CSRF pour le formulaire d'inscription
        CsrfTokenUtil.getToken(request);

        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    /**
     * Traite l'inscription : validation, persistance et envoi du code de confirmation par email.
     *
     * @param request  requête HTTP (données du formulaire patient)
     * @param response réponse HTTP (redirections vers register.jsp ou login.jsp)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        String ctx = request.getContextPath();

        String nom = trim(request.getParameter("nom"));
        String prenom = trim(request.getParameter("prenom"));
        String telephone = trim(request.getParameter("telephone"));
        String email = trim(request.getParameter("email"));
        String cin = trim(request.getParameter("cin"));
        String adresse = trim(request.getParameter("adresse"));
        String dateNaissanceStr = request.getParameter("dateNaissance");
        String password = request.getParameter("password");

        if (nom == null || prenom == null || telephone == null || email == null || password == null
                || cin == null || adresse == null || dateNaissanceStr == null) {
            response.sendRedirect(ctx + "/register.jsp?error=champ_manquant");
            return;
        }

        LocalDate dateNaissance;
        try {
            dateNaissance = LocalDate.parse(dateNaissanceStr.trim());
        } catch (Exception e) {
            response.sendRedirect(ctx + "/register.jsp?error=date_invalide");
            return;
        }

        if (!EmailUtil.isValidEmail(email)) {
            response.sendRedirect(ctx + "/register.jsp?error=email_invalide");
            return;
        }

        if (userDAO.findByEmail(email).isPresent()) {
            response.sendRedirect(ctx + "/register.jsp?error=email_deja_utilise");
            return;
        }
LOG.info(() -> "Inscription patient — tentative enregistrement, email=" + email);

String result = authService.registerPatient(
        nom, prenom, telephone, email, password, cin, adresse, dateNaissance);

// Si le résultat est un code à 6 chiffres, c'est un succès d'inscription (en attente de vérification)
if (result != null && result.length() == 6 && result.matches("\\d{6}")) {
    String confirmationCode = result;
    LOG.info(() -> "Patient enregistré (inactif) — email=" + email + ", code=" + confirmationCode);

    EmailSendResult emailResult = EmailUtil.sendRegistrationConfirmation(email, confirmationCode);

    // Stocker l'email en session pour la page de vérification
    request.getSession().setAttribute("pending_verification_email", email);

    if (emailResult.wasDelivered()) {
        response.sendRedirect(ctx + "/register_verify_otp.jsp");
    } else {
        response.sendRedirect(ctx + "/register_verify_otp.jsp?warning=envoi_email");
    }
    return;
}

if (result != null) {
    LOG.warning(() -> "Échec enregistrement patient — email=" + email + ", erreur=" + result);
    response.sendRedirect(ctx + "/register.jsp?error=" + result);
    return;
}
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

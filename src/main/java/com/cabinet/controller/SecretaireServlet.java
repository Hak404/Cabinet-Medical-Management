package com.cabinet.controller;

import com.cabinet.dao.MedecinDAO;
import com.cabinet.dao.SecretaireDAO;
import com.cabinet.model.Medecin;
import com.cabinet.model.Secretaire;
import com.cabinet.model.User;
import com.cabinet.util.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Contrôleur MVC d'ajout d'une secrétaire par le médecin titulaire.
 *
 * <p>Mappé sur {@code @WebServlet("/medecin/secretaire/add")}. Résout le cabinet via
 * {@link MedecinDAO}, persiste la secrétaire via {@link com.cabinet.dao.SecretaireDAO}
 * et redirige vers {@code /medecin/dashboard}.</p>
 */
@WebServlet("/medecin/secretaire/add")
public class SecretaireServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final SecretaireDAO secretaireDAO = new SecretaireDAO();
    private final MedecinDAO medecinDAO = new MedecinDAO();

    /**
     * Crée une secrétaire rattachée au cabinet du médecin connecté.
     *
     * @param request  requête HTTP (nom, prénom, email, mot de passe, téléphone, bureau)
     * @param response réponse HTTP (redirection dashboard médecin)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        // Vérifie que l'utilisateur connecté est bien un médecin.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");
        if (user.getRole() != User.Role.MEDECIN) {
            response.sendRedirect(request.getContextPath() + "/error.jsp?error=forbidden");
            return;
        }

        // Récupère automatiquement le cabinet du médecin connecté (pas depuis le formulaire).
        Medecin medecin = medecinDAO.findById(user.getId()).orElse(null);;
        if (medecin == null || medecin.getCabinetId() == null) {
            response.sendRedirect(request.getContextPath() + "/medecin/dashboard?error=cabinet_introuvable");
            return;
        }

        try {
            Secretaire secretaire = new Secretaire();
            secretaire.setNom(request.getParameter("nom"));
            secretaire.setPrenom(request.getParameter("prenom"));
            secretaire.setEmail(request.getParameter("email"));
            secretaire.setPassword(PasswordUtil.hashPassword(request.getParameter("password")));
            secretaire.setTelephone(request.getParameter("telephone"));
            secretaire.setBureau(request.getParameter("bureau"));
            secretaire.setMedecinId(user.getId());
            secretaire.setCabinetId(medecin.getCabinetId());

            // La transaction commit/rollback est gérée dans SecretaireDAO.
            String err = secretaireDAO.saveSecretaire(secretaire);
            if (err == null) {
                response.sendRedirect(request.getContextPath() + "/medecin/dashboard?success=secretaire_added");
            } else {
                response.sendRedirect(request.getContextPath() + "/medecin/dashboard?error=" + err);
            }
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/medecin/dashboard?error=secretaire_invalid");
        }
    }
}

package com.cabinet.controller;

import com.cabinet.dao.CabinetDAO;
import com.cabinet.model.Cabinet;
import com.cabinet.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Contrôleur MVC d'ajout de cabinet seul (désactivé au profit du formulaire combiné).
 *
 * <p>Mappé sur {@code @WebServlet("/admin/cabinets/add")}. Redirige vers le dashboard admin
 * avec une erreur invitant à utiliser {@link AddCabinetMedecinServlet}. Utilise
 * {@link CabinetDAO} en dépendance mais ne persiste pas via cette route.</p>
 */
@WebServlet("/admin/cabinets/add")
public class AddCabinetServlet extends HttpServlet {

    private final CabinetDAO cabinetDAO = new CabinetDAO();

    /**
     * Rejette la création cabinet seul et redirige vers le formulaire combiné.
     *
     * @param request  requête HTTP
     * @param response réponse HTTP (redirection dashboard avec erreur)
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'E/S
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        // Flux unique imposé: Ajouter Cabinet + Médecin en même temps.
        response.sendRedirect(request.getContextPath() + "/admin/dashboard?error=use_cabinet_medecin_form");
    }
}


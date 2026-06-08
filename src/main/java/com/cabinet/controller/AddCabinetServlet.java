package com.cabinet.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Contrôleur MVC d'ajout de cabinet seul (désactivé au profit du formulaire combiné).
 *
 * <p>Mappé sur {@code @WebServlet("/admin/cabinets/add")}. Redirige vers le dashboard admin
 * avec une erreur invitant à utiliser {@link AddCabinetMedecinServlet}.</p>
 */
@WebServlet("/admin/cabinets/add")
public class AddCabinetServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

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


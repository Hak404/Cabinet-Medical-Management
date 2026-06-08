package com.cabinet.filter;

import com.cabinet.util.CsrfTokenUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * Filtre de protection contre les attaques CSRF.
 * Valide la présence d'un jeton valide pour toute requête modifiant l'état (POST, PUT, DELETE).
 */
public class CsrfFilter implements Filter {

    private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (PROTECTED_METHODS.contains(request.getMethod())) {
            HttpSession session = request.getSession(false);
            String requestToken = request.getParameter("csrfToken");

            if (!CsrfTokenUtil.isValid(session, requestToken)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide ou manquant.");
                return;
            }
        }

        chain.doFilter(req, res);
    }
}

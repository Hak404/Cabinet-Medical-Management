package com.cabinet.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtre ajoutant les en-têtes de sécurité HTTP recommandés.
 */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse res = (HttpServletResponse) response;

        // Empêche le site d'être affiché dans une iframe (protection Clickjacking)
        res.setHeader("X-Frame-Options", "DENY");

        // Empêche le navigateur de deviner le type de contenu (protection MIME sniffing)
        res.setHeader("X-Content-Type-Options", "nosniff");

        // Active la protection XSS intégrée aux navigateurs modernes
        res.setHeader("X-XSS-Protection", "1; mode=block");

        // Politique de sécurité du contenu (CSP) - Ajuster selon les besoins
        // Autorise uniquement les scripts provenant du même domaine et certains CDNs de confiance
        res.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
            "img-src 'self' data:; " +
            "font-src 'self' https://cdnjs.cloudflare.com;");

        chain.doFilter(request, response);
    }
}

package com.cabinet.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filtre d'encodage UTF-8 sur toutes les requêtes et réponses HTTP.
 *
 * <p><b>Rôle :</b> garantir un traitement cohérent des caractères accentués (formulaires, JSP).</p>
 * <p><b>Objectif :</b> appliquer l'encodage configuré (par défaut UTF-8) avant la chaîne
 * filtre → servlet → JSP.</p>
 * <p><b>Place MVC :</b> couche transverse en amont des contrôleurs — premier maillon après
 * le conteneur servlet, avant {@link AuthFilter} et les servlets.</p>
 *
 * @see AuthFilter
 * @since 1.0
 */
public class CharacterEncodingFilter implements Filter {

    private static final String UTF8 = StandardCharsets.UTF_8.name();
    private String encoding = UTF8;

    /**
     * Lit le paramètre d'initialisation {@code encoding} depuis {@code web.xml}.
     *
     * @param filterConfig configuration du filtre déclarée dans le descripteur de déploiement
     */
    @Override
    public void init(FilterConfig filterConfig) {
        String configured = filterConfig.getInitParameter("encoding");
        if (configured != null && !configured.isBlank()) {
            encoding = configured.trim();
        }
    }

    /**
     * Applique l'encodage à la requête et à la réponse, puis poursuit la chaîne de filtres.
     *
     * @param request requête entrante
     * @param response réponse sortante
     * @param chain chaîne de filtres / servlet suivante
     * @throws IOException en cas d'erreur d'E/S
     * @throws ServletException en cas d'erreur servlet en aval
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request.setCharacterEncoding(encoding);
        response.setCharacterEncoding(encoding);

        if (request instanceof HttpServletRequest httpRequest) {
            httpRequest.setCharacterEncoding(encoding);
        }
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setCharacterEncoding(encoding);
            String contentType = httpResponse.getContentType();
            if (contentType != null && contentType.startsWith("text/")
                    && contentType.toLowerCase().indexOf("charset=") < 0) {
                httpResponse.setContentType(contentType + ";charset=" + encoding);
            }
        }

        chain.doFilter(request, response);
    }
}

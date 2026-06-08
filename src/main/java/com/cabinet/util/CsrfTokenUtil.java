package com.cabinet.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitaire de gestion des jetons CSRF (Cross-Site Request Forgery).
 */
public final class CsrfTokenUtil {

    public static final String CSRF_TOKEN_ATTR = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokenUtil() {}

    /**
     * Génère un nouveau jeton aléatoire sécurisé.
     */
    public static String generateToken() {
        byte[] buffer = new byte[32];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    /**
     * Récupère le jeton en session ou en génère un s'il est absent.
     * Force la création d'une session si nécessaire pour stocker le jeton.
     */
    public static String getToken(HttpServletRequest request) {
        if (request == null) return null;
        return getToken(request.getSession(true));
    }

    /**
     * Récupère le jeton en session ou en génère un s'il est absent.
     */
    public static String getToken(HttpSession session) {
        if (session == null) return null;
        String token = (String) session.getAttribute(CSRF_TOKEN_ATTR);
        if (token == null) {
            token = generateToken();
            session.setAttribute(CSRF_TOKEN_ATTR, token);
        }
        return token;
    }

    /**
     * Valide si le jeton fourni correspond à celui en session.
     */
    public static boolean isValid(HttpSession session, String requestToken) {
        if (session == null || requestToken == null) return false;
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_ATTR);
        return sessionToken != null && sessionToken.equals(requestToken);
    }
}

/**
 * Filtres servlet appliqués globalement avant les contrôleurs (voir {@code WEB-INF/web.xml}).
 *
 * <p>Première barrière de la chaîne de traitement HTTP : encodage des caractères et contrôle
 * d'accès par session/rôle, avant que la requête n'atteigne un servlet du package
 * {@link com.cabinet.controller}.</p>
 *
 * <ul>
 *   <li>{@link com.cabinet.filter.CharacterEncodingFilter} — encodage UTF-8 requête/réponse.</li>
 *   <li>{@link com.cabinet.filter.AuthFilter} — authentification et autorisation par rôle.</li>
 * </ul>
 *
 * <h2>Flux de données MVC</h2>
 * <pre>
 * Navigateur → Filtres (ce package) → Servlet → Service → DAO → MySQL
 *                                                      ↓
 *                                              JSP (vue)
 * </pre>
 *
 * @see com.cabinet.controller
 * @see com.cabinet.util.SessionUtil
 * @since 1.0
 */
package com.cabinet.filter;

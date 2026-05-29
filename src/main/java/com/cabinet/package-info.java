/**
 * Cabinet Medical Management — package racine de l'application.
 *
 * <h2>Architecture MVC</h2>
 * <ul>
 *   <li><b>Vue :</b> pages JSP sous {@code webapp/} qui affichent le HTML selon le rôle.</li>
 *   <li><b>Contrôleur :</b> servlets Jakarta dans {@link com.cabinet.controller} qui traitent les requêtes HTTP,
 *       valident les entrées, orchestrent services/DAO et redirigent ou transmettent vers les vues.</li>
 *   <li><b>Modèle :</b> POJO dans {@link com.cabinet.model} représentant les entités base de données et les données de formulaire.</li>
 *   <li><b>Persistance :</b> classes DAO dans {@link com.cabinet.dao} exécutant JDBC vers MySQL.</li>
 * </ul>
 *
 * <h2>Préoccupations transverses</h2>
 * <ul>
 *   <li>{@link com.cabinet.filter.AuthFilter} — protection par session et rôle sur les URL.</li>
 *   <li>{@link com.cabinet.filter.CharacterEncodingFilter} — encodage UTF-8 sur toutes les requêtes.</li>
 *   <li>{@link com.cabinet.util} — connexion base, mots de passe, email, helpers de session.</li>
 * </ul>
 *
 * <h2>Flux de données typique</h2>
 * <pre>
 * Navigateur (formulaire JSP)
 *     → HTTP POST/GET
 *     → Servlet (contrôleur)
 *         → Service (règles métier optionnelles, ex. {@link com.cabinet.service.RendezVousService})
 *         → DAO (SQL via {@link com.cabinet.util.DBConnection})
 *         → MySQL ({@code cabinet_medical})
 *     ← ResultSet mappé vers le Modèle
 *     ← Attributs de requête sur HttpServletRequest
 *     → forward vers JSP ou redirect
 * </pre>
 *
 * <p>Exemple — prise de rendez-vous patient :
 * {@code rendezvous.jsp} → {@code RendezVousServlet} → {@code RendezVousService.bookAppointment}
 * → {@code RendezVousDAO.save} → table {@code rendez_vous}.</p>
 *
 * @since 1.0
 */
package com.cabinet;

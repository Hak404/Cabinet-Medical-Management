/**
 * Contrôleurs HTTP (servlets Jakarta) de l'application de gestion de cabinet médical.
 *
 * <p>Chaque servlet est mappée via {@code @WebServlet} et joue le rôle de contrôleur dans
 * l'architecture MVC : lecture des paramètres de requête, appel des couches
 * {@link com.cabinet.service} ou {@link com.cabinet.dao}, alimentation des attributs de requête,
 * puis redirection ou transfert vers les vues JSP.</p>
 *
 * <p>Le contrôle d'accès est assuré par {@link com.cabinet.filter.AuthFilter} selon les préfixes
 * d'URL ({@code /admin/}, {@code /medecin/}, {@code /patient/}, etc.) alignés sur
 * {@link com.cabinet.model.User.Role}.</p>
 *
 * @see com.cabinet.package-info
 */
package com.cabinet.controller;

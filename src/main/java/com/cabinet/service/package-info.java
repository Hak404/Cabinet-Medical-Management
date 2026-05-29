/**
 * Services applicatifs — logique métier réutilisable entre contrôleurs et DAO.
 *
 * <p>Les services coordonnent un ou plusieurs DAO et appliquent des règles qui ne doivent pas
 * résider dans les servlets (génération de créneaux, authentification, inscription patient).
 * Ils constituent la couche « orchestration » du modèle MVC, entre le contrôleur HTTP et la persistance.</p>
 *
 * <h2>Flux de données</h2>
 * <pre>
 * JSP (formulaire) → Servlet (contrôleur) → Service (ce package) → DAO → MySQL
 *                                              ↓
 *                                    Modèle ({@link com.cabinet.model})
 *                                              ↓
 *                                    forward / redirect → JSP
 * </pre>
 *
 * @see com.cabinet.controller
 * @see com.cabinet.dao
 * @see com.cabinet.util
 * @since 1.0
 */
package com.cabinet.service;

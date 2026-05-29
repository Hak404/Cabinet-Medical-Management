/**
 * Modèle de domaine (entités POJO) mappées depuis les tables de la base de données.
 *
 * <p>Dans l'architecture MVC, ce package représente la couche <strong>modèle</strong> :
 * objets métier transportant l'état des entités (utilisateurs, rendez-vous, consultations, etc.).
 * La plupart des acteurs étendent {@link com.cabinet.model.User} (table {@code user}) avec des
 * tables spécifiques au rôle ({@code patient}, {@code medecin}, {@code admin}, etc.). Les classes
 * DAO peuplent ces objets à partir de requêtes SQL (souvent avec JOIN) ; les servlets les transmettent
 * en attributs de requête vers les vues JSP.</p>
 *
 * @see com.cabinet.dao
 */
package com.cabinet.model;

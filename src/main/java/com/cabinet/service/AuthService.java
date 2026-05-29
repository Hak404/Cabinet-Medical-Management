package com.cabinet.service;

import com.cabinet.dao.PatientDAO;
import com.cabinet.dao.UserDAO;
import com.cabinet.model.Patient;
import com.cabinet.util.PasswordUtil;

import java.time.LocalDate;

/**
 * Service d'authentification et d'inscription des patients.
 *
 * <p><b>Rôle :</b> orchestrer la connexion et l'enregistrement d'un compte patient.</p>
 * <p><b>Objectif :</b> déléguer la persistance aux DAO tout en appliquant le hash BCrypt
 * et les contrôles de champs obligatoires avant insertion.</p>
 * <p><b>Place MVC :</b> couche service entre les servlets de login/inscription
 * ({@link com.cabinet.controller}) et {@link UserDAO} / {@link PatientDAO} → MySQL.</p>
 *
 * @see com.cabinet.util.PasswordUtil
 * @see com.cabinet.dao.UserDAO
 * @since 1.0
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    /**
     * Authentifie un utilisateur par email et mot de passe en clair.
     *
     * @param email adresse email du compte
     * @param plainPassword mot de passe saisi sur le formulaire de connexion
     * @return utilisateur authentifié, ou {@code null} si identifiants invalides
     */
    public com.cabinet.model.User login(String email, String plainPassword) {
        return userDAO.authenticate(email, plainPassword);
    }

    /**
     * Inscrit un nouveau patient (rôle {@code PATIENT}, mot de passe hashé BCrypt).
     *
     * @param nom nom de famille
     * @param prenom prénom
     * @param telephone numéro de téléphone
     * @param email adresse email (identifiant de connexion)
     * @param plainPassword mot de passe en clair
     * @param cin carte d'identité nationale
     * @param adresse adresse postale
     * @param dateNaissance date de naissance
     * @return code de succès ou d'erreur ({@code champ_manquant}, {@code champ_vide},
     *         {@code password_vide}, ou code retourné par {@link PatientDAO#savePatient})
     */
    public String registerPatient(String nom,
                                  String prenom,
                                  String telephone,
                                  String email,
                                  String plainPassword,
                                  String cin,
                                  String adresse,
                                  LocalDate dateNaissance) {

        if (nom == null || prenom == null || telephone == null || email == null || plainPassword == null || cin == null
                || adresse == null || dateNaissance == null) {
            return "champ_manquant";
        }
        if (nom.isBlank() || prenom.isBlank() || telephone.isBlank() || email.isBlank() || plainPassword.isBlank()
                || cin.isBlank() || adresse.isBlank()) {
            return "champ_vide";
        }

        String hashed = PasswordUtil.hashPassword(plainPassword);
        if (hashed == null) {
            return "password_vide";
        }
        Patient patient = new Patient(nom, prenom, email, hashed, telephone, cin, adresse, dateNaissance);

        return patientDAO.savePatient(patient);
    }
}

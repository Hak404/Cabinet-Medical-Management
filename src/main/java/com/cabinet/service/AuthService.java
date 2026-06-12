package com.cabinet.service;

import com.cabinet.dao.PatientDAO;
import com.cabinet.dao.UserDAO;
import com.cabinet.model.Patient;
import com.cabinet.util.PasswordUtil;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service d'authentification et d'inscription des patients.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final SecureRandom secureRandom = new SecureRandom();

    public com.cabinet.model.User login(String email, String plainPassword) {
        return userDAO.authenticate(email, plainPassword);
    }

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
        patient.setActive(false); // Inactif tant que l'email n'est pas vérifié

        String result = patientDAO.savePatient(patient);
        if (result == null) {
            // Génération et stockage du code de vérification
            String code = String.format("%06d", secureRandom.nextInt(1_000_000));
            Optional<com.cabinet.model.User> userOpt = userDAO.findByEmail(email);
            if (userOpt.isPresent()) {
                userDAO.updateVerificationCode(userOpt.get().getId(), code, LocalDateTime.now().plusMinutes(15));
                return code; // On retourne le code pour que le Servlet puisse l'envoyer par email
            }
            return "error_db";
        }
        return result;
    }

    public String verifyRegistration(String email, String code) {
        Optional<com.cabinet.model.User> userOpt = userDAO.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "utilisateur_introuvable";
        }

        com.cabinet.model.User user = userOpt.get();
        if (user.isActive()) {
            return "deja_actif";
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            return "code_incorrect";
        }

        if (user.getVerificationExpiry() == null || user.getVerificationExpiry().isBefore(LocalDateTime.now())) {
            return "code_expire";
        }

        userDAO.activateUser(email);
        return null; // Succès
    }
}

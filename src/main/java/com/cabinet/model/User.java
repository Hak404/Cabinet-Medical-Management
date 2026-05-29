package com.cabinet.model;

/**
 * Entité utilisateur de base, mappée sur la table {@code user}.
 * Classe parente des profils métier (patient, médecin, administrateur, etc.).
 */
public class User {

    /**
     * Rôles applicatifs autorisés pour un compte utilisateur.
     */
    public enum Role {
        /** Administrateur du système. */
        ADMIN,
        /** Médecin rattaché à un cabinet. */
        MEDECIN,
        /** Secrétaire médicale. */
        SECRETAIRE,
        /** Patient du cabinet. */
        PATIENT,
        /** Pharmacie partenaire. */
        PHARMACIE
    }

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String telephone;
    private Role role;
    private boolean active = true;

    /** Construit un utilisateur vide (instanciation par le DAO ou frameworks). */
    public User() {}

    /**
     * Construit un utilisateur avec les champs d'identité et d'authentification de base.
     *
     * @param nom       nom de famille
     * @param prenom    prénom
     * @param email     adresse e-mail (identifiant de connexion)
     * @param password  mot de passe (hashé en base)
     * @param telephone numéro de téléphone
     * @param role      rôle applicatif
     */
    public User(String nom, String prenom, String email, String password, String telephone, Role role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.telephone = telephone;
        this.role = role;
    }

    /**
     * @return identifiant technique en base
     */
    public Long getId() { return id; }

    /**
     * @param id identifiant technique en base
     */
    public void setId(Long id) { this.id = id; }

    /**
     * @return nom de famille
     */
    public String getNom() { return nom; }

    /**
     * @param nom nom de famille
     */
    public void setNom(String nom) { this.nom = nom; }

    /**
     * @return prénom
     */
    public String getPrenom() { return prenom; }

    /**
     * @param prenom prénom
     */
    public void setPrenom(String prenom) { this.prenom = prenom; }

    /**
     * @return adresse e-mail
     */
    public String getEmail() { return email; }

    /**
     * @param email adresse e-mail
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * @return mot de passe (hashé en base)
     */
    public String getPassword() { return password; }

    /**
     * @param password mot de passe
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * @return numéro de téléphone
     */
    public String getTelephone() { return telephone; }

    /**
     * @param telephone numéro de téléphone
     */
    public void setTelephone(String telephone) { this.telephone = telephone; }

    /**
     * @return rôle applicatif
     */
    public Role getRole() { return role; }

    /**
     * @param role rôle applicatif
     */
    public void setRole(Role role) { this.role = role; }

    /**
     * @return {@code true} si le compte est actif et peut se connecter
     */
    public boolean isActive() { return active; }

    /**
     * @param active {@code true} pour activer le compte
     */
    public void setActive(boolean active) { this.active = active; }
}

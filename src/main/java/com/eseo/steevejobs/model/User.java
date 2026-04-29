package com.eseo.steevejobs.model;

//Commentaires géré par IA

import java.util.List;
import java.util.ArrayList;

/**
 * Classe représentant un utilisateur du système.
 * Elle regroupe les informations personnelles, professionnelles
 * ainsi que les plannings associés.
 */

public class User {
    /** Identifiant unique de l'utilisateur */
    private int id;

    /** Informations personnelles */
    private String nom;
    private String prenom;
    private String email;

    /** Mot de passe chiffré */
    private String passwordHash;

    /** Adresse postale de l'utilisateur */
    private String adresse;

    /** Rôle de l'utilisateur */
    private String role;

    /** Poste occupé dans l'entreprise */
    private String poste;

    /** Numéro de téléphone */
    private String tel;

    /** Indique si le compte est actif ou désactivé */
    private boolean actif;

    /** Liste des plannings associés à cet utilisateur */
    private List<Planning> plannings;

    /**
     * Constructeur complet permettant d'initialiser toutes les propriétés.
     * @param id Identifiant unique
     * @param nom Nom de famille
     * @param prenom Prénom
     * @param email Adresse email
     * @param passwordHash Mot de passe chiffré
     * @param adresse Adresse postale
     * @param role Rôle de l'utilisateur
     * @param tel Numéro de téléphone
     * @param poste Poste occupé
     * @param actif Indique si le compte est actif
     */

    public User( int id, String nom, String prenom, String email, String passwordHash, String adresse, String role, String tel, String poste, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.passwordHash = passwordHash;
        this.adresse = adresse;
        this.role = role;
        this.tel = tel;
        this.poste = poste;
        this.actif = actif;

        this.plannings = new ArrayList<>();
    }

    /** --- Getters & Setters classiques --- */

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }
    public String getPrenom() {
        return prenom;
    }
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    public String getAdresse() {
        return adresse;
    }
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getTel() {
        return tel;
    }
    public void setTel(String Tel) {
        this.tel = Tel;
    }
    public String getPoste() {
        return poste;
    }
    public void setPoste(String poste) {
        this.poste = poste;
    }
    public boolean isActif() {
        return actif;
    }
    public void setActif(boolean actif) {
        this.actif = actif;
    }
    public List<Planning> getPlannings() {
        return plannings;
    }
    public void setPlannings(List<Planning> plannings) {
        this.plannings = plannings;
    }
}

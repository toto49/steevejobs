package com.eseo.steevejobs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
//Commentaires géré par IA

/**
 * Représente un utilisateur du système.
 *
 * Un utilisateur correspond à un employé de l'entreprise.
 * Il possède des informations personnelles et professionnelles,
 * ainsi que des éléments qui lui sont directement rattachés,
 */

 public class User {
    /** Identifiant unique de l'utilisateur */
    private int id;

    /** Informations personnelles */
    private String nom;
    private String prenom;
    private String email;
    private int taux;
    private int tauxPatronal;

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

    /** Liste des plannings et fiches de paye associés à cet utilisateur */
    private final List<Planning> plannings;

    private final List<FichePaye> fichesPaye;

    private int tentativesEchouees;
    private LocalDateTime bloqueJusqua;

    private LocalDateTime dateDernierEchec;
    /**
     * Constructeur par défaut.
     *
     * Initialise les listes afin d'éviter toute
     * NullPointerException lors de leur utilisation.
     */

    public User() {
        this.plannings = new ArrayList<>();
        this.fichesPaye = new ArrayList<>();
    }

    /**
     * Constructeur complet permettant d'initialiser
     * toutes les propriétés principales de l'utilisateur.
     *
     * Les listes (plannings et fiches de paie) sont initialisées vides.
     *
     * @param id            identifiant unique
     * @param nom           nom de famille
     * @param prenom        prénom
     * @param email         adresse email
     * @param passwordHash  mot de passe chiffré
     * @param adresse       adresse postale
     * @param role          rôle dans l'application
     * @param tel           numéro de téléphone
     * @param poste         poste occupé
     * @param actif         état du compte utilisateur
     */

    public User( int id, int taux, String nom, String prenom, String email, String passwordHash, String adresse, String role, String tel, String poste, boolean actif, int tauxPatronal) {
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
        this.taux = taux;
        this.tauxPatronal = tauxPatronal;

        this.plannings = new ArrayList<>();
        this.fichesPaye = new ArrayList<>();
    }
    /**
     * Constructeur simplifié (sans taux) pour les tickets et messages
     */
    public User(int id, String nom, String prenom, String email, String passwordHash,
                String adresse, String role, String tel, String poste, boolean actif) {
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
        this.taux = 0;  // Valeur par défaut
        this.tauxPatronal = 0; // valeur par defaut
        this.plannings = new ArrayList<>();
        this.fichesPaye = new ArrayList<>();
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
    public void addPlanning(Planning planning) {
        this.plannings.add(planning);
    }
    public void removePlanning(Planning planning) {
        this.plannings.remove(planning);
    }
    public List<FichePaye> getFichesPaye() {
        return fichesPaye;
    }
    public void addFichePaye(FichePaye fichePaye) {
        this.fichesPaye.add(fichePaye);
    }
    public void removeFichePaye(FichePaye fichePaye) {
        this.fichesPaye.remove(fichePaye);
    }

    public int getTentativesEchouees() {
        return tentativesEchouees;
    }

    public void setTentativesEchouees(int tentativesEchouees) {
        this.tentativesEchouees = tentativesEchouees;
    }

    public LocalDateTime getBloqueJusqua() {
        return bloqueJusqua;
    }

    public void setBloqueJusqua(LocalDateTime bloqueJusqua) {
        this.bloqueJusqua = bloqueJusqua;
    }

    public LocalDateTime getDateDernierEchec() {
        return dateDernierEchec;
    }

    public void setDateDernierEchec(LocalDateTime dateDernierEchec) {
        this.dateDernierEchec = dateDernierEchec;
    }

    public int getTaux() {return taux;}
    public void setTaux(int taux) {this.taux = taux;}

    public int getTauxPatronal() { return tauxPatronal; }
    public void setTauxPatronal(int tauxPatronal) { this.tauxPatronal = tauxPatronal; }
}
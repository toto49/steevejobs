package com.eseo.steevejobs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Compte employé de l'application.
 * Entité persistée centrale (authentification, RH, tickets, documents) ; collections planning et fiches de paie en agrégat mémoire.
 */
public class User {
    /** Identifiant unique de l'utilisateur */
    private int id;

    /** Informations personnelles */
    private String nom;
    private String prenom;
    private String email;

    /** Taux horaire ou coefficient salarial employé */
    private int taux;

    /** Part patronale associée au taux */
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

    /** Liste des plannings associés à cet utilisateur */
    private final List<Planning> plannings;

    private final List<FichePaye> fichesPaye;

    /** Compteur de tentatives de connexion échouées */
    private int tentativesEchouees;

    /** Fin de blocage temporaire après échecs répétés */
    private LocalDateTime bloqueJusqua;

    /** Horodatage du dernier échec d'authentification */
    private LocalDateTime dateDernierEchec;

    /**
     * Constructeur par défaut ; initialise les listes pour éviter les accès sur référence nulle.
     */
    public User() {
        this.plannings = new ArrayList<>();
        this.fichesPaye = new ArrayList<>();
    }

    /**
     * Construit un utilisateur avec taux salariaux et listes vides.
     *
     * @param id           identifiant unique
     * @param taux         taux employé
     * @param nom          nom de famille
     * @param prenom       prénom
     * @param email        adresse email
     * @param passwordHash mot de passe chiffré
     * @param adresse      adresse postale
     * @param role         rôle dans l'application
     * @param tel          numéro de téléphone
     * @param poste        poste occupé
     * @param actif        état du compte
     * @param tauxPatronal part patronale
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
     * Construit un utilisateur sans taux (contexte tickets/messages) ; taux à zéro par défaut.
     *
     * @param id           identifiant unique
     * @param nom          nom de famille
     * @param prenom       prénom
     * @param email        adresse email
     * @param passwordHash mot de passe chiffré
     * @param adresse      adresse postale
     * @param role         rôle
     * @param tel          téléphone
     * @param poste        poste
     * @param actif        compte actif
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
        this.taux = 0;
        this.tauxPatronal = 0;
        this.plannings = new ArrayList<>();
        this.fichesPaye = new ArrayList<>();
    }

    /** @return identifiant technique */
    public int getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return nom de famille */
    public String getNom() {
        return nom;
    }

    /** @param nom nom de famille */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /** @return prénom */
    public String getPrenom() {
        return prenom;
    }

    /** @param prenom prénom */
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    /** @return adresse email */
    public String getEmail() {
        return email;
    }

    /** @param email adresse email */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return empreinte du mot de passe */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @param passwordHash empreinte du mot de passe */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** @return adresse postale */
    public String getAdresse() {
        return adresse;
    }

    /** @param adresse adresse postale */
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    /** @return rôle applicatif */
    public String getRole() {
        return role;
    }

    /** @param role rôle applicatif */
    public void setRole(String role) {
        this.role = role;
    }

    /** @return numéro de téléphone */
    public String getTel() {
        return tel;
    }

    /** @param Tel numéro de téléphone */
    public void setTel(String Tel) {
        this.tel = Tel;
    }

    /** @return poste occupé */
    public String getPoste() {
        return poste;
    }

    /** @param poste poste occupé */
    public void setPoste(String poste) {
        this.poste = poste;
    }

    /** @return {@code true} si le compte est actif */
    public boolean isActif() {
        return actif;
    }

    /** @param actif statut actif du compte */
    public void setActif(boolean actif) {
        this.actif = actif;
    }

    /** @return plannings associés */
    public List<Planning> getPlannings() {
        return plannings;
    }

    /** @param planning créneau à ajouter */
    public void addPlanning(Planning planning) {
        this.plannings.add(planning);
    }

    /** @param planning créneau à retirer */
    public void removePlanning(Planning planning) {
        this.plannings.remove(planning);
    }

    /** @return fiches de paie associées */
    public List<FichePaye> getFichesPaye() {
        return fichesPaye;
    }

    /** @param fichePaye fiche à associer */
    public void addFichePaye(FichePaye fichePaye) {
        this.fichesPaye.add(fichePaye);
    }

    /** @param fichePaye fiche à retirer */
    public void removeFichePaye(FichePaye fichePaye) {
        this.fichesPaye.remove(fichePaye);
    }

    /** @return nombre de tentatives de connexion échouées */
    public int getTentativesEchouees() {
        return tentativesEchouees;
    }

    /** @param tentativesEchouees nombre de tentatives échouées */
    public void setTentativesEchouees(int tentativesEchouees) {
        this.tentativesEchouees = tentativesEchouees;
    }

    /** @return fin de blocage temporaire, ou {@code null} */
    public LocalDateTime getBloqueJusqua() {
        return bloqueJusqua;
    }

    /** @param bloqueJusqua fin de blocage temporaire */
    public void setBloqueJusqua(LocalDateTime bloqueJusqua) {
        this.bloqueJusqua = bloqueJusqua;
    }

    /** @return date du dernier échec de connexion */
    public LocalDateTime getDateDernierEchec() {
        return dateDernierEchec;
    }

    /** @param dateDernierEchec date du dernier échec */
    public void setDateDernierEchec(LocalDateTime dateDernierEchec) {
        this.dateDernierEchec = dateDernierEchec;
    }

    /** @return taux employé */
    public int getTaux() {return taux;}

    /** @param taux taux employé */
    public void setTaux(int taux) {this.taux = taux;}

    /** @return taux patronal */
    public int getTauxPatronal() { return tauxPatronal; }

    /** @param tauxPatronal taux patronal */
    public void setTauxPatronal(int tauxPatronal) { this.tauxPatronal = tauxPatronal; }
}

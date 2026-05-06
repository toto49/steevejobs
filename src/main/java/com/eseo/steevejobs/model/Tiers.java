package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.TiersType;

import java.util.ArrayList;
import java.util.List;

//Commentaires géré par IA

/**
 * Représente un tiers de l'entreprise.
 *
 * Un tiers correspond à une personne ou une entité externe
 * (client particulier ou entreprise) pouvant être destinataire
 * de documents commerciaux tels que des devis ou des factures.
 *
 * Un tiers peut être associé à plusieurs documents.
 */

public class Tiers {

    /** Identifiant unique du tiers */
    private int id;

    /** Nom du tiers (ou raison sociale pour une entreprise) */
    private String nom;

    /** Prénom du tiers (utilisé principalement pour les particuliers) */
    private String prenom;

    /** Type du tiers (CLIENTS ou FOURNISSEUR) */
    private TiersType type;

    /** Adresse email du tiers */
    private String email;

    /** Adresse postale du tiers */
    private String adresse;

    /** Numéro de téléphone du tiers */
    private String tel;

    /** Numéro SIRET (uniquement pour les entreprises) */
    private String siret;

    /** Numéro de TVA intracommunautaire */
    private String num_tva;

    /** Indique si le tiers est actif ou non */
    private boolean actif;

    /** Liste des documents associés à ce tiers */
    private List<Document> documents;

    /**
     * Constructeur par défaut.
     *
     * Initialise la liste des documents afin d'éviter
     * toute erreur de type NullPointerException.
     */

    public Tiers() { this.documents = new ArrayList<>();}

    /**
     * Constructeur complet permettant d'initialiser
     * toutes les informations d'un tiers.
     *
     * Le tiers est actif par défaut.
     *
     * @param id        identifiant du tiers
     * @param nom       nom ou raison sociale
     * @param prenom    prénom (facultatif selon le type)
     * @param type      type du tiers
     * @param email     email de contact
     * @param adresse   adresse postale
     * @param tel       numéro de téléphone
     * @param siret     numéro SIRET
     * @param num_tva   numéro de TVA
     */

    public Tiers( int id, String nom, String prenom, TiersType type, String email, String adresse, String tel, String siret, String num_tva ) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.type = type;
        this.email = email;
        this.adresse = adresse;
        this.tel = tel;
        this.siret = siret;
        this.num_tva = num_tva;
        this.actif = true;

        this.documents = new ArrayList<>();
    }

    public int  getId() {
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
    public TiersType getType() {
        return type;
    }
    public void setType(TiersType type) {
        this.type = type;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getAdresse() {
        return adresse;
    }
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }
    public String getTel() {
        return tel;
    }
    public void setTel(String tel) {
        this.tel = tel;
    }
    public String getSiret() {
        return siret;
    }
    public void setSiret(String siret) {
        this.siret = siret;
    }
    public String getNum_tva() {
        return num_tva;
    }
    public void setNum_tva(String num_tva) {
        this.num_tva = num_tva;
    }
    public boolean isActif() {
        return actif;
    }
    public void setActif(boolean actif) {
        this.actif = actif;
    }
    public List<Document> getDocuments() {
        return documents;
    }
    public void addDocument(Document document) {
        this.documents.add(document);
    }
    public void removeDocument(Document document) {
        this.documents.remove(document);
    }
}

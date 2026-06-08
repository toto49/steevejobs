package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.TiersType;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiers externe (client ou fournisseur) de l'entreprise.
 * Entité persistée référencée par les {@code Document} commerciaux ; gérée dans les écrans clients et devis/factures.
 */
public class Tiers {

    /** Identifiant unique du tiers */
    private int id;

    /** Nom du tiers (ou raison sociale pour une entreprise) */
    private String nom;

    /** Prénom du tiers (utilisé principalement pour les particuliers) */
    private String prenom;

    /** Type du tiers (CLIENT ou FOURNISSEUR) */
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
     * Constructeur par défaut ; initialise la liste des documents.
     */
    public Tiers() { this.documents = new ArrayList<>();}

    /**
     * Construit un tiers actif avec ses coordonnées.
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

    /** @return identifiant technique */
    public int  getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return nom ou raison sociale */
    public String getNom() {
        return nom;
    }

    /** @param nom nom ou raison sociale */
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

    /** @return type client ou fournisseur */
    public TiersType getType() {
        return type;
    }

    /** @param type type client ou fournisseur */
    public void setType(TiersType type) {
        this.type = type;
    }

    /** @return adresse email */
    public String getEmail() {
        return email;
    }

    /** @param email adresse email */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return adresse postale */
    public String getAdresse() {
        return adresse;
    }

    /** @param adresse adresse postale */
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    /** @return numéro de téléphone */
    public String getTel() {
        return tel;
    }

    /** @param tel numéro de téléphone */
    public void setTel(String tel) {
        this.tel = tel;
    }

    /** @return numéro SIRET */
    public String getSiret() {
        return siret;
    }

    /** @param siret numéro SIRET */
    public void setSiret(String siret) {
        this.siret = siret;
    }

    /** @return numéro de TVA intracommunautaire */
    public String getNum_tva() {
        return num_tva;
    }

    /** @param num_tva numéro de TVA intracommunautaire */
    public void setNum_tva(String num_tva) {
        this.num_tva = num_tva;
    }

    /** @return {@code true} si le tiers est actif */
    public boolean isActif() {
        return actif;
    }

    /** @param actif statut actif du tiers */
    public void setActif(boolean actif) {
        this.actif = actif;
    }

    /** @return documents liés (liste modifiable en mémoire) */
    public List<Document> getDocuments() {
        return documents;
    }

    /** @param document document à associer */
    public void addDocument(Document document) {
        this.documents.add(document);
    }

    /** @param document document à retirer */
    public void removeDocument(Document document) {
        this.documents.remove(document);
    }
}

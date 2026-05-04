package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//Commentaires géré par IA

/**
 * Représente un document commercial de l'entreprise.
 *
 * Un document peut être un devis, une facture ou un bon de commande.
 * Il est créé par un utilisateur (éditeur) et associé à un tiers (client).
 *
 * Cette classe contient :
 * - les informations d'identification du document
 * - les données financières (prix HT et TTC)
 * - l’état du document dans son cycle de vie
 * - les liens vers les entités métier associées (User, Tiers)
 */

public class Document {
    /** Identifiant unique du document */
    private int id;

    /** Type du document (DEVIS, FACTURE, BON_DE_COMMANDE, etc.) */
    private DocumentType type;

    /** Date de création ou d’édition du document */
    private LocalDateTime date;

    /** Montant hors taxes */
    private BigDecimal prixHt;

    /** Montant toutes taxes comprises */
    private BigDecimal prixTtc;

    /** Statut du document (À_PAYER,EN_ATTENTE,PAYÉ,REFUSÉ) */
    private DocumentStatut statut;

    /** URL ou chemin de stockage du document (PDF, etc.) */
    private String url;

    /** Tiers (client) concerné par ce document */
    private Tiers tiers;

    /** Utilisateur qui a créé ou édité le document */
    private User editeur;

    /**
     * Constructeur complet permettant d'instancier un document
     * avec toutes ses informations métier.
     *
     * @param id        identifiant du document
     * @param type      type du document
     * @param date      date de création
     * @param prixHt    montant hors taxes
     * @param prixTtc   montant toutes taxes comprises
     * @param statut    statut du document
     * @param url       emplacement de stockage
     * @param tiers     tiers destinataire
     * @param editeur   utilisateur créateur
     */

    public Document(int id, DocumentType type, LocalDateTime date,  BigDecimal prixHt, BigDecimal prixTtc, DocumentStatut statut, String url, Tiers tiers, User editeur) {
        this.id = id;
        this.type = type;
        this.date = date;
        this.prixHt = prixHt;
        this.prixTtc = prixTtc;
        this.statut = statut;
        this.url = url;
        this.tiers = tiers;
        this.editeur = editeur;
    }
    /** --- Getters & Setters classiques --- */

    public Tiers getTiers() {
        return tiers;
    }
    public void setTiers(Tiers tiers) {
        this.tiers = tiers;
    }
    public User getEditeur() {
        return editeur;
    }
    public User SetEditeur(User editeur) {
        return this.editeur;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public DocumentType getType() {
        return type;
    }
    public void setType(DocumentType type) {
        this.type = type;
    }
    public LocalDateTime getDate() {
        return date;
    }
    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    public BigDecimal getPrixHt() {
        return prixHt;
    }
    public void setPrixHt(BigDecimal prixHt) {
        this.prixHt = prixHt;
    }
    public BigDecimal getPrixTtc() {
        return prixTtc;
    }
    public void setPrixTtc(BigDecimal prixTtc) {
        this.prixTtc = prixTtc;
    }
    public DocumentStatut getStatut() {
        return statut;
    }
    public void setStatut(DocumentStatut statut) {
        this.statut = statut;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

}

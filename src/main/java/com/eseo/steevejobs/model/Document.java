package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Document commercial (devis, facture, bon de commande).
 * Entité persistée liée à un {@code Tiers} et un {@code User} éditeur ; génère PDF et workflows de paiement en UI.
 */
public class Document {
    /** Identifiant unique du document */
    private int id;

    /** Type du document (DEVIS, FACTURE, BON_DE_COMMANDE, etc.) */
    private DocumentType type;

    /** Date de création ou d'édition du document */
    private LocalDateTime date;

    /** Montant hors taxes */
    private BigDecimal prixHt;

    /** Montant toutes taxes comprises */
    private BigDecimal prixTtc;

    /** Statut du document (À_PAYER, EN_ATTENTE, PAYÉ, REFUSÉ) */
    private DocumentStatut statut;

    /** URL ou chemin de stockage du document (PDF, etc.) */
    private String url;

    /** Tiers (client) concerné par ce document */
    private Tiers tiers;

    /** Utilisateur qui a créé ou édité le document */
    private User editeur;

    /**
     * Construit un document avec toutes ses informations métier.
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

    /** @return tiers client associé */
    public Tiers getTiers() {
        return tiers;
    }

    /** @param tiers tiers client associé */
    public void setTiers(Tiers tiers) {
        this.tiers = tiers;
    }

    /** @return utilisateur éditeur */
    public User getEditeur() {
        return editeur;
    }

    /**
     * @param editeur utilisateur éditeur (comportement historique : retourne l'éditeur courant)
     * @return éditeur actuellement associé
     */
    public User SetEditeur(User editeur) {
        return this.editeur;
    }

    /** @return identifiant technique */
    public int getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return type de document */
    public DocumentType getType() {
        return type;
    }

    /** @param type type de document */
    public void setType(DocumentType type) {
        this.type = type;
    }

    /** @return date du document */
    public LocalDateTime getDate() {
        return date;
    }

    /** @param date date du document */
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    /** @return montant HT */
    public BigDecimal getPrixHt() {
        return prixHt;
    }

    /** @param prixHt montant HT */
    public void setPrixHt(BigDecimal prixHt) {
        this.prixHt = prixHt;
    }

    /** @return montant TTC */
    public BigDecimal getPrixTtc() {
        return prixTtc;
    }

    /** @param prixTtc montant TTC */
    public void setPrixTtc(BigDecimal prixTtc) {
        this.prixTtc = prixTtc;
    }

    /** @return statut métier */
    public DocumentStatut getStatut() {
        return statut;
    }

    /** @param statut statut métier */
    public void setStatut(DocumentStatut statut) {
        this.statut = statut;
    }

    /** @return URL ou chemin du fichier */
    public String getUrl() {
        return url;
    }

    /** @param url URL ou chemin du fichier */
    public void setUrl(String url) {
        this.url = url;
    }

}

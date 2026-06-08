package com.eseo.steevejobs.model;

import java.math.BigDecimal;

/**
 * Ligne de composition d'un document commercial (association document–produit).
 * Correspond à une entrée de table de liaison en persistance ; utilisée pour le calcul des totaux en UI document.
 */
public class Composer {

    private int idDocument;
    private Produit produit;
    private BigDecimal quantite;
    /** Prix unitaire appliqué sur la ligne (peut différer du catalogue). */
    private BigDecimal prixVente;

    /**
     * Construit une ligne de composition.
     *
     * @param idDocument identifiant du document parent
     * @param produit    produit référencé
     * @param quantite   quantité commandée
     * @param prixVente  prix de vente unitaire sur la ligne
     */
    public Composer(int idDocument, Produit produit, BigDecimal quantite, BigDecimal prixVente) {
        this.idDocument = idDocument;
        this.produit    = produit;
        this.quantite   = quantite;
        this.prixVente  = prixVente;
    }

    /** @return identifiant du document parent */
    public int        getIdDocument() { return idDocument; }
    /** @return produit associé à la ligne */
    public Produit    getProduit()    { return produit; }
    /** @return quantité sur la ligne */
    public BigDecimal getQuantite()   { return quantite; }
    /** @return prix de vente unitaire */
    public BigDecimal getPrixVente()  { return prixVente; }

    /** @param idDocument identifiant du document parent */
    public void setIdDocument(int idDocument)       { this.idDocument = idDocument; }
    /** @param produit produit associé à la ligne */
    public void setProduit(Produit produit)          { this.produit = produit; }
    /** @param quantite quantité sur la ligne */
    public void setQuantite(BigDecimal quantite)     { this.quantite = quantite; }
    /** @param prixVente prix de vente unitaire */
    public void setPrixVente(BigDecimal prixVente)   { this.prixVente = prixVente; }
}

package com.eseo.steevejobs.model;

import java.math.BigDecimal;

public class Composer {

    private int idDocument;
    private Produit produit;
    private BigDecimal quantite;
    private BigDecimal prixVente;

    public Composer(int idDocument, Produit produit, BigDecimal quantite, BigDecimal prixVente) {
        this.idDocument = idDocument;
        this.produit    = produit;
        this.quantite   = quantite;
        this.prixVente  = prixVente;
    }

    public int        getIdDocument() { return idDocument; }
    public Produit    getProduit()    { return produit; }
    public BigDecimal getQuantite()   { return quantite; }
    public BigDecimal getPrixVente()  { return prixVente; }

    public void setIdDocument(int idDocument)       { this.idDocument = idDocument; }
    public void setProduit(Produit produit)          { this.produit = produit; }
    public void setQuantite(BigDecimal quantite)     { this.quantite = quantite; }
    public void setPrixVente(BigDecimal prixVente)   { this.prixVente = prixVente; }
}

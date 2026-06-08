package com.eseo.steevejobs.model;

import java.math.BigDecimal;

/**
 * Article du catalogue stock et commercial.
 * Persisté pour inventaire et lignes de documents ; seuil d'alerte et statut actif pilotent l'UI stocks.
 */
public class Produit {
    /** Identifiant unique du produit */
    private int id;

    /** Nom du produit */
    private String nom;

    /** Prix hors taxe du produit */
    private BigDecimal prixUnitaire;

    /** Taux de TVA appliqué au produit */
    private BigDecimal tauxTva;

    /** Quantité disponible en stock */
    private int quantite;

    /** Poids du produit */
    private BigDecimal poids;

    /** Indique si le produit est actif dans le catalogue */
    private boolean actif;

    /** Seuil en dessous duquel une alerte stock est déclenchée */
    private int seuilAlerte;

    /**
     * Construit un produit avec l'ensemble des attributs catalogue.
     *
     * @param id            identifiant unique
     * @param nom           nom du produit
     * @param prixUnitaire  prix hors taxe
     * @param tauxTva       taux de TVA
     * @param quantite      quantité en stock
     * @param poids         poids du produit
     * @param actif         statut actif/inactif
     * @param seuilAlerte   seuil d'alerte stock
     */
    public Produit(int id, String nom, BigDecimal prixUnitaire, BigDecimal tauxTva, int quantite, BigDecimal poids, boolean actif, int seuilAlerte) {
        this.id = id;
        this.nom = nom;
        this.prixUnitaire = prixUnitaire;
        this.tauxTva = tauxTva;
        this.quantite = quantite;
        this.poids = poids;
        this.actif = actif;
        this.seuilAlerte = seuilAlerte;
    }

    /** @return identifiant technique */
    public int  getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return nom du produit */
    public String getNom() {
        return nom;
    }

    /** @param nom nom du produit */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /** @return prix unitaire hors taxes */
    public BigDecimal getPrix() {
        return prixUnitaire;
    }

    /** @param prix prix unitaire hors taxes */
    public void setPrix(BigDecimal prix) {
        this.prixUnitaire = prix;
    }

    /** @return taux de TVA */
    public BigDecimal getTauxTva() {
        return tauxTva;
    }

    /** @param tauxTva taux de TVA */
    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTva = tauxTva;
    }

    /** @return quantité en stock */
    public int getQuantite() {
        return quantite;
    }

    /** @param quantite quantité en stock */
    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    /** @return poids du produit */
    public BigDecimal getPoid() {
        return poids;
    }

    /** @param poid poids du produit */
    public void setPoid(BigDecimal poid) {
        this.poids = poid;
    }

    /** @return {@code true} si le produit est actif au catalogue */
    public boolean isActif() {
        return actif;
    }

    /** @param actif statut actif au catalogue */
    public void setActif(boolean actif) {
        this.actif = actif;
    }

    /** @return seuil d'alerte stock */
    public int getSeuilAlerte(){
        return seuilAlerte;
    }

    /** @param seuilAlerte seuil d'alerte stock */
    public void setSeuilAlerte(int seuilAlerte){this.seuilAlerte = seuilAlerte;}
}

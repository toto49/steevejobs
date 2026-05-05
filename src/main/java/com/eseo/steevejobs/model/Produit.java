package com.eseo.steevejobs.model;

import java.math.BigDecimal;

//Commentaire générés par IA

/**
 * Représente un produit avec ses caractéristiques principales :
 * identifiant, nom, prix, TVA, quantité, poids et statut d'activité.
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

    /**
     * Constructeur complet permettant d'initialiser toutes les propriétés du produit.
     *
     * @param id            identifiant unique
     * @param nom           nom du produit
     * @param prixUnitaire prix hors taxe
     * @param tauxTva      taux de TVA
     * @param quantite      quantité en stock
     * @param poids         poids du produit
     * @param actif         statut actif/inactif
     */

    public Produit(int id, String nom, BigDecimal prixUnitaire, BigDecimal tauxTva, int quantite, BigDecimal poids, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.prixUnitaire = prixUnitaire;
        this.tauxTva = tauxTva;
        this.quantite = quantite;
        this.poids = poids;
        this.actif = actif;
    }

    /** --- Getters & Setters classiques --- */

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
    public BigDecimal getPrix() {
        return prixUnitaire;
    }
    public void setPrix(BigDecimal prix) {
        this.prixUnitaire = prix;
    }
    public BigDecimal getTauxTva() {
        return tauxTva;
    }
    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTva = tauxTva;
    }
    public int getQuantite() {
        return quantite;
    }
    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }
    public BigDecimal getPoid() {
        return poids;
    }
    public void setPoid(BigDecimal poid) {
        this.poids = poid;
    }
    public boolean isActif() {
        return actif;
    }
    public void setActif(boolean actif) {
        this.actif = actif;
    }
}

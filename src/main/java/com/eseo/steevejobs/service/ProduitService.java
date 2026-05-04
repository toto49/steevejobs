

package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.model.Produit;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class ProduitService {

    private final ProduitDAO produitDAO;

    // Constructeur par défaut
    public ProduitService() {
        this.produitDAO = new ProduitDAO();
    }

    // --------------------------------------------------------
    // MÉTHODES PUBLIQUES (Appelées par tes contrôleurs JavaFX)
    // --------------------------------------------------------

    public void ajouterProduit(Produit produit) throws IllegalArgumentException, SQLException {
        // 1. Validation métier
        validerProduit(produit);

        boolean success = produitDAO.createProduit(produit);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter le produit au catalogue.");
        }
    }

    public void modifierProduit(Produit produit) throws IllegalArgumentException, SQLException {
        if (produit.getId() <= 0) {
            throw new IllegalArgumentException("L'ID du produit est invalide pour une modification.");
        }

        validerProduit(produit);

        boolean success = produitDAO.updateProduit(produit);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de mettre à jour ce produit.");
        }
    }

    public void supprimerProduit(int idProduit) throws SQLException {
        if (idProduit <= 0) {
            throw new IllegalArgumentException("L'ID du produit est invalide.");
        }

        boolean success = produitDAO.deleteProduit(idProduit);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de supprimer ce produit. Il est peut-être lié à des factures.");
        }
    }

    public void mettreAJourStock(int idProduit, int variation) throws IllegalArgumentException, SQLException {

        if (idProduit <= 0) {
            throw new IllegalArgumentException("L'ID du produit est invalide.");
        }

        Produit produitActuel = produitDAO.getById(idProduit);
        if (produitActuel == null) {
            throw new IllegalArgumentException("Produit introuvable.");
        }

        int nouveauStock = produitActuel.getQuantite() + variation; // <-- On calcule le nouveau stock

        if (nouveauStock < 0) {
            throw new IllegalArgumentException("Le stock ne peut pas être négatif !");
        }

        boolean success = produitDAO.updateStock(idProduit, nouveauStock);
        if (!success) throw new RuntimeException("Erreur de mise à jour du stock.");
    }

    public List<Produit> obtenirTousLesProduits() throws SQLException {
        return produitDAO.findAll();
    }

    // --------------------------------------------------------
    // MÉTHODES PRIVÉES (Logique métier interne)
    // --------------------------------------------------------

    private void validerProduit(Produit produit) throws IllegalArgumentException {
        if (produit == null) {
            throw new IllegalArgumentException("Les données du produit sont vides.");
        }

        if (produit.getNom() == null || produit.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire.");
        }

        if (produit.getPrix().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix du produit ne peut pas être négatif.");
        }

        if (produit.getQuantite() < 0) {
            throw new IllegalArgumentException("Le stock initial ne peut pas être négatif.");
        }
    }
}
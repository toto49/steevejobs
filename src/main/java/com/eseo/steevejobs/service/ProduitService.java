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

    public void ajouterProduit(Produit produit) throws IllegalArgumentException, SQLException {
        validerProduit(produit);

        boolean success = produitDAO.createProduit(produit);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter le produit au catalogue.");
        }
    }

    public void modifierProduit(Produit produit) throws IllegalArgumentException, SQLException {
        if (produit == null) {
            throw new IllegalArgumentException("Les données du produit sont vides.");
        }
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

        int nouveauStock = produitActuel.getQuantite() + variation;

        if (nouveauStock < 0) {
            throw new IllegalArgumentException("Le stock ne peut pas être négatif !");
        }

        boolean success = produitDAO.updateStock(idProduit, nouveauStock);
        if (!success) {
            throw new RuntimeException("Erreur de mise à jour du stock.");
        }
    }
    public void mettreAJourStockAuto(
            int idProduit,
            Integer variationQuantite,
            BigDecimal variationPoids
    ) throws SQLException {

        if (idProduit <= 0) {
            throw new IllegalArgumentException("L'ID du produit est invalide.");
        }

        Produit produit = produitDAO.getById(idProduit);
        if (produit == null) {
            throw new IllegalArgumentException("Produit introuvable.");
        }

        if (produit.getPoid() != null) {

            if (variationPoids == null) {
                throw new IllegalArgumentException(
                        "Variation de poids requise pour un produit géré en vrac."
                );
            }

            BigDecimal nouveauPoids = produit.getPoid().add(variationPoids);

            if (nouveauPoids.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Le stock en poids ne peut pas être négatif."
                );
            }

            boolean success = produitDAO.updatePoids(idProduit, nouveauPoids);
            if (!success) {
                throw new RuntimeException("Erreur de mise à jour du stock en poids.");
            }

            return;
        }

        if (variationQuantite == null) {
            throw new IllegalArgumentException(
                    "Variation de quantité requise pour un produit unitaire."
            );
        }

        int nouvelleQuantite = produit.getQuantite() + variationQuantite;

        if (nouvelleQuantite < 0) {
            throw new IllegalArgumentException(
                    "Le stock ne peut pas être négatif."
            );
        }

        boolean success = produitDAO.updateStock(idProduit, nouvelleQuantite);
        if (!success) {
            throw new RuntimeException("Erreur de mise à jour du stock en quantité.");
        }
    }

    public List<Produit> obtenirTousLesProduits() throws SQLException {
        return produitDAO.findAll();
    }

    /** Recherche par nom (barre de recherche de la page Stock) */
    public List<Produit> rechercherProduitsParNom(String term) throws SQLException {
        if (term == null) term = "";
        return produitDAO.searchByNom(term.trim());
    }

    /** Liste des produits dont le stock ← seuil (bouton/filtre "stock bas") */
    public List<Produit> obtenirProduitsStockBas(int seuil) throws SQLException {
        if (seuil < 0) {
            throw new IllegalArgumentException("Le seuil ne peut pas être négatif.");
        }
        return produitDAO.findProduitsWithLowStock(seuil);
    }

    /** Optionnel : utile pour écran détail ou rechargement */
    public Produit obtenirProduitParId(int idProduit) throws SQLException {
        if (idProduit <= 0) {
            throw new IllegalArgumentException("L'ID du produit est invalide.");
        }
        return produitDAO.getById(idProduit);
    }

    private void validerProduit(Produit produit) throws IllegalArgumentException {
        if (produit == null) {
            throw new IllegalArgumentException("Les données du produit sont vides.");
        }

        if (produit.getNom() == null || produit.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire.");
        }

        if (produit.getPrix() == null) {
            throw new IllegalArgumentException("Le prix du produit est obligatoire.");
        }

        if (produit.getPrix().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix du produit ne peut pas être négatif.");
        }

        if (produit.getQuantite() < 0) {
            throw new IllegalArgumentException("Le stock initial ne peut pas être négatif.");
        }

        if (produit.getTauxTva() != null && produit.getTauxTva().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le taux de TVA ne peut pas être négatif.");
        }

        if (produit.getPoid() != null && produit.getPoid().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le poids ne peut pas être négatif.");
        }
    }
}
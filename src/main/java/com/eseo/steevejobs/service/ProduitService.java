package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.model.Produit;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Gestion du catalogue produits et des stocks (quantité ou poids selon le produit).
 * <p>
 * Règles métier : prix et nom obligatoires ; stock non négatif ;
 * produits « vrac » gérés par poids ({@code poid} non null), les autres par quantité entière.
 * Aucun effet de bord réseau.
 * </p>
 */
public class ProduitService {

    private final ProduitDAO produitDAO;

    /**
     * Constructeur par défaut.
     */
    public ProduitService() {
        this.produitDAO = new ProduitDAO();
    }

    /**
     * Constructeur avec injection du DAO (tests).
     *
     * @param produitDAO accès persistance produits
     */
    public ProduitService(ProduitDAO produitDAO) {
        this.produitDAO = produitDAO;
    }

    /**
     * Ajoute un produit au catalogue après validation.
     *
     * @param produit entité produit
     * @throws IllegalArgumentException si les données sont invalides
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si l'insertion échoue
     */
    public void ajouterProduit(Produit produit) throws IllegalArgumentException, SQLException {
        validerProduit(produit);

        boolean success = produitDAO.createProduit(produit);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter le produit au catalogue.");
        }
    }

    /**
     * Met à jour un produit existant.
     *
     * @param produit produit avec identifiant valide
     * @throws IllegalArgumentException si données invalides
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la mise à jour échoue
     */
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

    /**
     * Supprime un produit du catalogue.
     *
     * @param idProduit identifiant produit
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si suppression impossible (liaisons documents)
     */
    public void supprimerProduit(int idProduit) throws SQLException {
        if (idProduit <= 0) {
            throw new IllegalArgumentException("L'ID du produit est invalide.");
        }

        boolean success = produitDAO.deleteProduit(idProduit);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de supprimer ce produit. Il est peut-être lié à des factures.");
        }
    }

    /**
     * Applique une variation absolue sur le stock en quantité.
     *
     * @param idProduit identifiant produit
     * @param variation delta (positif ou négatif)
     * @throws IllegalArgumentException si produit introuvable ou stock négatif résultant
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la mise à jour échoue
     */
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

    /**
     * Met à jour le stock selon le mode du produit (poids ou quantité).
     *
     * @param idProduit          identifiant produit
     * @param variationQuantite  delta quantité (produits unitaires)
     * @param variationPoids     delta poids (produits vrac)
     * @throws IllegalArgumentException si le type de variation ne correspond pas au produit
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la mise à jour échoue
     */
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

    /**
     * Liste tous les produits (actifs et inactifs selon le DAO).
     *
     * @return catalogue complet
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<Produit> obtenirTousLesProduits() throws SQLException {
        return produitDAO.findAll();
    }

    /**
     * Liste les produits actifs uniquement.
     *
     * @return produits actifs
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<Produit> findAllActive() throws SQLException {
        return produitDAO.findAllActive();
    }

    /**
     * Recherche par nom (barre de recherche page Stock).
     *
     * @param term fragment de nom ; {@code null} traité comme chaîne vide
     * @return produits correspondants
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<Produit> rechercherProduitsParNom(String term) throws SQLException {
        if (term == null) term = "";
        return produitDAO.searchByNom(term.trim());
    }

    /**
     * Liste les produits dont le stock est inférieur ou égal au seuil.
     *
     * @param seuil seuil d'alerte (≥ 0)
     * @return produits en stock bas
     * @throws IllegalArgumentException si le seuil est négatif
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public List<Produit> obtenirProduitsStockBas(int seuil) throws SQLException {
        if (seuil < 0) {
            throw new IllegalArgumentException("Le seuil ne peut pas être négatif.");
        }
        return produitDAO.findProduitsWithLowStock(seuil);
    }

    /**
     * Charge un produit par identifiant.
     *
     * @param idProduit identifiant produit
     * @return produit ou {@code null} selon le DAO
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
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

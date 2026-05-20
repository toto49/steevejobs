package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Produit;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) dédié à la gestion des produits.
 *
 * Rôle :
 * - Centraliser TOUT l'accès à la base de données pour les produits
 * - Isoler le SQL du reste de l'application
 *
 * Contient toutes les opérations CRUD :
 * - CREATE  : insertion d'un produit
 * - READ    : lecture d'un ou plusieurs produits
 * - UPDATE  : mise à jour des informations ou du stock
 * - DELETE  : suppression d'un produit
 *
 * Cette classe NE CONTIENT PAS de logique métier.
 * Toute règle métier (stock >= 0, validations, etc.) est gérée par le Service.
 */
public class ProduitDAO {

    /**
     * Crée un nouveau produit dans la base de données.
     *
     * - Insère le produit
     * - Récupère la clé générée (ID)
     * - Met à jour l'objet Produit passé en paramètre
     *
     * @param produit le produit à créer
     * @return true si l'insertion a réussi
     * @throws SQLException en cas d'erreur SQL
     */
    public boolean createProduit(Produit produit) throws SQLException {
        String sql = "INSERT INTO PRODUITS (nom, prix_unitaire, taux_tva, quantite, poids, actif, seuil_alerte) " +
                "VALUES (?, ?, ?, ?, ?, ?,?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Mapping objet -> base de données
            stmt.setString(1, produit.getNom());
            stmt.setBigDecimal(2, produit.getPrix());
            stmt.setBigDecimal(3, produit.getTauxTva());
            stmt.setInt(4, produit.getQuantite());
            stmt.setBigDecimal(5, produit.getPoid());
            stmt.setBoolean(6, produit.isActif());
            stmt.setInt(7, produit.getSeuilAlerte());

            int rows = stmt.executeUpdate();

            // Récupération de l'ID auto-généré
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    produit.setId(keys.getInt(1));
                }
            }

            return rows > 0;
        }
    }

    /**
     * Met à jour toutes les informations d'un produit existant.
     *
     * @param produit le produit à mettre à jour
     * @return true si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur SQL
     */
    public boolean updateProduit(Produit produit) throws SQLException {
        String sql =
                "UPDATE PRODUITS SET nom = ?, prix_unitaire = ?, taux_tva = ?, quantite = ?, poids = ?, actif = ?, seuil_alerte = ? " +
                        "WHERE id_produits = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produit.getNom());
            stmt.setBigDecimal(2, produit.getPrix());
            stmt.setBigDecimal(3, produit.getTauxTva());
            stmt.setInt(4, produit.getQuantite());
            stmt.setBigDecimal(5, produit.getPoid());
            stmt.setBoolean(6, produit.isActif());
            stmt.setInt(7, produit.getSeuilAlerte());
            stmt.setInt(8, produit.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Supprime un produit par son identifiant.
     *
     * @param id identifiant du produit
     * @return true si le produit a été supprimé
     * @throws SQLException en cas d'erreur SQL
     */
    public boolean deleteProduit(int id) throws SQLException {
        String sql = "DELETE FROM PRODUITS WHERE id_produits = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Récupère un produit par son identifiant.
     *
     * @param id identifiant du produit
     * @return le produit trouvé, ou null s'il n'existe pas
     * @throws SQLException en cas d'erreur SQL
     */
    public Produit getById(int id) throws SQLException {
        String sql = "SELECT * FROM PRODUITS WHERE id_produits = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToProduit(rs) : null;
            }
        }
    }

    /**
     * Récupère un produit par son nom exact.
     *
     * @param nom nom du produit
     * @return le produit trouvé ou null
     * @throws SQLException en cas d'erreur SQL
     */
    public Produit getByNom(String nom) throws SQLException {
        String sql = "SELECT * FROM PRODUITS WHERE nom = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nom);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToProduit(rs) : null;
            }
        }
    }

    /**
     * Récupère la liste de tous les produits triés par nom.
     *
     * Utilisé principalement pour l'affichage de la page Stock.
     *
     * @return la liste des produits
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Produit> findAll() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM PRODUITS ORDER BY nom";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produits.add(mapRowToProduit(rs));
            }
        }
        return produits;
    }

    /**
     * Récupère les produits dont le stock est inférieur ou égal à un seuil.
     *
     * Utilisé pour les alertes de stock bas.
     *
     * @param threshold seuil de stock
     * @return la liste des produits concernés
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Produit> findProduitsWithLowStock(int threshold) throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql =
                "SELECT * FROM PRODUITS WHERE quantite <= ? AND actif = 1 ORDER BY quantite ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, threshold);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(mapRowToProduit(rs));
                }
            }
        }
        return produits;
    }

    /**
     * Recherche des produits par nom (recherche partielle).
     *
     * @param searchTerm texte recherché
     * @return la liste des produits correspondants
     * @throws SQLException en cas d'erreur SQL
     */
    public List<Produit> searchByNom(String searchTerm) throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM PRODUITS WHERE nom LIKE ? ORDER BY nom";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + searchTerm + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(mapRowToProduit(rs));
                }
            }
        }
        return produits;
    }

    /**
     * Met à jour la quantité en stock d'un produit.
     *
     * Méthode simple : le service calcule la nouvelle quantité
     * puis appelle cette méthode.
     *
     * @param id identifiant du produit
     * @param quantite nouvelle quantité
     * @return true si la mise à jour a réussi
     * @throws SQLException en cas d'erreur SQL
     */
    public boolean updateStock(int id, int quantite) throws SQLException {
        String sql = "UPDATE PRODUITS SET quantite = ? WHERE id_produits = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantite);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     *
     * - Applique une variation (+/-)
     * - Empêche le stock négatif directement en SQL
     * - Évite les problèmes de concurrence
     *
     * @param idProduit identifiant du produit
     * @param variation variation de stock
     * @return true si la mise à jour a réussi
     * @throws SQLException en cas d'erreur SQL
     */
    public boolean updateStockByVariation(int idProduit, int variation) throws SQLException {
        String sql =
                "UPDATE PRODUITS SET quantite = quantite + ? " +
                        "WHERE id_produits = ? AND (quantite + ?) >= 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, variation);
            stmt.setInt(2, idProduit);
            stmt.setInt(3, variation);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updatePoids(int id, BigDecimal poids) throws SQLException {
        String sql = "UPDATE PRODUITS SET poids = ? WHERE id_produits = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, poids);
            stmt.setInt(2, id);

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Compte le nombre total de produits.
     *
     * @return le nombre de produits
     * @throws SQLException en cas d'erreur SQL
     */
    public int countProduits() throws SQLException {
        String sql = "SELECT COUNT(*) FROM PRODUITS";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Méthode utilitaire privée.
     *
     * Permet de centraliser le mapping ResultSet -> Objet Produit
     * et d'éviter la duplication de code.
     */
    private Produit mapRowToProduit(ResultSet rs) throws SQLException {
        return new Produit(
                rs.getInt("id_produits"),
                rs.getString("nom"),
                rs.getBigDecimal("prix_unitaire"),
                rs.getBigDecimal("taux_tva"),
                rs.getInt("quantite"),
                rs.getBigDecimal("poids"),
                rs.getBoolean("actif"),
                rs.getInt("seuil_alerte")
        );
    }
}
package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des produits.
 * <p>
 * Contient les requêtes SQL (INSERT, SELECT, UPDATE, DELETE) permettant de lire
 * et sauvegarder les objets {@link com.eseo.steevejobs.model.Produit} en base de données.
 * </p>
 */
public class ProduitDAO {

    /**
     * Créer un nouveau produit
     *
     * @param produit le produit à créer
     * @return
     * @throws SQLException exception SQL
     */
    public boolean createProduit(Produit produit) throws SQLException {
        String sql = "INSERT INTO PRODUITS (nom, prix_unitaire, taux_tva, quantite, poids, actif) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, produit.getNom());
            stmt.setBigDecimal(2, produit.getPrix());
            stmt.setBigDecimal(3, produit.getTauxTva());
            stmt.setInt(4, produit.getQuantite());
            stmt.setBigDecimal(5, produit.getPoid());
            stmt.setBoolean(6, produit.isActif());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    produit.setId(generatedKeys.getInt(1));
                }
            }
            return true;

        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Mettre à jour un produit existant
     *
     * @param produit le produit à mettre à jour
     * @return
     * @throws SQLException exception SQL
     */
    public boolean updateProduit(Produit produit) throws SQLException {
        String sql = "UPDATE PRODUITS SET nom = ?, prix_unitaire = ?, taux_tva = ?, quantite = ?, poids = ?, actif = ? WHERE id_produits = ?";
        int rowsAffected;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produit.getNom());
            stmt.setBigDecimal(2, produit.getPrix());
            stmt.setBigDecimal(3, produit.getTauxTva());
            stmt.setInt(4, produit.getQuantite());
            stmt.setBigDecimal(5, produit.getPoid());
            stmt.setBoolean(6, produit.isActif());
            stmt.setInt(7, produit.getId());

            rowsAffected = stmt.executeUpdate();
        }
        return rowsAffected > 0;
    }

    /**
     * Supprimer un produit par son ID
     * @param id l'ID du produit
     * @return true si supprimé, false sinon
     * @throws SQLException exception SQL
     */
    public boolean deleteProduit(int id) throws SQLException {
        String sql = "DELETE FROM PRODUITS WHERE id_produits = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Récupérer un produit par son ID
     * @param id l'ID du produit
     * @return le produit trouvé, null sinon
     * @throws SQLException exception SQL
     */
    public Produit getById(int id) throws SQLException {
        String sql = "SELECT * FROM PRODUITS WHERE id_produits = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Produit(
                            rs.getInt("id_produits"),
                            rs.getString("nom"),
                            rs.getBigDecimal("prix_unitaire"),
                            rs.getBigDecimal("taux_tva"),
                            rs.getInt("quantite"),
                            rs.getBigDecimal("poids"),
                            rs.getBoolean("actif")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer un produit par son nom
     * @param nom le nom du produit
     * @return le produit trouvé, null sinon
     * @throws SQLException exception SQL
     */
    public Produit getByNom(String nom) throws SQLException {
        String sql = "SELECT * FROM PRODUITS WHERE nom = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Produit(
                            rs.getInt("id_produits"),
                            rs.getString("nom"),
                            rs.getBigDecimal("prix_unitaire"),
                            rs.getBigDecimal("taux_tva"),
                            rs.getInt("quantite"),
                            rs.getBigDecimal("poids"),
                            rs.getBoolean("actif")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer tous les produits.
     *
     * @return la liste de tous les produits
     * @throws SQLException exception SQL
     */
    public List<Produit> findAll() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM PRODUITS ORDER BY nom";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                produits.add(new Produit(
                        rs.getInt("id_produits"),
                        rs.getString("nom"),
                        rs.getBigDecimal("prix_unitaire"),
                        rs.getBigDecimal("taux_tva"),
                        rs.getInt("quantite"),
                        rs.getBigDecimal("poids"),
                        rs.getBoolean("actif")
                ));
            }
        }
        return produits;
    }

    /**
     * Récupérer les produits avec un stock inférieur au seuil
     * @param threshold le seuil de stock
     * @return la liste des produits avec stock bas
     * @throws SQLException exception SQL
     */
    public List<Produit> findProduitsWithLowStock(int threshold) throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM PRODUITS WHERE quantite <= ? AND actif = 1 ORDER BY quantite ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, threshold);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(new Produit(
                            rs.getInt("id_produits"),
                            rs.getString("nom"),
                            rs.getBigDecimal("prix_unitaire"),
                            rs.getBigDecimal("taux_tva"),
                            rs.getInt("quantite"),
                            rs.getBigDecimal("poids"),
                            rs.getBoolean("actif")
                    ));
                }
            }
        }
        return produits;
    }

    /**
     * Rechercher des produits par nom
     * @param searchTerm le terme de recherche
     * @return la liste des produits correspondants
     * @throws SQLException exception SQL
     */
    public List<Produit> searchByNom(String searchTerm) throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM PRODUITS WHERE nom LIKE ? ORDER BY nom";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String pattern = "%" + searchTerm + "%";
            stmt.setString(1, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(new Produit(
                            rs.getInt("id_produits"),
                            rs.getString("nom"),
                            rs.getBigDecimal("prix_unitaire"),
                            rs.getBigDecimal("taux_tva"),
                            rs.getInt("quantite"),
                            rs.getBigDecimal("poids"),
                            rs.getBoolean("actif")
                    ));
                }
            }
        }
        return produits;
    }


    /**
     * Compter le nombre total de produits
     * @return le nombre total de produits
     * @throws SQLException exception SQL
     */
    public boolean updateStock(int id, int quantite) throws SQLException {
        String sql = "UPDATE PRODUITS SET quantite = ? WHERE id_produits = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantite);
            stmt.setInt(2, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Count total products
     * @return the int
     * @throws SQLException the sql exception
     */
    public int countProduits() throws SQLException {
        String sql = "SELECT COUNT(*) FROM PRODUITS";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
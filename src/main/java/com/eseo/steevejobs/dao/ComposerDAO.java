package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComposerDAO {

    /**
     * Récupérer toutes les lignes produits d'un document
     * @param idDocument l'ID du document
     * @return la liste des lignes
     */
    public List<Composer> findByDocumentId(int idDocument) throws SQLException {
        List<Composer> lignes = new ArrayList<>();
        String sql = "SELECT c.*, p.nom, p.prix_unitaire, p.taux_tva, p.quantite, p.poids, p.actif " +
                "FROM COMPOSER c " +
                "INNER JOIN PRODUITS p ON c.id_produits = p.id_produits " +
                "WHERE c.id_documents = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDocument);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Produit produit = new Produit(
                            rs.getInt("id_produits"),
                            rs.getString("nom"),
                            rs.getBigDecimal("prix_unitaire"),
                            rs.getBigDecimal("taux_tva"),
                            rs.getInt("quantite"),
                            rs.getBigDecimal("poids"),
                            rs.getBoolean("actif"),
                            rs.getInt("seuil_alerte")
                    );
                    lignes.add(new Composer(
                            idDocument,
                            produit,
                            rs.getBigDecimal("quantite"),
                            rs.getBigDecimal("prix_vente")
                    ));
                }
            }
        }
        return lignes;
    }

    /**
     * Ajouter une ligne produit à un document
     */
    public boolean createLigne(Composer composer) throws SQLException {
        String sql = "INSERT INTO COMPOSER (id_documents, id_produits, quantite, prix_vente) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, composer.getIdDocument());
            stmt.setInt(2, composer.getProduit().getId());
            stmt.setBigDecimal(3, composer.getQuantite());
            stmt.setBigDecimal(4, composer.getPrixVente());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Supprimer toutes les lignes d'un document
     */
    public boolean deleteByDocumentId(int idDocument) throws SQLException {
        String sql = "DELETE FROM COMPOSER WHERE id_documents = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDocument);
            return stmt.executeUpdate() > 0;
        }
    }
}

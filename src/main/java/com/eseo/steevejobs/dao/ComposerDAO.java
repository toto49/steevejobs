package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Produit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données de la table {@code COMPOSER} (lignes produit d'un document).
 * <p>
 * Chaque opération ouvre une connexion via {@link DatabaseConnection} en auto-commit.
 * Les jointures avec {@code PRODUITS} enrichissent les lignes lors des lectures.
 * Les {@link SQLException} sont propagées à l'appelant.
 * </p>
 */
public class ComposerDAO {

    /**
     * Récupère toutes les lignes produit associées à un document.
     * <p>
     * SQL : {@code SELECT} sur {@code COMPOSER} avec {@code INNER JOIN PRODUITS}
     * filtré par {@code id_documents}.
     * </p>
     *
     * @param idDocument identifiant du document
     * @return liste des lignes composant le document (vide si aucune ligne)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<Composer> findByDocumentId(int idDocument) throws SQLException {
        List<Composer> lignes = new ArrayList<>();
        String sql = "SELECT c.*, p.nom, p.prix_unitaire, p.taux_tva, p.quantite, p.poids, p.actif, p.seuil_alerte " +
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
     * Insère une ligne produit dans un document.
     * <p>
     * SQL : {@code INSERT INTO COMPOSER} avec quantité et prix de vente.
     * </p>
     *
     * @param composer ligne à persister (document, produit, quantité, prix)
     * @return {@code true} si au moins une ligne a été insérée
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Supprime toutes les lignes produit d'un document.
     * <p>
     * SQL : {@code DELETE FROM COMPOSER WHERE id_documents = ?}.
     * Retourne {@code false} si aucune ligne n'existait pour ce document.
     * </p>
     *
     * @param idDocument identifiant du document
     * @return {@code true} si au moins une ligne a été supprimée
     * @throws SQLException en cas d'erreur d'accès à la base
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

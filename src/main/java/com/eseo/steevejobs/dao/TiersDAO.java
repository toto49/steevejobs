package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.TiersType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des tiers (clients/fournisseurs).
 * <p>
 * Contient les requêtes SQL (INSERT, SELECT, UPDATE, DELETE) permettant de lire
 * et sauvegarder les objets {@link com.eseo.steevejobs.model.Tiers} en base de données.
 * </p>
 */
public class TiersDAO {

    /**
     * Créer un nouveau tiers
     *
     * @param tiers le tiers à créer
     * @return
     * @throws SQLException exception SQL
     */
    public boolean createTiers(Tiers tiers) throws SQLException {
        String sql = "INSERT INTO TIERS (nom, prenom, type, email, adresse, tel, siret, num_tva, actif) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, tiers.getNom());
            stmt.setString(2, tiers.getPrenom());
            stmt.setString(3, tiers.getType().name());
            stmt.setString(4, tiers.getEmail());
            stmt.setString(5, tiers.getAdresse());
            stmt.setString(6, tiers.getTel());
            stmt.setString(7, tiers.getSiret());
            stmt.setString(8, tiers.getNum_tva());
            stmt.setBoolean(9, tiers.isActif());

            int lignesModifiees = stmt.executeUpdate();

            if (lignesModifiees > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        tiers.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Mettre à jour un tiers existant
     *
     * @param tiers le tiers à mettre à jour
     * @return
     * @throws SQLException exception SQL
     */
    public boolean updateTiers(Tiers tiers) throws SQLException {
        String sql = "UPDATE TIERS SET nom = ?, prenom = ?, type = ?, email = ?, adresse = ?, tel = ?, siret = ?, num_tva = ?, actif = ? WHERE id_tiers = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tiers.getNom());
            stmt.setString(2, tiers.getPrenom());
            stmt.setString(3, tiers.getType().name());
            stmt.setString(4, tiers.getEmail());
            stmt.setString(5, tiers.getAdresse());
            stmt.setString(6, tiers.getTel());
            stmt.setString(7, tiers.getSiret());
            stmt.setString(8, tiers.getNum_tva());
            stmt.setBoolean(9, tiers.isActif());
            stmt.setInt(10, tiers.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Supprimer un tiers par son ID
     * @param id l'ID du tiers
     * @return true si supprimé, false sinon
     * @throws SQLException exception SQL
     */
    public boolean deleteTiers(int id) throws SQLException {
        String sql = "DELETE FROM TIERS WHERE id_tiers = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Récupérer un tiers par son ID
     * @param id l'ID du tiers
     * @return le tiers trouvé, null sinon
     * @throws SQLException exception SQL
     */
    public Tiers getById(int id) throws SQLException {
        String sql = "SELECT * FROM TIERS WHERE id_tiers = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Tiers(
                            rs.getInt("id_tiers"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            TiersType.valueOf(rs.getString("type")),
                            rs.getString("email"),
                            rs.getString("adresse"),
                            rs.getString("tel"),
                            rs.getString("siret"),
                            rs.getString("num_tva")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer un tiers par son email
     * @param email l'email du tiers
     * @return le tiers trouvé, null sinon
     * @throws SQLException exception SQL
     */
    public Tiers getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM TIERS WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Tiers(
                            rs.getInt("id_tiers"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            TiersType.valueOf(rs.getString("type")),
                            rs.getString("email"),
                            rs.getString("adresse"),
                            rs.getString("tel"),
                            rs.getString("siret"),
                            rs.getString("num_tva")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer un tiers par son numéro SIRET
     * @param siret le numéro SIRET du tiers
     * @return le tiers trouvé, null sinon
     * @throws SQLException exception SQL
     */
    public Tiers getBySiret(String siret) throws SQLException {
        String sql = "SELECT * FROM TIERS WHERE siret = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, siret);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Tiers(
                            rs.getInt("id_tiers"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            TiersType.valueOf(rs.getString("type")),
                            rs.getString("email"),
                            rs.getString("adresse"),
                            rs.getString("tel"),
                            rs.getString("siret"),
                            rs.getString("num_tva")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer tous les tiers
     * @return la liste de tous les tiers
     * @throws SQLException exception SQL
     */
    public List<Tiers> findAll() throws SQLException {
        List<Tiers> tiers = new ArrayList<>();
        String sql = "SELECT * FROM TIERS ORDER BY nom, prenom";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tiers.add(new Tiers(
                        rs.getInt("id_tiers"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        TiersType.valueOf(rs.getString("type")),
                        rs.getString("email"),
                        rs.getString("adresse"),
                        rs.getString("tel"),
                        rs.getString("siret"),
                        rs.getString("num_tva")
                ));
            }
        }
        return tiers;
    }

    /**
     * Récupérer les tiers par type (CLIENT ou FOURNISSEUR)
     * @param type le type de tiers
     * @return la liste des tiers correspondants
     * @throws SQLException exception SQL
     */
    public List<Tiers> findByType(TiersType type) throws SQLException {
        List<Tiers> tiers = new ArrayList<>();
        String sql = "SELECT * FROM TIERS WHERE type = ? ORDER BY nom, prenom";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tiers.add(new Tiers(
                            rs.getInt("id_tiers"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            TiersType.valueOf(rs.getString("type")),
                            rs.getString("email"),
                            rs.getString("adresse"),
                            rs.getString("tel"),
                            rs.getString("siret"),
                            rs.getString("num_tva")
                    ));
                }
            }
        }
        return tiers;
    }

    /**
     * Rechercher des tiers par nom ou prénom
     * @param searchTerm le terme de recherche
     * @return la liste des tiers correspondants
     * @throws SQLException exception SQL
     */
    public List<Tiers> searchByName(String searchTerm) throws SQLException {
        List<Tiers> tiers = new ArrayList<>();
        String sql = "SELECT * FROM TIERS WHERE nom LIKE ? OR prenom LIKE ? ORDER BY nom, prenom";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String pattern = "%" + searchTerm + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tiers.add(new Tiers(
                            rs.getInt("id_tiers"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            TiersType.valueOf(rs.getString("type")),
                            rs.getString("email"),
                            rs.getString("adresse"),
                            rs.getString("tel"),
                            rs.getString("siret"),
                            rs.getString("num_tva")
                    ));
                }
            }
        }
        return tiers;
    }

    /**
     * Activer un tiers
     * @param id l'ID du tiers
     * @return true si activé, false sinon
     * @throws SQLException exception SQL
     */
    public boolean activateTiers(int id) throws SQLException {
        String sql = "UPDATE TIERS SET actif = 1 WHERE id_tiers = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Désactiver un tiers
     * @param id l'ID du tiers
     * @return true si désactivé, false sinon
     * @throws SQLException exception SQL
     */
    public boolean deactivateTiers(int id) throws SQLException {
        String sql = "UPDATE TIERS SET actif = 0 WHERE id_tiers = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Compter le nombre total de tiers
     * @return le nombre total de tiers
     * @throws SQLException exception SQL
     */
    public int countTiers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM TIERS";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de tiers actifs.
     *
     * @return le nombre de tiers actifs
     * @throws SQLException exception SQL
     */
    public int countActiveTiers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM TIERS WHERE actif = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de tiers par type
     * @param type le type de tiers (CLIENT ou FOURNISSEUR)
     * @return le nombre de tiers correspondants
     * @throws SQLException exception SQL
     */
    public int countByType(TiersType type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TIERS WHERE type = ? AND actif = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Vérifier si un email existe déjà
     * @param email l'email à vérifier
     * @return true si l'email existe, false sinon
     * @throws SQLException exception SQL
     */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TIERS WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Vérifier si un numéro SIRET existe déjà
     * @param siret le numéro SIRET à vérifier
     * @return true si le SIRET existe, false sinon
     * @throws SQLException exception SQL
     */
    public boolean siretExists(String siret) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TIERS WHERE siret = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, siret);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}
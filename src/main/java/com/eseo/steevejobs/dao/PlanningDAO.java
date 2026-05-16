package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des plannings.
 * <p>
 * Contient les requêtes SQL (INSERT, SELECT, UPDATE, DELETE) permettant de lire
 * et sauvegarder les objets {@link com.eseo.steevejobs.model.Planning} en base de données.
 * </p>
 */
public class PlanningDAO {

    /**
     * Créer un nouveau planning
     * @param planning le planning à créer
     * @throws SQLException exception SQL
     */
    public boolean createPlanning(Planning planning) throws SQLException {
        String sql = "INSERT INTO PLANNING (jour_debut, jour_fin, type, id_user) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setTimestamp(1, Timestamp.valueOf(planning.getJourDebut()));
            stmt.setTimestamp(2, Timestamp.valueOf(planning.getJourFin()));
            stmt.setString(3, planning.getType());
            stmt.setInt(4, planning.getUser().getId());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    planning.setId(generatedKeys.getInt(1));
                }
            }return true;

        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Mettre à jour un planning existant
     * @param planning le planning à mettre à jour
     * @throws SQLException exception SQL
     */
    public boolean updatePlanning(Planning planning) throws SQLException {
        String sql = "UPDATE PLANNING SET jour_debut = ?, jour_fin = ?, type = ?, id_user = ? WHERE id_planning = ?";

        int rowsAffected;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(planning.getJourDebut()));
            stmt.setTimestamp(2, Timestamp.valueOf(planning.getJourFin()));
            stmt.setString(3, planning.getType());
            stmt.setInt(4, planning.getUser().getId());
            stmt.setInt(5, planning.getId());

            rowsAffected = stmt.executeUpdate();
        }
        return rowsAffected > 0;
    }

    /**
     * Supprimer un planning par son ID
     * @param id l'ID du planning
     * @return true si supprimé, false sinon
     * @throws SQLException exception SQL
     */
    public boolean deletePlanning(int id) throws SQLException {
        String sql = "DELETE FROM PLANNING WHERE id_planning = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Récupérer un planning par son ID
     * @param id l'ID du planning
     * @return le planning trouvé, null sinon
     * @throws SQLException exception SQL
     */
    public Planning getById(int id) throws SQLException {
        String sql = "SELECT p.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM PLANNING p " +
                "INNER JOIN USER u ON p.id_user = u.id_user " +
                "WHERE p.id_planning = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                            rs.getInt("id_user"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("mdp"),
                            rs.getString("adresse"),
                            rs.getString("role"),
                            rs.getString("tel"),
                            rs.getString("poste"),
                            rs.getBoolean("actif")
                    );
                    return new Planning(
                            rs.getInt("id_planning"),
                            rs.getTimestamp("jour_debut").toLocalDateTime(),
                            rs.getTimestamp("jour_fin").toLocalDateTime(),
                            rs.getString("type"),
                            user
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer tous les plannings d'un utilisateur
     * @param userId l'ID de l'utilisateur
     * @return la liste des plannings de l'utilisateur
     * @throws SQLException exception SQL
     */
    public List<Planning> findByUserId(int userId) throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        String sql = "SELECT p.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM PLANNING p " +
                "INNER JOIN USER u ON p.id_user = u.id_user " +
                "WHERE p.id_user = ? " +
                "ORDER BY p.jour_debut";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User(
                            rs.getInt("id_user"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("mdp"),
                            rs.getString("adresse"),
                            rs.getString("role"),
                            rs.getString("tel"),
                            rs.getString("poste"),
                            rs.getBoolean("actif")
                    );
                    plannings.add(new Planning(
                            rs.getInt("id_planning"),
                            rs.getTimestamp("jour_debut").toLocalDateTime(),
                            rs.getTimestamp("jour_fin").toLocalDateTime(),
                            rs.getString("type"),
                            user
                    ));
                }
            }
        }
        return plannings;
    }

    /**
     * Récupérer tous les plannings
     * @return la liste de tous les plannings
     * @throws SQLException exception SQL
     */
    public List<Planning> findAll() throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        String sql = "SELECT p.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM PLANNING p " +
                "INNER JOIN USER u ON p.id_user = u.id_user " +
                "ORDER BY p.jour_debut";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User user = new User(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("mdp"),
                        rs.getString("adresse"),
                        rs.getString("role"),
                        rs.getString("tel"),
                        rs.getString("poste"),
                        rs.getBoolean("actif")
                );
                plannings.add(new Planning(
                        rs.getInt("id_planning"),
                        rs.getTimestamp("jour_debut").toLocalDateTime(),
                        rs.getTimestamp("jour_fin").toLocalDateTime(),
                        rs.getString("type"),
                        user
                ));
            }
        }
        return plannings;
    }

    /**
     * Compter le nombre total de plannings
     * @return le nombre total de plannings
     * @throws SQLException exception SQL
     */
    public int countPlannings() throws SQLException {
        String sql = "SELECT COUNT(*) FROM PLANNING";

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
     * Compter le nombre de plannings par utilisateur
     * @param userId l'ID de l'utilisateur
     * @return le nombre de plannings de l'utilisateur
     * @throws SQLException exception SQL
     */
    public int countByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PLANNING WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
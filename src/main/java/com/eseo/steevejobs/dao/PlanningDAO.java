package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des plannings.
 */
public class PlanningDAO {

    /**
     * Créer un nouveau planning
     */
    public boolean createPlanning(Planning planning) throws SQLException {
        // Ajout de 'description'
        String sql = "INSERT INTO PLANNING (jour_debut, jour_fin, type, description, id_user) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setTimestamp(1, Timestamp.valueOf(planning.getJourDebut()));
            stmt.setTimestamp(2, Timestamp.valueOf(planning.getJourFin()));
            stmt.setString(3, planning.getType());
            stmt.setString(4, planning.getDescription()); // Nouveau champ
            stmt.setInt(5, planning.getUser().getId());   // Décalé à 5

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    planning.setId(generatedKeys.getInt(1));
                }
            }
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Mettre à jour un planning existant
     */
    public boolean updatePlanning(Planning planning) throws SQLException {
        // Ajout de 'description'
        String sql = "UPDATE PLANNING SET jour_debut = ?, jour_fin = ?, type = ?, description = ?, id_user = ? WHERE id_planning = ?";

        int rowsAffected;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(planning.getJourDebut()));
            stmt.setTimestamp(2, Timestamp.valueOf(planning.getJourFin()));
            stmt.setString(3, planning.getType());
            stmt.setString(4, planning.getDescription()); // Nouveau champ
            stmt.setInt(5, planning.getUser().getId());   // Décalé à 5
            stmt.setInt(6, planning.getId());             // Décalé à 6

            rowsAffected = stmt.executeUpdate();
        }
        return rowsAffected > 0;
    }

    /**
     * Supprimer un planning par son ID
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
                    User user = extractUser(rs);
                    return new Planning(
                            rs.getInt("id_planning"),
                            rs.getTimestamp("jour_debut").toLocalDateTime(),
                            rs.getTimestamp("jour_fin").toLocalDateTime(),
                            rs.getString("type"),
                            rs.getString("description"), // Récupération du nouveau champ
                            user
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer tous les plannings d'un utilisateur
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
                    User user = extractUser(rs);
                    plannings.add(new Planning(
                            rs.getInt("id_planning"),
                            rs.getTimestamp("jour_debut").toLocalDateTime(),
                            rs.getTimestamp("jour_fin").toLocalDateTime(),
                            rs.getString("type"),
                            rs.getString("description"), // Récupération du nouveau champ
                            user
                    ));
                }
            }
        }
        return plannings;
    }

    /**
     * Récupérer tous les plannings
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
                User user = extractUser(rs);
                plannings.add(new Planning(
                        rs.getInt("id_planning"),
                        rs.getTimestamp("jour_debut").toLocalDateTime(),
                        rs.getTimestamp("jour_fin").toLocalDateTime(),
                        rs.getString("type"),
                        rs.getString("description"), // Récupération du nouveau champ
                        user
                ));
            }
        }
        return plannings;
    }

    /**
     * Compter le nombre total de plannings
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

    /**
     * Méthode utilitaire pour éviter de dupliquer la création de l'objet User
     */
    private User extractUser(ResultSet rs) throws SQLException {
        return new User(
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
    }
}
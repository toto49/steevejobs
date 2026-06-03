package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données de la table {@code PLANNING}.
 * <p>
 * Les lectures joignent {@code USER} pour hydrater l'employé concerné.
 * Chaque opération s'exécute en auto-commit sauf {@link #createPlanning} qui intercepte
 * les erreurs SQL et retourne {@code false} après journalisation sur {@code System.err}.
 * Les autres méthodes propagent les {@link SQLException}.
 * </p>
 */
public class PlanningDAO {

    /**
     * Insère un créneau de planning et récupère la clé générée.
     * <p>
     * SQL : {@code INSERT INTO PLANNING} avec {@code RETURN_GENERATED_KEYS}.
     * </p>
     *
     * @param planning créneau à persister ; l'identifiant est mis à jour après insertion
     * @return {@code true} si l'insertion a réussi, {@code false} si une erreur SQL est interceptée
     */
    public boolean createPlanning(Planning planning) throws SQLException {
        String sql = "INSERT INTO PLANNING (jour_debut, jour_fin, type, description, couleur, id_user) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setTimestamp(1, Timestamp.valueOf(planning.getJourDebut()));
            stmt.setTimestamp(2, Timestamp.valueOf(planning.getJourFin()));
            stmt.setString(3, planning.getType());
            stmt.setString(4, planning.getDescription());
            stmt.setString(5, planning.getCouleur()); // Nouveau champ
            stmt.setInt(6, planning.getUser().getId());

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
     * Met à jour un créneau de planning existant.
     * <p>
     * SQL : {@code UPDATE PLANNING} filtré par {@code id_planning}.
     * </p>
     *
     * @param planning créneau avec identifiant et champs modifiés
     * @return {@code true} si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean updatePlanning(Planning planning) throws SQLException {
        String sql = "UPDATE PLANNING SET jour_debut = ?, jour_fin = ?, type = ?, description = ?, couleur = ?, id_user = ? WHERE id_planning = ?";

        int rowsAffected;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(planning.getJourDebut()));
            stmt.setTimestamp(2, Timestamp.valueOf(planning.getJourFin()));
            stmt.setString(3, planning.getType());
            stmt.setString(4, planning.getDescription());
            stmt.setString(5, planning.getCouleur()); // Nouveau champ
            stmt.setInt(6, planning.getUser().getId());
            stmt.setInt(7, planning.getId());

            rowsAffected = stmt.executeUpdate();
        }
        return rowsAffected > 0;
    }

    /**
     * Supprime un créneau par identifiant.
     *
     * @param id identifiant du créneau
     * @return {@code true} si au moins une ligne a été supprimée
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Recherche un créneau par identifiant avec employé joint.
     *
     * @param id identifiant du créneau
     * @return créneau trouvé, ou {@code null}
     * @throws SQLException en cas d'erreur d'accès à la base
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
                            rs.getString("description"),
                            rs.getString("couleur"), // Nouveau champ
                            user
                    );
                }
                return null;
            }
        }
    }

    /**
     * Liste les créneaux d'un employé, triés par date de début croissante.
     *
     * @param userId identifiant de l'employé
     * @return liste des créneaux (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
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
                            rs.getString("description"),
                            rs.getString("couleur"), // Nouveau champ
                            user
                    ));
                }
            }
        }
        return plannings;
    }

    /**
     * Liste tous les créneaux de planning, triés par date de début croissante.
     *
     * @return liste complète (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
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
                        rs.getString("description"),
                        rs.getString("couleur"), // Nouveau champ
                        user
                ));
            }
        }
        return plannings;
    }

    /**
     * Compte le nombre total de créneaux enregistrés.
     *
     * @return nombre de créneaux ({@code 0} si la table est vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public int countPlannings() throws SQLException {
        String sql = "SELECT COUNT(*) FROM PLANNING";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) { return rs.getInt(1); }
        }
        return 0;
    }

    /**
     * Compte le nombre de créneaux d'un employé.
     *
     * @param userId identifiant de l'employé
     * @return nombre de créneaux ({@code 0} si aucun)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public int countByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PLANNING WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) { return rs.getInt(1); }
            }
        }
        return 0;
    }

    /**
     * Construit un employé à partir des colonnes {@code USER} du résultat joint.
     *
     * @param rs curseur positionné sur la ligne courante
     * @return employé hydraté
     * @throws SQLException en cas d'erreur de lecture JDBC
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
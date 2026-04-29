package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des utilisateurs.
 *  * <p>
 *  * Contient les requêtes SQL (INSERT, SELECT, UPDATE, DELETE) permettant de lire
 *  * et sauvegarder les objets {@link com.eseo.steevejobs.model.User} en base de données.
 *  * </p>
 */

public class UserDAO {

    /**
     * Create user.
     * @param user the user
     * @throws SQLException the sql exception
     */
    public void createUser(User user) throws SQLException {
        String sql = "INSERT INTO USER (nom, prenom, email, mdp, adresse, tel, role, poste, actif) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getAdresse());
            stmt.setString(6, user.getTel());
            stmt.setString(7, user.getRole());
            stmt.setString(8, user.getPoste());
            stmt.setBoolean(9, user.isActif());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * Update user.
     * @param user the user
     * @throws SQLException the sql exception
     */
    public void updateUser(User user) throws SQLException {
        String sql = "UPDATE USER SET nom = ?, prenom = ?, email = ?, mdp = ?, adresse = ?, tel = ?, role = ?, poste = ?, actif = ? WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getAdresse());
            stmt.setString(6, user.getTel());
            stmt.setString(7, user.getRole());
            stmt.setString(8, user.getPoste());
            stmt.setBoolean(9, user.isActif());
            stmt.setInt(10, user.getId());

            stmt.executeUpdate();
        }
    }

    /**
     * Delete user.
     * @param id the user id
     * @return the boolean
     * @throws SQLException the sql exception
     */
    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM USER WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Get by id user.
     * @param id the user id
     * @return the user
     * @throws SQLException the sql exception
     */
    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM USER WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
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
                return null; //si pas de resultat
            }
        }
    }

    /**
     * Get by email user.
     * @param email the email
     * @return the user
     * @throws SQLException the sql exception
     */
    public User getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM USER WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
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
                return null;
            }
        }
    }

    /**
     * Authenticate user
     * @param email        the email
     * @param passwordHash the password hash
     * @return the user if authenticated, null otherwise
     * @throws SQLException the sql exception
     */
    public User authenticate(String email, String passwordHash) throws SQLException {
        String sql = "SELECT * FROM USER WHERE email = ? AND mdp = ? AND actif = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, passwordHash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
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
                return null;
            }
        }
    }

    /**
     * Find all list
     * @return the list
     * @throws SQLException the sql exception
     */
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USER ORDER BY nom, prenom";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new User(
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
                ));
            }
        }
        return users;
    }

    /**
     * Find by role list
     * @param role the role
     * @return the list
     * @throws SQLException the sql exception
     */
    public List<User> findByRole(String role) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USER WHERE role = ? ORDER BY nom, prenom";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(
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
                    ));
                }
            }
        }
        return users;
    }

    /**
     * Find active users list
     * @return the list
     * @throws SQLException the sql exception
     */
    public List<User> findActiveUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USER WHERE actif = 1 ORDER BY nom, prenom";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new User(
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
                ));
            }
        }
        return users;
    }

    /**
     * Search by name list
     * @param searchTerm the search term
     * @return the list
     * @throws SQLException the sql exception
     */
    public List<User> searchByName(String searchTerm) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USER WHERE nom LIKE ? OR prenom LIKE ? ORDER BY nom, prenom";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String pattern = "%" + searchTerm + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(
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
                    ));
                }
            }
        }
        return users;
    }

    /**
     * Deactivate user
     * @param id the user id
     * @return the boolean
     * @throws SQLException the sql exception
     */
    public boolean deactivateUser(int id) throws SQLException {
        String sql = "UPDATE USER SET actif = 0 WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Activate user
     * @param id the user id
     * @return the boolean
     * @throws SQLException the sql exception
     */
    public boolean activateUser(int id) throws SQLException {
        String sql = "UPDATE USER SET actif = 1 WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }


    /**
     * Email exists
     * @param email the email
     * @return the boolean
     * @throws SQLException the sql exception
     */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM USER WHERE email = ?";

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
     * Count users
     *  @return the int
     *  @throws SQLException the sql exception
     */
    public int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM USER";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Count active users
     * @return the int
     * @throws SQLException the sql exception
     */
    public int countActiveUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM USER WHERE actif = 1";

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
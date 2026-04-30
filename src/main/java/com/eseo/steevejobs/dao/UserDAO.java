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
     * Créer un utilisateur.
     * @param user l'utilisateur
     * @throws SQLException l'exception SQL
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
     * Mettre à jour l'utilisateur.
     * @param user l'utilisateur
     * @throws SQLException l'exception SQL
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
     Supprimer l'utilisateur.
     @param id l'identifiant de l'utilisateur
     @return la valeur booléenne
     @throws SQLException l'exception SQL
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
     * Récupérer l'utilisateur par son identifiant.
     * @param id l'identifiant de l'utilisateur
     * @return l'utilisateur
     * @throws SQLException l'exception SQL
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
     * Récupérer l'utilisateur par son adresse e-mail.
     * @param email l'adresse e-mail
     * @return l'utilisateur
     * @throws SQLException l'exception SQL
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
     * Authentifier l'utilisateur
     * @param email l'adresse e-mail
     * @param passwordHash le hachage du mot de passe
     * @return l'utilisateur si authentifié, null sinon
     * @throws SQLException l'exception SQL
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
     * Mise à jour du mot de passe
     * @param id l'identifiant de l'utilisateur
     * @param newPasswordHash le hachage du nouveau mot de passe
     * @return la valeur booléenne
     * @throws SQLException l'exception SQL
     */
    public boolean updatePassword(int id, String newPasswordHash) throws SQLException {
        String sql = "UPDATE USER SET mdp = ? WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    /**
     * Trouver toute la liste
     * @return la liste
     * @throws SQLException l'exception SQL
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
     * Recherche par liste de rôles
     * @param role le rôle
     * @return la liste
     * @throws SQLException l'exception SQL
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
     * Trouver la liste des utilisateurs actifs
     * @return la liste
     * @throws SQLException l'exception SQL
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
     * Recherche par liste de noms
     * @param searchTerm le terme de recherche
     * @return la liste
     * @throws SQLException l'exception SQL
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
     * Désactiver l'utilisateur
     * @param id l'identifiant de l'utilisateur
     * @return la valeur booléenne
     * @throws SQLException l'exception SQL
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
     * Activer l'utilisateur
     * @param id l'identifiant de l'utilisateur
     * @return la valeur booléenne
     * @throws SQLException l'exception SQL
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
     * L'adresse e-mail existe
     * @param email l'adresse e-mail
     * @return la valeur booléenne
     * @throws SQLException l'exception SQL
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
     * Compte user
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
     * Compte user active
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
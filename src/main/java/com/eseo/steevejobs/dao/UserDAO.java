package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des utilisateurs.
 */
public class UserDAO {

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("id_user"),
                rs.getInt("taux"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("mdp"),
                rs.getString("adresse"),
                rs.getString("role"),
                rs.getString("tel"),
                rs.getString("poste"),
                rs.getBoolean("actif"),
                rs.getInt("taux_patronal")
        );

        user.setTentativesEchouees(rs.getInt("tentatives_echouees"));

        Timestamp tsBloque = rs.getTimestamp("bloque_jusqua");
        if (tsBloque != null) {
            user.setBloqueJusqua(tsBloque.toLocalDateTime());
        }
        Timestamp tsDernierEchec = rs.getTimestamp("date_dernier_echec");
        if (tsDernierEchec != null) {
            user.setDateDernierEchec(tsDernierEchec.toLocalDateTime());
        }

        return user;
    }

    public void createUser(User user) throws SQLException {
        String sql = "INSERT INTO USER (nom, prenom, email, mdp, adresse, tel, role, poste, actif, taux) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            stmt.setInt(10, user.getTaux());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updateUser(User user) throws SQLException {
        String sql = "UPDATE USER SET nom = ?, prenom = ?, email = ?, mdp = ?, adresse = ?, tel = ?, role = ?, poste = ?, actif = ?, taux = ? WHERE id_user = ?";

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
            stmt.setInt(10, user.getTaux());
            stmt.setInt(11, user.getId());

            stmt.executeUpdate();
        }
    }

    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM USER WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM USER WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
                return null;
            }
        }
    }

    public User getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM USER WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
                return null;
            }
        }
    }

    public User authenticate(String email, String passwordHash) throws SQLException {
        String sql = "SELECT * FROM USER WHERE email = ? AND mdp = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, passwordHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
                return null;
            }
        }
    }

    public boolean updatePassword(int id, String newPasswordHash) throws SQLException {
        String sql = "UPDATE USER SET mdp = ? WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateTaux(int userId, int taux) throws SQLException {
        String sql = "UPDATE USER SET taux = ? WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taux);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateTauxPatronal(int userId, int tauxPatronal) throws SQLException {
        String sql = "UPDATE USER SET taux_patronal = ? WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tauxPatronal);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USER ORDER BY nom, prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    public List<User> findByRole(String role) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USER WHERE role = ? ORDER BY nom, prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }
        return users;
    }

    public List<User> findActiveUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USER WHERE actif = 1 ORDER BY nom, prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    public List<User> searchByName(String searchTerm) throws SQLException {
        return searchByName(searchTerm, 50);
    }

    public List<User> searchByName(String searchTerm, int maxResults) throws SQLException {
        List<User> users = new ArrayList<>();
        if (searchTerm == null || searchTerm.trim().length() < 2) {
            return users;
        }

        String sql = "SELECT * FROM USER WHERE actif = 1 AND ("
                + "nom LIKE ? OR prenom LIKE ? OR email LIKE ? "
                + "OR CONCAT(prenom, ' ', nom) LIKE ? OR CONCAT(nom, ' ', prenom) LIKE ?"
                + ") ORDER BY nom, prenom LIMIT ?";

        String pattern = "%" + searchTerm.trim() + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            stmt.setString(5, pattern);
            stmt.setInt(6, Math.max(1, maxResults));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }
        return users;
    }

    public boolean deactivateUser(int id) throws SQLException {
        String sql = "UPDATE USER SET actif = 0 WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean activateUser(int id) throws SQLException {
        String sql = "UPDATE USER SET actif = 1 WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

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

    public void updateTentativesEtBlocage(int userId, int tentatives, LocalDateTime bloqueJusqua, LocalDateTime dateDernierEchec) throws SQLException {
        String query = "UPDATE USER SET tentatives_echouees = ?, bloque_jusqua = ?, date_dernier_echec = ? WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, tentatives);
            if (bloqueJusqua != null) {
                pstmt.setTimestamp(2, Timestamp.valueOf(bloqueJusqua));
            } else {
                pstmt.setNull(2, java.sql.Types.TIMESTAMP);
            }
            if (dateDernierEchec != null) {
                pstmt.setTimestamp(3, Timestamp.valueOf(dateDernierEchec));
            } else {
                pstmt.setNull(3, java.sql.Types.TIMESTAMP);
            }
            pstmt.setInt(4, userId);
            pstmt.executeUpdate();
        }
    }
}
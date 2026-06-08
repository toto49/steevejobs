package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données de la table {@code USER}.
 * <p>
 * Chaque opération s'exécute en auto-commit via {@link DatabaseConnection}.
 * Les {@link SQLException} sont propagées à l'appelant.
 * Le mapping inclut les champs de sécurité (tentatives, blocage).
 * </p>
 */
public class UserDAO {

    /**
     * Construit un utilisateur à partir d'une ligne {@code USER}, y compris les champs de sécurité.
     *
     * @param rs curseur positionné sur la ligne courante
     * @return utilisateur hydraté
     * @throws SQLException en cas d'erreur de lecture JDBC
     */
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

    /**
     * Insère un utilisateur et récupère la clé générée.
     * <p>
     * SQL : {@code INSERT INTO USER} avec {@code RETURN_GENERATED_KEYS}.
     * </p>
     *
     * @param user utilisateur à persister ; l'identifiant est mis à jour après insertion
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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

    /**
     * Met à jour un utilisateur existant.
     *
     * @param user utilisateur avec identifiant et champs modifiés
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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

    /**
     * Supprime un utilisateur par identifiant.
     *
     * @param id identifiant de l'utilisateur
     * @return {@code true} si au moins une ligne a été supprimée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM USER WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Recherche un utilisateur par identifiant.
     *
     * @param id identifiant de l'utilisateur
     * @return utilisateur trouvé, ou {@code null}
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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

    /**
     * Recherche un utilisateur par adresse e-mail.
     *
     * @param email adresse e-mail recherchée
     * @return utilisateur trouvé, ou {@code null}
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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

    /**
     * Met à jour le mot de passe hashé d'un utilisateur.
     *
     * @param id                  identifiant de l'utilisateur
     * @param hashedBCryptPassword hash BCrypt du nouveau mot de passe
     * @return {@code true} si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean updatePassword(int id, String hashedBCryptPassword) throws SQLException {
        String sql = "UPDATE USER SET mdp = ? WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedBCryptPassword);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Met à jour le taux salarial d'un utilisateur.
     *
     * @param userId identifiant de l'utilisateur
     * @param taux   nouveau taux salarial
     * @return {@code true} si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean updateTaux(int userId, int taux) throws SQLException {
        String sql = "UPDATE USER SET taux = ? WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taux);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Met à jour le taux patronal d'un utilisateur.
     *
     * @param userId       identifiant de l'utilisateur
     * @param tauxPatronal nouveau taux patronal
     * @return {@code true} si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean updateTauxPatronal(int userId, int tauxPatronal) throws SQLException {
        String sql = "UPDATE USER SET taux_patronal = ? WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tauxPatronal);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Liste tous les utilisateurs, triés par nom puis prénom.
     *
     * @return liste complète (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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

    /**
     * Liste les utilisateurs d'un rôle donné, triés par nom puis prénom.
     *
     * @param role nom du rôle
     * @return liste des utilisateurs (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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

    /**
     * Liste les utilisateurs actifs, triés par nom puis prénom.
     *
     * @return liste des utilisateurs actifs (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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

    /**
     * Recherche des utilisateurs actifs par nom, prénom ou e-mail (limite par défaut : 50).
     * <p>
     * Retourne une liste vide si le terme est {@code null} ou comporte moins de 2 caractères.
     * </p>
     *
     * @param searchTerm texte recherché
     * @return liste des utilisateurs correspondants (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<User> searchByName(String searchTerm) throws SQLException {
        return searchByName(searchTerm, 50);
    }

    /**
     * Recherche des utilisateurs actifs par nom, prénom ou e-mail avec limite configurable.
     * <p>
     * SQL : {@code LIKE} avec caractères d'échappement sur nom, prénom, e-mail et concaténations.
     * Retourne une liste vide si le terme est {@code null} ou comporte moins de 2 caractères.
     * </p>
     *
     * @param searchTerm texte recherché
     * @param maxResults nombre maximal de résultats (minimum 1)
     * @return liste des utilisateurs correspondants (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<User> searchByName(String searchTerm, int maxResults) throws SQLException {
        List<User> users = new ArrayList<>();
        if (searchTerm == null || searchTerm.trim().length() < 2) {
            return users;
        }

        String sql = "SELECT * FROM USER WHERE actif = 1 AND ("
                + "nom LIKE ? ESCAPE '\\\\' OR prenom LIKE ? ESCAPE '\\\\' OR email LIKE ? ESCAPE '\\\\' "
                + "OR CONCAT(prenom, ' ', nom) LIKE ? ESCAPE '\\\\' OR CONCAT(nom, ' ', prenom) LIKE ? ESCAPE '\\\\'"
                + ") ORDER BY nom, prenom LIMIT ?";

        String pattern = toLikePattern(searchTerm.trim());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                stmt.setString(i, pattern);
            }
            stmt.setInt(6, Math.max(1, maxResults));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }
        return users;
    }

    /**
     * Transforme un terme de recherche en motif {@code LIKE} avec caractères spéciaux échappés.
     *
     * @param searchTerm texte saisi (non vide côté appelant)
     * @return motif entouré de {@code %} avec {@code \}, {@code %} et {@code _} échappés
     */
    private String toLikePattern(String searchTerm) {
        String escaped = searchTerm
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /**
     * Désactive un utilisateur (soft delete).
     *
     * @param id identifiant de l'utilisateur
     * @return {@code true} si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean deactivateUser(int id) throws SQLException {
        String sql = "UPDATE USER SET actif = 0 WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Réactive un utilisateur précédemment désactivé.
     *
     * @param id identifiant de l'utilisateur
     * @return {@code true} si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean activateUser(int id) throws SQLException {
        String sql = "UPDATE USER SET actif = 1 WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Vérifie si une adresse e-mail est déjà enregistrée.
     *
     * @param email adresse e-mail à vérifier
     * @return {@code true} si au moins un utilisateur possède cette adresse
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Compte le nombre total d'utilisateurs enregistrés.
     *
     * @return nombre d'utilisateurs ({@code 0} si la table est vide)
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Compte le nombre d'utilisateurs actifs.
     *
     * @return nombre d'utilisateurs actifs ({@code 0} si aucun)
     * @throws SQLException en cas d'erreur d'accès à la base
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

    /**
     * Met à jour les compteurs de tentatives de connexion et la fenêtre de blocage.
     * <p>
     * SQL : {@code UPDATE USER SET tentatives_echouees, bloque_jusqua, date_dernier_echec}.
     * Les dates {@code null} sont persistées en SQL NULL.
     * </p>
     *
     * @param userId            identifiant de l'utilisateur
     * @param tentatives        nombre de tentatives échouées consécutives
     * @param bloqueJusqua      date limite de blocage (nullable)
     * @param dateDernierEchec  date du dernier échec (nullable)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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
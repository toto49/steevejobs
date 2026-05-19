package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des fiches de paie.
 * <p>
 * Contient les requêtes SQL (INSERT, SELECT, UPDATE, DELETE) permettant de lire
 * et sauvegarder les objets {@link com.eseo.steevejobs.model.FichePaye} en base de données.
 * </p>
 */
public class FichePayeDAO {

    /**
     * Créer une nouvelle fiche de paie
     * @param fichePaye la fiche de paie à créer
     * @throws SQLException exception SQL
     */
    public void createFichePaye(FichePaye fichePaye) throws SQLException {
        String sql = "INSERT INTO FICHE_PAYE (date, url, id_user) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDate(1, java.sql.Date.valueOf(fichePaye.getDate().toLocalDate()));
            stmt.setString(2, fichePaye.getUrl());
            stmt.setInt(3, fichePaye.getEmploye().getId());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    fichePaye.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * Mettre à jour une fiche de paie existante
     * @param fichePaye la fiche de paie à mettre à jour
     * @throws SQLException exception SQL
     */
    public void updateFichePaye(FichePaye fichePaye) throws SQLException {
        String sql = "UPDATE FICHE_PAYE SET date = ?, url = ?, id_user = ? WHERE id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(fichePaye.getDate().toLocalDate()));
            stmt.setString(2, fichePaye.getUrl());
            stmt.setInt(3, fichePaye.getEmploye().getId());
            stmt.setInt(4, fichePaye.getId());

            stmt.executeUpdate();
        }
    }

    /**
     * Supprimer une fiche de paie par son ID
     * @param id l'ID de la fiche de paie
     * @return true si supprimé, false sinon
     * @throws SQLException exception SQL
     */
    public boolean deleteFichePaye(int id) throws SQLException {
        String sql = "DELETE FROM FICHE_PAYE WHERE id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Récupérer une fiche de paie par son ID
     * @param id l'ID de la fiche de paie
     * @return la fiche de paie trouvée, null sinon
     * @throws SQLException exception SQL
     */
    public FichePaye getById(int id) throws SQLException {
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE f.id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User employe = new User(
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
                            rs.getBoolean("actif")
                    );
                    return new FichePaye(
                            rs.getInt("id_paye"),
                            rs.getDate("date").toLocalDate().atStartOfDay(),
                            rs.getString("url"),
                            employe
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer toutes les fiches de paie d'un employé
     * @param employeId l'ID de l'employé
     * @return la liste des fiches de paie de l'employé
     * @throws SQLException exception SQL
     */
    public List<FichePaye> findByEmployeId(int employeId) throws SQLException {
        List<FichePaye> fichesPaye = new ArrayList<>();
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE f.id_user = ? " +
                "ORDER BY f.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User employe = new User(
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
                            rs.getBoolean("actif")
                    );
                    fichesPaye.add(new FichePaye(
                            rs.getInt("id_paye"),
                            rs.getDate("date").toLocalDate().atStartOfDay(),
                            rs.getString("url"),
                            employe
                    ));
                }
            }
        }
        return fichesPaye;
    }

    /**
     * Récupérer la fiche de paie d'un employé pour un mois spécifique
     * @param employeId l'ID de l'employé
     * @param date      le mois recherché (LocalDateTime)
     * @return la fiche de paie trouvée, null sinon
     * @throws SQLException exception SQL
     */
    public FichePaye findByEmployeIdAndDate(int employeId, LocalDateTime date) throws SQLException {
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE f.id_user = ? AND YEAR(f.date) = ? AND MONTH(f.date) = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeId);
            stmt.setInt(2, date.getYear());
            stmt.setInt(3, date.getMonthValue());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User employe = new User(
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
                            rs.getBoolean("actif")
                    );
                    return new FichePaye(
                            rs.getInt("id_paye"),
                            rs.getDate("date").toLocalDate().atStartOfDay(),
                            rs.getString("url"),
                            employe
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer toutes les fiches de paie
     * @return la liste de toutes les fiches de paie
     * @throws SQLException exception SQL
     */
    public List<FichePaye> findAll() throws SQLException {
        List<FichePaye> fichesPaye = new ArrayList<>();
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "ORDER BY f.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User employe = new User(
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
                        rs.getBoolean("actif")
                );
                fichesPaye.add(new FichePaye(
                        rs.getInt("id_paye"),
                        rs.getDate("date").toLocalDate().atStartOfDay(),
                        rs.getString("url"),
                        employe
                ));
            }
        }
        return fichesPaye;
    }

    /**
     * Récupérer les fiches de paie d'une année spécifique
     * @param annee l'année recherchée
     * @return la liste des fiches de paie de l'année
     * @throws SQLException exception SQL
     */
    public List<FichePaye> findByAnnee(int annee) throws SQLException {
        List<FichePaye> fichesPaye = new ArrayList<>();
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE YEAR(f.date) = ? " +
                "ORDER BY f.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, annee);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User employe = new User(
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
                            rs.getBoolean("actif")
                    );
                    fichesPaye.add(new FichePaye(
                            rs.getInt("id_paye"),
                            rs.getDate("date").toLocalDate().atStartOfDay(),
                            rs.getString("url"),
                            employe
                    ));
                }
            }
        }
        return fichesPaye;
    }

    /**
     * Mettre à jour l'URL d'une fiche de paie
     * @param id  l'ID de la fiche de paie
     * @param url la nouvelle URL
     * @return true si mis à jour, false sinon
     * @throws SQLException exception SQL
     */
    public boolean updateUrl(int id, String url) throws SQLException {
        String sql = "UPDATE FICHE_PAYE SET url = ? WHERE id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, url);
            stmt.setInt(2, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Compter le nombre total de fiches de paie
     * @return le nombre total de fiches de paie
     * @throws SQLException exception SQL
     */
    public int countFichesPaye() throws SQLException {
        String sql = "SELECT COUNT(*) FROM FICHE_PAYE";

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
     * Compter le nombre de fiches de paie par employé
     * @param employeId l'ID de l'employé
     * @return le nombre de fiches de paie de l'employé
     * @throws SQLException exception SQL
     */
    public int countByEmployeId(int employeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM FICHE_PAYE WHERE id_user = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
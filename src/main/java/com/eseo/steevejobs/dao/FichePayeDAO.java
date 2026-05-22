package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des fiches de paie.
 */
public class FichePayeDAO {

    /**
     * Créer une nouvelle fiche de paie
     */
    public void createFichePaye(FichePaye fichePaye) throws SQLException {
        String sql = "INSERT INTO FICHE_PAYE (mois, url, id_user) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDate(1, java.sql.Date.valueOf(fichePaye.getMois().toLocalDate()));
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
     */
    public void updateFichePaye(FichePaye fichePaye) throws SQLException {
        String sql = "UPDATE FICHE_PAYE SET mois = ?, url = ?, id_user = ? WHERE id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(fichePaye.getMois().toLocalDate()));
            stmt.setString(2, fichePaye.getUrl());
            stmt.setInt(3, fichePaye.getEmploye().getId());
            stmt.setInt(4, fichePaye.getId());

            stmt.executeUpdate();
        }
    }

    /**
     * Supprimer une fiche de paie par son ID
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
     */
    public FichePaye getById(int id) throws SQLException {
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
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
                            rs.getDate("mois").toLocalDate().atStartOfDay(),
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
     */
    public List<FichePaye> findByEmployeId(int employeId) throws SQLException {
        List<FichePaye> fichesPaye = new ArrayList<>();
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE f.id_user = ? " +
                "ORDER BY f.mois DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User employe = new User(
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
                    fichesPaye.add(new FichePaye(
                            rs.getInt("id_paye"),
                            rs.getDate("mois").toLocalDate().atStartOfDay(),
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
     */
    public FichePaye findByEmployeIdAndMois(int employeId, LocalDateTime mois) throws SQLException {
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE f.id_user = ? AND YEAR(mois) = ? AND MONTH(mois) = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeId);
            stmt.setInt(2, mois.getYear());
            stmt.setInt(3, mois.getMonthValue());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User employe = new User(
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
                    return new FichePaye(
                            rs.getInt("id_paye"),
                            rs.getDate("mois").toLocalDate().atStartOfDay(),
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
     */
    public List<FichePaye> findAll() throws SQLException {
        List<FichePaye> fichesPaye = new ArrayList<>();
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "ORDER BY f.mois DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User employe = new User(
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
                fichesPaye.add(new FichePaye(
                        rs.getInt("id_paye"),
                        rs.getDate("mois").toLocalDate().atStartOfDay(),
                        rs.getString("url"),
                        employe
                ));
            }
        }
        return fichesPaye;
    }

    /**
     * Récupérer les fiches de paie d'une année spécifique
     */
    public List<FichePaye> findByAnnee(int annee) throws SQLException {
        List<FichePaye> fichesPaye = new ArrayList<>();
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE YEAR(mois) = ? " +
                "ORDER BY mois DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, annee);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User employe = new User(
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
                    fichesPaye.add(new FichePaye(
                            rs.getInt("id_paye"),
                            rs.getDate("mois").toLocalDate().atStartOfDay(),
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
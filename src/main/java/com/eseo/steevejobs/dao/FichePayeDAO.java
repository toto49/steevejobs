package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des fiches de paie.
 */
public class FichePayeDAO {

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

    public boolean deleteFichePaye(int id) throws SQLException {
        String sql = "DELETE FROM FICHE_PAYE WHERE id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public FichePaye getById(int id) throws SQLException {
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux, u.taux_patronal " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE f.id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User employe = mapUser(rs);
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

    public List<FichePaye> findByEmployeId(int employeId) throws SQLException {
        List<FichePaye> fichesPaye = new ArrayList<>();
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux, u.taux_patronal " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE f.id_user = ? " +
                "ORDER BY f.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    fichesPaye.add(mapFichePaye(rs));
                }
            }
        }
        return fichesPaye;
    }

    /**
     * Récupérer la fiche de paie d'un employé pour un mois spécifique.
     */
    public FichePaye findByEmployeIdAndDate(int employeId, LocalDateTime date) throws SQLException {
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux, u.taux_patronal " +
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
                    return mapFichePaye(rs);
                }
                return null;
            }
        }
    }

    public List<FichePaye> findAll() throws SQLException {
        List<FichePaye> fichesPaye = new ArrayList<>();
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux, u.taux_patronal " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "ORDER BY f.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                fichesPaye.add(mapFichePaye(rs));
            }
        }
        return fichesPaye;
    }

    public List<FichePaye> findByAnnee(int annee) throws SQLException {
        List<FichePaye> fichesPaye = new ArrayList<>();
        String sql = "SELECT f.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, u.taux, u.taux_patronal " +
                "FROM FICHE_PAYE f " +
                "INNER JOIN USER u ON f.id_user = u.id_user " +
                "WHERE YEAR(f.date) = ? " +
                "ORDER BY f.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, annee);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    fichesPaye.add(mapFichePaye(rs));
                }
            }
        }
        return fichesPaye;
    }

    public boolean updateUrl(int id, String url) throws SQLException {
        String sql = "UPDATE FICHE_PAYE SET url = ? WHERE id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, url);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

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

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
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
    }

    private FichePaye mapFichePaye(ResultSet rs) throws SQLException {
        return new FichePaye(
                rs.getInt("id_paye"),
                rs.getDate("date").toLocalDate().atStartOfDay(),
                rs.getString("url"),
                mapUser(rs)
        );
    }
}
package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données de la table {@code FICHE_PAYE}.
 * <p>
 * Les lectures joignent {@code USER} pour hydrater l'employé.
 * Chaque opération s'exécute en auto-commit ; les {@link SQLException} sont propagées.
 * </p>
 */
public class FichePayeDAO {

    /**
     * Insère une fiche de paie et récupère la clé générée.
     * <p>
     * SQL : {@code INSERT INTO FICHE_PAYE} avec {@code RETURN_GENERATED_KEYS}.
     * </p>
     *
     * @param fichePaye fiche à persister ; l'identifiant est mis à jour après insertion
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Met à jour une fiche de paie existante.
     * <p>
     * SQL : {@code UPDATE FICHE_PAYE} filtré par {@code id_paye}.
     * </p>
     *
     * @param fichePaye fiche avec identifiant et champs modifiés
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Supprime une fiche de paie par identifiant.
     *
     * @param id identifiant de la fiche
     * @return {@code true} si au moins une ligne a été supprimée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean deleteFichePaye(int id) throws SQLException {
        String sql = "DELETE FROM FICHE_PAYE WHERE id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Recherche une fiche de paie par identifiant avec employé joint.
     *
     * @param id identifiant de la fiche
     * @return fiche trouvée, ou {@code null}
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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
                    return mapFichePaye(rs);
                }
                return null;
            }
        }
    }

    /**
     * Liste les fiches de paie d'un employé, triées par date décroissante.
     *
     * @param employeId identifiant de l'employé
     * @return liste des fiches (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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
     * Recherche la fiche de paie d'un employé pour un mois donné.
     * <p>
     * SQL : filtre par {@code id_user}, {@code YEAR(date)} et {@code MONTH(date)}.
     * </p>
     *
     * @param employeId identifiant de l'employé
     * @param date      date de référence (seuls le mois et l'année sont utilisés)
     * @return fiche du mois si elle existe, ou {@code null}
     * @throws SQLException en cas d'erreur d'accès à la base
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

    /**
     * Liste toutes les fiches de paie, triées par date décroissante.
     *
     * @return liste complète (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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

    /**
     * Liste les fiches de paie d'une année calendaire.
     *
     * @param annee année recherchée
     * @return liste des fiches de l'année (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
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

    /**
     * Met à jour uniquement l'URL du fichier PDF d'une fiche de paie.
     *
     * @param id  identifiant de la fiche
     * @param url nouvelle URL
     * @return {@code true} si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean updateUrl(int id, String url) throws SQLException {
        String sql = "UPDATE FICHE_PAYE SET url = ? WHERE id_paye = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, url);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Compte le nombre total de fiches de paie enregistrées.
     *
     * @return nombre de fiches ({@code 0} si la table est vide)
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Compte le nombre de fiches de paie d'un employé.
     *
     * @param employeId identifiant de l'employé
     * @return nombre de fiches ({@code 0} si aucune)
     * @throws SQLException en cas d'erreur d'accès à la base
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

    /**
     * Construit un employé à partir des colonnes {@code USER} du résultat joint.
     *
     * @param rs curseur positionné sur la ligne courante
     * @return employé hydraté (taux salarial et patronal inclus)
     * @throws SQLException en cas d'erreur de lecture JDBC
     */
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

    /**
     * Construit une fiche de paie à partir d'une ligne de résultat jointe.
     *
     * @param rs curseur positionné sur la ligne courante
     * @return fiche de paie avec employé associé
     * @throws SQLException en cas d'erreur de lecture JDBC
     */
    private FichePaye mapFichePaye(ResultSet rs) throws SQLException {
        return new FichePaye(
                rs.getInt("id_paye"),
                rs.getDate("date").toLocalDate().atStartOfDay(),
                rs.getString("url"),
                mapUser(rs)
        );
    }
}

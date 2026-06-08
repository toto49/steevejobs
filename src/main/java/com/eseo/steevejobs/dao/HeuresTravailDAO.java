package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.HeuresTravail;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Accès aux données de la table {@code HEURES_TRAVAIL}.
 * <p>
 * Les lectures joignent {@code USER} pour hydrater l'employé.
 * L'enregistrement utilise {@code INSERT ... ON DUPLICATE KEY UPDATE} (clé unique employé/date).
 * Chaque opération s'exécute en auto-commit ; les {@link SQLException} sont propagées.
 * </p>
 */
public class HeuresTravailDAO {

    /**
     * Enregistre ou met à jour les heures de travail d'un employé pour une date donnée.
     * <p>
     * SQL : upsert sur {@code (id_user, date_jour)} ; les horaires {@code null} sont stockés en SQL NULL.
     * </p>
     *
     * @param idUser  identifiant de l'employé
     * @param dateJour date du relevé
     * @param debutM   heure de début matin (nullable)
     * @param finM     heure de fin matin (nullable)
     * @param debutA   heure de début après-midi (nullable)
     * @param finA     heure de fin après-midi (nullable)
     * @param total    total d'heures calculé (nullable)
     * @return {@code true} si l'upsert a affecté au moins une ligne
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean sauvegarder(int idUser, LocalDate dateJour, LocalTime debutM, LocalTime finM,
                               LocalTime debutA, LocalTime finA, LocalTime total) throws SQLException {
        String sql = "INSERT INTO HEURES_TRAVAIL (id_user, date_jour, heure_debut_matin, heure_fin_matin, " +
                "heure_debut_aprem, heure_fin_aprem, heures_total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE heure_debut_matin = VALUES(heure_debut_matin), " +
                "heure_fin_matin = VALUES(heure_fin_matin), " +
                "heure_debut_aprem = VALUES(heure_debut_aprem), " +
                "heure_fin_aprem = VALUES(heure_fin_aprem), " +
                "heures_total = VALUES(heures_total)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUser);
            stmt.setDate(2, Date.valueOf(dateJour));
            setTimeOrNull(stmt, 3, debutM);
            setTimeOrNull(stmt, 4, finM);
            setTimeOrNull(stmt, 5, debutA);
            setTimeOrNull(stmt, 6, finA);
            setTimeOrNull(stmt, 7, total);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Récupère les heures enregistrées pour un employé et une date.
     * <p>
     * SQL : {@code SELECT} sur {@code HEURES_TRAVAIL} avec jointure {@code USER},
     * filtré par {@code id_user} et {@code date_jour}.
     * </p>
     *
     * @param idUser   identifiant de l'employé
     * @param dateJour date recherchée
     * @return relevé trouvé, ou {@code null} si aucune saisie pour cette date
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public HeuresTravail getHeuresParDate(int idUser, LocalDate dateJour) throws SQLException {
        String sql = "SELECT h.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM HEURES_TRAVAIL h " +
                "INNER JOIN USER u ON h.id_user = u.id_user " +
                "WHERE h.id_user = ? AND h.date_jour = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUser);
            stmt.setDate(2, Date.valueOf(dateJour));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = extractUser(rs);
                    return new HeuresTravail(
                            rs.getInt("id_heures"),
                            rs.getDate("date_jour").toLocalDate(),
                            getLocalTime(rs, "heure_debut_matin"),
                            getLocalTime(rs, "heure_fin_matin"),
                            getLocalTime(rs, "heure_debut_aprem"),
                            getLocalTime(rs, "heure_fin_aprem"),
                            getLocalTime(rs, "heures_total"),
                            user
                    );
                }
            }
        }
        return null;
    }

    /**
     * Affecte une heure SQL ou {@code NULL} sur un paramètre préparé.
     *
     * @param stmt  requête préparée cible
     * @param index index du paramètre (base 1)
     * @param time  heure à persister, ou {@code null} pour SQL NULL
     * @throws SQLException en cas d'erreur d'affectation JDBC
     */
    private void setTimeOrNull(PreparedStatement stmt, int index, LocalTime time) throws SQLException {
        if (time != null) {
            stmt.setTime(index, Time.valueOf(time));
        } else {
            stmt.setNull(index, Types.TIME);
        }
    }

    /**
     * Lit une colonne TIME et la convertit en {@link LocalTime}.
     *
     * @param rs         curseur de résultat
     * @param columnName nom de la colonne TIME
     * @return heure locale, ou {@code null} si la valeur SQL est NULL
     * @throws SQLException en cas d'erreur de lecture JDBC
     */
    private LocalTime getLocalTime(ResultSet rs, String columnName) throws SQLException {
        Time time = rs.getTime(columnName);
        return time != null ? time.toLocalTime() : null;
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
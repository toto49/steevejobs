package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.HeuresTravail;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class HeuresTravailDAO {

    public boolean sauvegarder(int idUser, LocalDate dateJour, LocalTime debutM, LocalTime finM, LocalTime debutA, LocalTime finA, LocalTime total) throws SQLException {
        // Ajout de heures_total dans l'INSERT et le UPDATE
        String sql = "INSERT INTO HEURES_TRAVAIL (id_user, date_jour, heure_debut_matin, heure_fin_matin, heure_debut_aprem, heure_fin_aprem, heures_total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE heure_debut_matin = VALUES(heure_debut_matin), heure_fin_matin = VALUES(heure_fin_matin), " +
                "heure_debut_aprem = VALUES(heure_debut_aprem), heure_fin_aprem = VALUES(heure_fin_aprem), heures_total = VALUES(heures_total)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUser);
            stmt.setDate(2, Date.valueOf(dateJour));
            setTimeOrNull(stmt, 3, debutM);
            setTimeOrNull(stmt, 4, finM);
            setTimeOrNull(stmt, 5, debutA);
            setTimeOrNull(stmt, 6, finA);
            setTimeOrNull(stmt, 7, total); // Enregistrement du total

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

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
                            user
                    );
                }
            }
        }
        return null;
    }

    private void setTimeOrNull(PreparedStatement stmt, int index, LocalTime time) throws SQLException {
        if (time != null) {
            stmt.setTime(index, Time.valueOf(time));
        } else {
            stmt.setNull(index, Types.TIME);
        }
    }

    private LocalTime getLocalTime(ResultSet rs, String columnName) throws SQLException {
        Time time = rs.getTime(columnName);
        return time != null ? time.toLocalTime() : null;
    }

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
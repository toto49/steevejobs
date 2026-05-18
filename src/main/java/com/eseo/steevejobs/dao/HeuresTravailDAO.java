package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.HeuresTravail;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class HeuresTravailDAO {

    public void sauvegarder(int idUser, LocalDate dateJour, int heuresMatin, int heuresAprem) throws SQLException {
        String query = "INSERT INTO HEURES_TRAVAIL (id_user, date_jour, heures_matin, heures_aprem) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE heures_matin = VALUES(heures_matin), heures_aprem = VALUES(heures_aprem)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, idUser);
            stmt.setDate(2, java.sql.Date.valueOf(dateJour));
            stmt.setInt(3, heuresMatin);
            stmt.setInt(4, heuresAprem);

            stmt.executeUpdate();
        }
    }

    public HeuresTravail getHeuresParDate(int idUser, LocalDate dateJour) throws SQLException {
        String query = "SELECT * FROM HEURES_TRAVAIL WHERE id_user = ? AND date_jour = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, idUser);
            stmt.setDate(2, java.sql.Date.valueOf(dateJour));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new HeuresTravail(
                        rs.getInt("id_heures"),
                        rs.getInt("id_user"),
                        rs.getDate("date_jour").toLocalDate(),
                        rs.getInt("heures_matin"),
                        rs.getInt("heures_aprem")
                );
            }
        }
        return null; // Renvoie null si aucune heure n'a été saisie pour ce jour
    }
}
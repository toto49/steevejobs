package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.DatabaseConnection;
import com.eseo.steevejobs.dao.HeuresTravailDAO;
import com.eseo.steevejobs.model.HeuresTravail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class HeuresTravailService {

    private HeuresTravailDAO heuresTravailDAO;

    public HeuresTravailService() {
        this.heuresTravailDAO = new HeuresTravailDAO();
    }

    public void sauvegarderHeures(int idUser, LocalDate dateJour, int heuresMatin, int heuresAprem) throws SQLException {
        heuresTravailDAO.sauvegarder(idUser, dateJour, heuresMatin, heuresAprem);
    }

    public HeuresTravail getHeuresParDate(int idUser, LocalDate dateJour) throws SQLException {
        return heuresTravailDAO.getHeuresParDate(idUser, dateJour);
    }
    public double getTotalHeuresByMonth(int userId, int annee, int mois) throws SQLException {
        double total = 0;
        String sql = "SELECT SUM(TIME_TO_SEC(heures_total)) / 3600 as total_heures " +
                "FROM HEURES_TRAVAIL " +
                "WHERE id_user = ? AND YEAR(date_jour) = ? AND MONTH(date_jour) = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, annee);
            stmt.setInt(3, mois);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total_heures");
            }
        }
        return total;
    }
    }

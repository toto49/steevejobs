package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.HeuresTravailDAO;
import com.eseo.steevejobs.model.HeuresTravail;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public class HeuresTravailService {

    private HeuresTravailDAO heuresTravailDAO;

    public HeuresTravailService() {
        this.heuresTravailDAO = new HeuresTravailDAO();
    }

    public boolean sauvegarderHeures(int idUser, LocalDate dateJour, LocalTime debutM, LocalTime finM, LocalTime debutA, LocalTime finA, LocalTime tTotal) throws SQLException {
        return heuresTravailDAO.sauvegarder(idUser, dateJour, debutM, finM, debutA, finA, tTotal);
    }

    public HeuresTravail getHeuresParDate(int idUser, LocalDate dateJour) throws SQLException {
        return heuresTravailDAO.getHeuresParDate(idUser, dateJour);
    }
}
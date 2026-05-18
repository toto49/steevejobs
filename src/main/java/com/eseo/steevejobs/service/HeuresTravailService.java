package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.HeuresTravailDAO;
import com.eseo.steevejobs.model.HeuresTravail;
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
}
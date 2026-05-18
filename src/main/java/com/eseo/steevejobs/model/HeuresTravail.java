package com.eseo.steevejobs.model;

import java.time.LocalDate;

public class HeuresTravail {
    private int idHeures;
    private int idUser;
    private LocalDate dateJour;
    private int heuresMatin;
    private int heuresAprem;

    public HeuresTravail(int idHeures, int idUser, LocalDate dateJour, int heuresMatin, int heuresAprem) {
        this.idHeures = idHeures;
        this.idUser = idUser;
        this.dateJour = dateJour;
        this.heuresMatin = heuresMatin;
        this.heuresAprem = heuresAprem;
    }

    // Getters
    public int getIdHeures() { return idHeures; }
    public int getIdUser() { return idUser; }
    public LocalDate getDateJour() { return dateJour; }
    public int getHeuresMatin() { return heuresMatin; }
    public int getHeuresAprem() { return heuresAprem; }

    // Setters
    public void setIdHeures(int idHeures) { this.idHeures = idHeures; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public void setDateJour(LocalDate dateJour) { this.dateJour = dateJour; }
    public void setHeuresMatin(int heuresMatin) { this.heuresMatin = heuresMatin; }
    public void setHeuresAprem(int heuresAprem) { this.heuresAprem = heuresAprem; }
}
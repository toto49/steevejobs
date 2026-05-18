package com.eseo.steevejobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Représente les créneaux d'heures de travail effectués par un utilisateur pour une journée.
 */
public class HeuresTravail {

    private int idHeures;
    private LocalDate dateJour;
    private LocalTime debutMatin;
    private LocalTime finMatin;
    private LocalTime debutAprem;
    private LocalTime finAprem;
    private User user;

    public HeuresTravail(int idHeures, LocalDate dateJour, LocalTime debutMatin, LocalTime finMatin, LocalTime debutAprem, LocalTime finAprem, User user) {
        this.idHeures = idHeures;
        this.dateJour = dateJour;
        this.debutMatin = debutMatin;
        this.finMatin = finMatin;
        this.debutAprem = debutAprem;
        this.finAprem = finAprem;
        this.user = user;
    }

    public int getIdHeures() {
        return idHeures;
    }

    public void setIdHeures(int idHeures) {
        this.idHeures = idHeures;
    }

    public LocalDate getDateJour() {
        return dateJour;
    }

    public void setDateJour(LocalDate dateJour) {
        this.dateJour = dateJour;
    }

    public LocalTime getDebutMatin() {
        return debutMatin;
    }

    public void setDebutMatin(LocalTime debutMatin) {
        this.debutMatin = debutMatin;
    }

    public LocalTime getFinMatin() {
        return finMatin;
    }

    public void setFinMatin(LocalTime finMatin) {
        this.finMatin = finMatin;
    }

    public LocalTime getDebutAprem() {
        return debutAprem;
    }

    public void setDebutAprem(LocalTime debutAprem) {
        this.debutAprem = debutAprem;
    }

    public LocalTime getFinAprem() {
        return finAprem;
    }

    public void setFinAprem(LocalTime finAprem) {
        this.finAprem = finAprem;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
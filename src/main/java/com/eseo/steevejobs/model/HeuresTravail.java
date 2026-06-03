package com.eseo.steevejobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Saisie des heures de travail d'un employé pour une journée (matin et après-midi).
 * Entité ou ligne persistée liée à {@code User} ; affichée dans les écrans de pointage RH.
 */
public class HeuresTravail {

    private int idHeures;
    private LocalDate dateJour;
    private LocalTime debutMatin;
    private LocalTime finMatin;
    private LocalTime debutAprem;
    private LocalTime finAprem;
    private LocalTime heurestotal;
    private User user;

    /**
     * Construit un enregistrement d'heures pour une journée.
     *
     * @param idHeures    identifiant technique
     * @param dateJour    date calendaire concernée
     * @param debutMatin  début créneau matin
     * @param finMatin    fin créneau matin
     * @param debutAprem  début créneau après-midi
     * @param finAprem    fin créneau après-midi
     * @param heurestotal durée totale calculée
     * @param user        employé concerné
     */
    public HeuresTravail(int idHeures, LocalDate dateJour, LocalTime debutMatin, LocalTime finMatin, LocalTime debutAprem, LocalTime finAprem, LocalTime heurestotal, User user) {
        this.idHeures = idHeures;
        this.dateJour = dateJour;
        this.debutMatin = debutMatin;
        this.finMatin = finMatin;
        this.debutAprem = debutAprem;
        this.finAprem = finAprem;
        this.heurestotal = heurestotal;
        this.user = user;
    }

    /** @return identifiant de la saisie */
    public int getIdHeures() {
        return idHeures;
    }

    /** @param idHeures identifiant de la saisie */
    public void setIdHeures(int idHeures) {
        this.idHeures = idHeures;
    }

    /** @return date de la journée travaillée */
    public LocalDate getDateJour() {
        return dateJour;
    }

    /** @param dateJour date de la journée travaillée */
    public void setDateJour(LocalDate dateJour) {
        this.dateJour = dateJour;
    }

    /** @return heure de début du matin */
    public LocalTime getDebutMatin() {
        return debutMatin;
    }

    /** @param debutMatin heure de début du matin */
    public void setDebutMatin(LocalTime debutMatin) {
        this.debutMatin = debutMatin;
    }

    /** @return heure de fin du matin */
    public LocalTime getFinMatin() {
        return finMatin;
    }

    /** @param finMatin heure de fin du matin */
    public void setFinMatin(LocalTime finMatin) {
        this.finMatin = finMatin;
    }

    /** @return heure de début de l'après-midi */
    public LocalTime getDebutAprem() {
        return debutAprem;
    }

    /** @param debutAprem heure de début de l'après-midi */
    public void setDebutAprem(LocalTime debutAprem) {
        this.debutAprem = debutAprem;
    }

    /** @return heure de fin de l'après-midi */
    public LocalTime getFinAprem() {
        return finAprem;
    }

    /** @param finAprem heure de fin de l'après-midi */
    public void setFinAprem(LocalTime finAprem) {
        this.finAprem = finAprem;
    }

    /** @return employé associé */
    public User getUser() {
        return user;
    }

    /** @param user employé associé */
    public void setUser(User user) {
        this.user = user;
    }

    /** @return durée totale de la journée */
    public LocalTime getHeurestotal() {
        return heurestotal;
    }

    /** @param heurestotal durée totale de la journée */
    public void setHeurestotal(LocalTime heurestotal) {this.heurestotal = heurestotal;}
}

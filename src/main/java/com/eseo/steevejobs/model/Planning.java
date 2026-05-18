package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

/**
 * Représente un élément de planning pour un utilisateur.
 * Un planning contient :
 * - une date/heure de début
 * - une date/heure de fin
 * - un type (ex : "travail", "congé", "maladie", etc.)
 * - une description
 * - un utilisateur associé
 */

public class Planning {

    /** Identifiant unique du planning. */
    private int id;
    /** Date et heure de début du créneau de travail. */
    private LocalDateTime jourDebut;
    /** Date et heure de fin du créneau de travail. */
    private LocalDateTime jourFin;
    /** Type de planning (ex : "présence", "télétravail", "absence"). */
    private String type;
    /** Description ou détails supplémentaires sur l'événement. */
    private String description;
    /** Utilisateur auquel ce planning est associé. */
    private User user;


    /**
     * Constructeur permettant d'initialiser un planning complet.
     *
     * @param id            identifiant unique
     * @param jourDebut     date/heure de début
     * @param jourFin       date/heure de fin
     * @param type          type de planning
     * @param description   description de l'événement
     * @param user          utilisateur concerné
     */

    public Planning(int id, LocalDateTime jourDebut, LocalDateTime jourFin, String type, String description, User user) {
        this.id = id;
        this.jourDebut = jourDebut;
        this.jourFin = jourFin;
        this.type = type;
        this.description = description;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getJourDebut() {
        return jourDebut;
    }

    public void setJourDebut(LocalDateTime jourDebut) {
        this.jourDebut = jourDebut;
    }

    public LocalDateTime getJourFin() {
        return jourFin;
    }

    public void setJourFin(LocalDateTime jourFin) {
        this.jourFin = jourFin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
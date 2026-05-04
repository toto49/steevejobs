package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

// Commentaire générés par IA

/**
 * Représente un élément de planning pour un utilisateur.
 * Un planning contient :
 * - une date/heure de début
 * - une date/heure de fin
 * - un type (ex : "travail", "congé", "maladie", etc.)
 * - un utilisateur associé
 */

public class Planning {

    /** Identifiant unique du planning. */
    private int id;

    /** Date et heure de début du créneau de travail. */
    private LocalDateTime jour_debut;

    /** Date et heure de fin du créneau de travail. */
    private LocalDateTime jour_fin;

    /** Type de planning (ex : "présence", "télétravail", "absence"). */
    private String type;

    /** Utilisateur auquel ce planning est associé. */
    private User user;

    /**
     * Constructeur permettant d'initialiser un planning complet.
     *
     * @param id            identifiant unique
     * @param jour_debut    date/heure de début
     * @param jour_fin      date/heure de fin
     * @param type          type de planning
     * @param user          utilisateur concerné
     */

    public Planning(int id, LocalDateTime jour_debut, LocalDateTime jour_fin, String type, User user) {
        this.id = id;
        this.jour_debut = jour_debut;
        this.jour_fin = jour_fin;
        this.type = type;
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
    public LocalDateTime getJour_debut() {
        return jour_debut;
    }
    public void setJour_debut(LocalDateTime jour_debut) {
        this.jour_debut = jour_debut;
    }
    public LocalDateTime getJour_fin() {
        return jour_fin;
    }
    public void setJour_fin(LocalDateTime jour_fin) {
        this.jour_fin = jour_fin;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}

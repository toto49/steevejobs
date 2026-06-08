package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

/**
 * Créneau de planning RH (congé, réunion, astreinte, etc.) rattaché à un employé.
 * Persisté et affiché dans le calendrier RH ; lié à {@code User} et éventuellement aux demandes de congé.
 */
public class Planning {

    private int id;
    private LocalDateTime jourDebut;
    private LocalDateTime jourFin;
    private String type;
    private String description;
    /** Code couleur d'affichage dans le calendrier (hex ou nom). */
    private String couleur;
    private User user;

    /**
     * Construit un créneau de planning complet.
     *
     * @param id          identifiant technique
     * @param jourDebut   début du créneau
     * @param jourFin     fin du créneau
     * @param type        catégorie métier (libellé)
     * @param description libellé détaillé
     * @param couleur     couleur d'affichage calendrier
     * @param user        employé concerné
     */
    public Planning(int id, LocalDateTime jourDebut, LocalDateTime jourFin, String type, String description, String couleur, User user) {
        this.id = id;
        this.jourDebut = jourDebut;
        this.jourFin = jourFin;
        this.type = type;
        this.description = description;
        this.couleur = couleur;
        this.user = user;
    }

    /** Constructeur vide pour persistance ou formulaire. */
    public Planning() {

    }

    /** @return employé associé au créneau */
    public User getUser() { return user; }
    /** @param user employé associé au créneau */
    public void setUser(User user) { this.user = user; }

    /** @return identifiant technique */
    public int getId() { return id; }
    /** @param id identifiant technique */
    public void setId(int id) { this.id = id; }

    /** @return début du créneau */
    public LocalDateTime getJourDebut() { return jourDebut; }
    /** @param jourDebut début du créneau */
    public void setJourDebut(LocalDateTime jourDebut) { this.jourDebut = jourDebut; }

    /** @return fin du créneau */
    public LocalDateTime getJourFin() { return jourFin; }
    /** @param jourFin fin du créneau */
    public void setJourFin(LocalDateTime jourFin) { this.jourFin = jourFin; }

    /** @return type ou catégorie du créneau */
    public String getType() { return type; }
    /** @param type type ou catégorie du créneau */
    public void setType(String type) { this.type = type; }

    /** @return description affichée */
    public String getDescription() { return description; }
    /** @param description description affichée */
    public void setDescription(String description) { this.description = description; }

    /** @return couleur d'affichage calendrier */
    public String getCouleur() { return couleur; }
    /** @param couleur couleur d'affichage calendrier */
    public void setCouleur(String couleur) { this.couleur = couleur; }
}

package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

/**
 * Représente un élément de planning pour un utilisateur.
 */
public class Planning {

    private int id;
    private LocalDateTime jourDebut;
    private LocalDateTime jourFin;
    private String type;
    private String description;
    private String couleur; // NOUVEL ATTRIBUT
    private User user;

    public Planning(int id, LocalDateTime jourDebut, LocalDateTime jourFin, String type, String description, String couleur, User user) {
        this.id = id;
        this.jourDebut = jourDebut;
        this.jourFin = jourFin;
        this.type = type;
        this.description = description;
        this.couleur = couleur;
        this.user = user;
    }

    public Planning() {

    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getJourDebut() { return jourDebut; }
    public void setJourDebut(LocalDateTime jourDebut) { this.jourDebut = jourDebut; }

    public LocalDateTime getJourFin() { return jourFin; }
    public void setJourFin(LocalDateTime jourFin) { this.jourFin = jourFin; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }
}
package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

//Commentaires géré par IA

/**
 * Représente une fiche de paie associée à un employé de l'entreprise.
 *
 * Une fiche de paie correspond à un mois donné et référence
 * un document (généralement un PDF) stocké via une URL.
 * Elle est toujours rattachée à un utilisateur (employé).
 */

public class FichePaye {

    /** Identifiant unique de la fiche de paie */
    private int id;

    /** date concerné par la paie (ex : janvier 2026) */
    private LocalDateTime date;

    /** URL ou chemin du fichier de la fiche de paie */
    private String url;

    /** Employé auquel cette fiche de paie est associée */
    private User employe;


    /**
     * Constructeur complet permettant d'initialiser
     * toutes les propriétés d'une fiche de paie.
     *
     * @param id       identifiant unique
     * @param date     date correspondant à la paie
     * @param url      lien vers le fichier de la fiche de paie
     * @param employe  employé concerné par la fiche
     */

    public FichePaye(int id, LocalDateTime date, String url, User employe) {
        this.id = id;
        this.date = date;
        this.url = url;
        this.employe = employe;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }
    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public User getEmploye() {
        return employe;
    }
    public void setEmploye(User employe) {
        this.employe = employe;
    }

}

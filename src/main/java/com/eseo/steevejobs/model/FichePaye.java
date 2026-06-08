package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

/**
 * Fiche de paie mensuelle d'un employé.
 * Référence un fichier (URL) et une période ; persistée et consultée dans le module RH paie.
 */
public class FichePaye {

    /** Identifiant unique de la fiche de paie */
    private int id;

    /** Période concernée par la paie (ex. mois de référence) */
    private LocalDateTime date;

    /** URL ou chemin du fichier de la fiche de paie */
    private String url;

    /** Employé auquel cette fiche de paie est associée */
    private User employe;

    /**
     * Construit une fiche de paie complète.
     *
     * @param id       identifiant unique
     * @param date     période correspondant à la paie
     * @param url      lien vers le fichier PDF ou équivalent
     * @param employe  employé concerné par la fiche
     */
    public FichePaye(int id, LocalDateTime date, String url, User employe) {
        this.id = id;
        this.date = date;
        this.url = url;
        this.employe = employe;
    }

    /** @return identifiant technique */
    public int getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return période de paie */
    public LocalDateTime getDate() {
        return date;
    }

    /** @param date période de paie */
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    /** @return emplacement du document */
    public String getUrl() {
        return url;
    }

    /** @param url emplacement du document */
    public void setUrl(String url) {
        this.url = url;
    }

    /** @return employé titulaire */
    public User getEmploye() {
        return employe;
    }

    /** @param employe employé titulaire */
    public void setEmploye(User employe) {
        this.employe = employe;
    }

}

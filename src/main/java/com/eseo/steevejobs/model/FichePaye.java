package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

public class FichePaye {

    /***/
    private int id;

    /***/
    private LocalDateTime mois;

    /***/
    private String url;

    private User employe;

    /**
     * Constructeur complet permettant d'initialiser toutes les propriétés de la Fiche de paye
     * @param id        identifiant unique
     * @param mois      Mois correspondant à la paie
     * @param url       Lien vers le fichier
     * */
    public FichePaye(int id, LocalDateTime mois, String url, User employe) {
        this.id = id;
        this.mois = mois;
        this.url = url;
        this.employe = employe;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public LocalDateTime getMois() {
        return mois;
    }
    public void setMois(LocalDateTime mois) {
        this.mois = mois;
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

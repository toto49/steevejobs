package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

public class FichePaye {

    /***/
    private int id;

    /***/
    private LocalDateTime mois;

    /***/
    private String url;
    // TODO rajouter une liste de fiches de paye

    /**
     * Constructeur complet permettant d'initialiser toutes les propriétés de la Fiche de paye
     * @param id        identifiant unique
     * @param mois      Mois correspondant à la paie
     * @param url       Emplacement du fichier
     * */
    public FichePaye(int id, LocalDateTime mois, String url) {
        this.id = id;
        this.mois = mois;
        this.url = url;
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

}

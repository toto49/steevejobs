package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

public class FichePaye {
    private int id;
    private LocalDateTime mois;
    private String url;
    // TODO rajouter une liste de fiches de paye

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

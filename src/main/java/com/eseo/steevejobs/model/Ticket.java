package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

public class Ticket {
    private int id;
    private String service;
    private Enum statut;
    private LocalDateTime date_ouverture;
    // TODO rajouter id auteur

    public Ticket(int id, String service, Enum statut, LocalDateTime date_ouverture) {
        this.id = id;
        this.service = service;
        this.statut = statut;
        this.date_ouverture = date_ouverture;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getService() {
        return service;
    }
    public void setService(String service) {
        this.service = service;
    }
    public Enum getStatut() {
        return statut;
    }
    public void setStatut(Enum statut) {
        this.statut = statut;
    }
    public LocalDateTime getDate_ouverture() {
        return date_ouverture;
    }
    public void setDate_ouverture(LocalDateTime date_ouverture) {
        this.date_ouverture = date_ouverture;
    }
}

package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.ReunionType;
import com.eseo.steevejobs.model.Enum.VisioStatut;

import java.time.LocalDateTime;

public class Visio {
    private int id;
    private String room_name;
    private int createur_id;
    private VisioStatut statut;
    private LocalDateTime heure_programmee;
    private LocalDateTime heure_debut;
    private LocalDateTime heure_fin;
    private ReunionType type_reunion;

    public Visio() {
    }

    public Visio(int id, String room_name, int createur_id, VisioStatut statut, ReunionType type_reunion, LocalDateTime heure_debut, LocalDateTime heure_programmee, LocalDateTime heure_fin) {
        this.id = id;
        this.room_name = room_name;
        this.createur_id = createur_id;
        this.statut = statut;
        this.type_reunion = type_reunion;
        this.heure_debut = heure_debut;
        this.heure_programmee = heure_programmee;
        this.heure_fin = heure_fin;
    }

    public Visio(String room_name, int createur_id, LocalDateTime heure_programmee) {
        this.room_name = room_name;
        this.createur_id = createur_id;
        this.heure_programmee = heure_programmee;
        this.statut = VisioStatut.PROGRAMMEE;
        this.type_reunion = ReunionType.PLANIFIEE;
    }

    public String getRoom_name() {
        return room_name;
    }

    public void setRoom_name(String room_name) {
        this.room_name = room_name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCreateur_id() {
        return createur_id;
    }

    public void setCreateur_id(int createur_id) {
        this.createur_id = createur_id;
    }

    public VisioStatut getStatut() {
        return statut;
    }

    public void setStatut(VisioStatut statut) {
        this.statut = statut;
    }

    public LocalDateTime getHeure_programmee() {
        return heure_programmee;
    }

    public void setHeure_programmee(LocalDateTime heure_programmee) {
        this.heure_programmee = heure_programmee;
    }

    public LocalDateTime getHeure_fin() {
        return heure_fin;
    }

    public void setHeure_fin(LocalDateTime heure_fin) {
        this.heure_fin = heure_fin;
    }

    public LocalDateTime getHeure_debut() {
        return heure_debut;
    }

    public void setHeure_debut(LocalDateTime heure_debut) {
        this.heure_debut = heure_debut;
    }
}

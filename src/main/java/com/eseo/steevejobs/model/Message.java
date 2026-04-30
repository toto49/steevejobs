package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private String contenu;
    private String piece_jointe;
    private LocalDateTime date_envoi;
    // TODO rajouter un id auteur
    // TODO rajouter un id ticket

    public Message(int id, String contenu, String piece_jointe, LocalDateTime date_envoi) {
        this.id = id;
        this.contenu = contenu;
        this.piece_jointe = piece_jointe;
        this.date_envoi = date_envoi;
    }

    public int  getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getContenu() {
        return contenu;
    }
    public void setContenu(String contenu) {
        this.contenu = contenu;
    }
    public String getPiece_jointe() {
        return piece_jointe;
    }
    public void setPiece_jointe(String piece_jointe) {
        this.piece_jointe = piece_jointe;
    }
    public LocalDateTime getDate_envoi() {
        return date_envoi;
    }
    public void setDate_envoi(LocalDateTime date_envoi) {
        this.date_envoi = date_envoi;
    }
}

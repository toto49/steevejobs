package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

//Commentaires géré par IA

/**
 * Représente un message appartenant à un ticket de support.
 *
 * Un message est écrit par un utilisateur (auteur) et est
 * obligatoirement rattaché à un ticket. Il peut contenir
 * un texte, une pièce jointe et une date d’envoi.
 */

public class Message {
    /** Identifiant unique du message */
    private int id;

    /** Contenu textuel du message */
    private String contenu;

    /** Pièce jointe associée au message (URL ou chemin de fichier) */
    private String pieceJointe;

    /** Date et heure d’envoi du message */
    private LocalDateTime dateEnvoi;

    /** Utilisateur ayant rédigé le message */
    private User auteur;

    /** Ticket auquel ce message est rattaché */
    private Ticket ticket;

    /**
     * Constructeur complet permettant d'initialiser
     * toutes les propriétés du message.
     *
     * @param id           identifiant du message
     * @param contenu      contenu textuel
     * @param pieceJointe  pièce jointe éventuelle
     * @param dateEnvoi    date d’envoi du message
     * @param auteur       auteur du message
     * @param ticket       ticket associé
     */

    public Message() {
    }
    public Message(int id, String contenu, String pieceJointe, LocalDateTime dateEnvoi, User auteur, Ticket ticket) {
        this.id = id;
        this.contenu = contenu;
        this.pieceJointe = pieceJointe;
        this.dateEnvoi = dateEnvoi;
        this.auteur = auteur;
        this.ticket = ticket;
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
    public String getPieceJointe() {
        return pieceJointe;
    }
    public void setPieceJointe(String pieceJointe) {
        this.pieceJointe = pieceJointe;
    }
    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }
    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }
    public User getAuteur() {
        return auteur;
    }
    public void setAuteur(User auteur) {
        this.auteur = auteur;
    }
    public Ticket getTicket() {
        return ticket;
    }
    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}

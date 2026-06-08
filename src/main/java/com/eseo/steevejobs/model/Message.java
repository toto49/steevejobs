package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

/**
 * Message d'échange au sein d'un ticket de support.
 * Entité persistée liée à {@code Ticket} et {@code User} (auteur) ; affichée dans le fil de discussion UI.
 */
public class Message {
    /** Identifiant unique du message */
    private int id;

    /** Contenu textuel du message */
    private String contenu;

    /** Pièce jointe associée au message (URL ou chemin de fichier) */
    private String pieceJointe;

    /** Date et heure d'envoi du message */
    private LocalDateTime dateEnvoi;

    /** Utilisateur ayant rédigé le message */
    private User auteur;

    /** Ticket auquel ce message est rattaché */
    private Ticket ticket;

    /** Constructeur vide pour mapping ou formulaire. */
    public Message() {
    }

    /**
     * Construit un message avec toutes ses propriétés.
     *
     * @param id           identifiant du message
     * @param contenu      contenu textuel
     * @param pieceJointe  pièce jointe éventuelle
     * @param dateEnvoi    date d'envoi du message
     * @param auteur       auteur du message
     * @param ticket       ticket associé
     */
    public Message(int id, String contenu, String pieceJointe, LocalDateTime dateEnvoi, User auteur, Ticket ticket) {
        this.id = id;
        this.contenu = contenu;
        this.pieceJointe = pieceJointe;
        this.dateEnvoi = dateEnvoi;
        this.auteur = auteur;
        this.ticket = ticket;
    }

    /** @return identifiant technique */
    public int  getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return contenu textuel */
    public String getContenu() {
        return contenu;
    }

    /** @param contenu contenu textuel */
    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    /** @return chemin ou URL de pièce jointe */
    public String getPieceJointe() {
        return pieceJointe;
    }

    /** @param pieceJointe chemin ou URL de pièce jointe */
    public void setPieceJointe(String pieceJointe) {
        this.pieceJointe = pieceJointe;
    }

    /** @return date d'envoi */
    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    /** @param dateEnvoi date d'envoi */
    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    /** @return auteur du message */
    public User getAuteur() {
        return auteur;
    }

    /** @param auteur auteur du message */
    public void setAuteur(User auteur) {
        this.auteur = auteur;
    }

    /** @return ticket parent */
    public Ticket getTicket() {
        return ticket;
    }

    /** @param ticket ticket parent */
    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}

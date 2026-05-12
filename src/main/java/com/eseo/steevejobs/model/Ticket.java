package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.StatutTicket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente un ticket de support ou de demande utilisateur.
 */
public class Ticket {
    /** Identifiant unique du ticket */
    private int id;

    /**
     * Sujet ou titre résumé de la demande
     */
    private String sujet;

    /** Description détaillée du problème */
    private String description;

    /** Service concerné par le ticket (ex : IT, RH, Support) */
    private String service;

    /** Statut actuel du ticket (ouvert, en cours, fermé, etc.) */
    private StatutTicket statut;

    /** Date et heure d’ouverture du ticket */
    private LocalDateTime dateOuverture;

    private LocalDateTime dateDerniereActivite;

    /** Utilisateur ayant créé le ticket */
    private User auteur;

    /** Liste des messages associés à ce ticket */
    private List<Message> messages;

    public Ticket() {
        this.messages = new ArrayList<>();
        this.statut = StatutTicket.EN_ATTENTE;
    }
    /**
     * Constructeur complet permettant d'initialiser un ticket.
     */
    public Ticket(int id, String sujet, String description, String service, StatutTicket statut, LocalDateTime dateOuverture, User auteur) {
        this.id = id;
        this.sujet = sujet;
        this.description = description;
        this.service = service;
        this.statut = statut;
        this.dateOuverture = dateOuverture;
        this.auteur = auteur;
        this.messages = new ArrayList<>();
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public StatutTicket getStatut() {
        return statut;
    }

    public void setStatut(StatutTicket statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(LocalDateTime dateOuverture) {
        this.dateOuverture = dateOuverture;
    }


    public LocalDateTime getDateDerniereActivite() {
        if (this.dateDerniereActivite == null) {
            return this.dateOuverture;
        }
        return this.dateDerniereActivite;
    }

    public void setDateDerniereActivite(LocalDateTime dateDerniereActivite) {
        this.dateDerniereActivite = dateDerniereActivite;
    }


    public User getAuteur() {
        return auteur;
    }

    public void setAuteur(User auteur) {
        this.auteur = auteur;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public void removeMessage(Message message) {
        this.messages.remove(message); }
}
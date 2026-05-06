package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.StatutTicket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//Commentaires géré par IA

/**
 * Représente un ticket de support ou de demande utilisateur.
 *
 * Un ticket est créé par un utilisateur (auteur) pour un service donné
 * (informatique, RH, support client, etc.). Il possède un statut et
 * contient une liste de messages échangés entre les intervenants.
 */

public class Ticket {
    /** Identifiant unique du ticket */
    private int id;

    /** Service concerné par le ticket (ex : IT, RH, Support) */
    private String service;

    /** Statut actuel du ticket (ouvert, en cours, fermé, etc.) */
    private StatutTicket statut;

    /** Date et heure d’ouverture du ticket */
    private LocalDateTime dateOuverture;

    /** Utilisateur ayant créé le ticket */
    private User auteur;

    /** Liste des messages associés à ce ticket */
    private List<Message> messages;

    /**
     * Constructeur complet permettant d'initialiser un ticket.
     *
     * À la création, la liste des messages est initialisée vide.
     *
     * @param id            identifiant du ticket
     * @param service       service concerné
     * @param statut        statut initial du ticket
     * @param dateOuverture date d’ouverture
     * @param auteur        utilisateur créateur du ticket
     */

    public Ticket(int id, String service, StatutTicket statut, LocalDateTime dateOuverture,  User auteur) {
        this.id = id;
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
    public User getAuteur() {
        return auteur;
    }
    public void setAuteur(User auteur) {
        this.auteur = auteur;
    }
    public List<Message> getMessages() {
        return messages;
    }
    public void addMessage(Message message) {
        this.messages.add(message);
    }
    public void removeMessage(Message message) {
        this.messages.remove(message);
    }
}

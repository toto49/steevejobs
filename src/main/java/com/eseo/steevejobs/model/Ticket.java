package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.StatutTicket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ticket de support ou demande interne.
 * Entité persistée avec fil de {@code Message} ; statuts et indicateurs de non-lu alimentent les listes UI support.
 */
public class Ticket {
    /** Identifiant unique du ticket */
    private int id;

    /** Sujet ou titre résumé de la demande */
    private String sujet;

    /** Description détaillée du problème */
    private String description;

    /** Service concerné par le ticket (ex. IT, RH, Support) */
    private String service;

    /** Statut actuel du ticket (ouvert, en cours, fermé, etc.) */
    private StatutTicket statut;

    /** Date et heure d'ouverture du ticket */
    private LocalDateTime dateOuverture;

    /** Dernière activité sur le fil (message ou mise à jour) */
    private LocalDateTime dateDerniereActivite;

    /** Utilisateur ayant créé le ticket */
    private User auteur;

    /** Liste des messages associés à ce ticket */
    private List<Message> messages;

    /** Indique une réponse non lue côté administrateur */
    private boolean nonLuAdmin;

    /** Indique une réponse non lue côté auteur du ticket */
    private boolean nonLuAuteur;

    /** Initialise listes et statut par défaut ({@link StatutTicket#EN_ATTENTE}). */
    public Ticket() {
        this.messages = new ArrayList<>();
        this.statut = StatutTicket.EN_ATTENTE;
    }

    /**
     * Construit un ticket avec ses attributs principaux.
     *
     * @param id             identifiant
     * @param sujet          titre du ticket
     * @param description    description détaillée
     * @param service        service destinataire
     * @param statut         statut initial
     * @param dateOuverture  date d'ouverture
     * @param auteur         créateur du ticket
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

    /** @return identifiant technique */
    public int getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return sujet du ticket */
    public String getSujet() {
        return sujet;
    }

    /** @param sujet sujet du ticket */
    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    /** @return description détaillée */
    public String getDescription() {
        return description;
    }

    /** @param description description détaillée */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return service concerné */
    public String getService() {
        return service;
    }

    /** @param service service concerné */
    public void setService(String service) {
        this.service = service;
    }

    /** @return statut du ticket */
    public StatutTicket getStatut() {
        return statut;
    }

    /** @param statut statut du ticket */
    public void setStatut(StatutTicket statut) {
        this.statut = statut;
    }

    /** @return date d'ouverture */
    public LocalDateTime getDateOuverture() {
        return dateOuverture;
    }

    /** @param dateOuverture date d'ouverture */
    public void setDateOuverture(LocalDateTime dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    /**
     * Retourne la dernière activité, ou la date d'ouverture si aucune activité enregistrée.
     *
     * @return horodatage de dernière activité effective
     */
    public LocalDateTime getDateDerniereActivite() {
        if (this.dateDerniereActivite == null) {
            return this.dateOuverture;
        }
        return this.dateDerniereActivite;
    }

    /** @param dateDerniereActivite horodatage de dernière activité */
    public void setDateDerniereActivite(LocalDateTime dateDerniereActivite) {
        this.dateDerniereActivite = dateDerniereActivite;
    }

    /** @return auteur du ticket */
    public User getAuteur() {
        return auteur;
    }

    /** @param auteur auteur du ticket */
    public void setAuteur(User auteur) {
        this.auteur = auteur;
    }

    /** @return fil de messages */
    public List<Message> getMessages() {
        return messages;
    }

    /** @param messages fil de messages */
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    /** @param message message à ajouter au fil */
    public void addMessage(Message message) {
        this.messages.add(message);
    }

    /** @param message message à retirer du fil */
    public void removeMessage(Message message) {
        this.messages.remove(message); }

    /** @return {@code true} si non lu par l'administration */
    public boolean isNonLuAdmin() {
        return nonLuAdmin;
    }

    /** @param nonLuAdmin indicateur non lu administration */
    public void setNonLuAdmin(boolean nonLuAdmin) {
        this.nonLuAdmin = nonLuAdmin;
    }

    /** @return {@code true} si non lu par l'auteur */
    public boolean isNonLuAuteur() {
        return nonLuAuteur;
    }

    /** @param nonLuAuteur indicateur non lu auteur */
    public void setNonLuAuteur(boolean nonLuAuteur) {
        this.nonLuAuteur = nonLuAuteur;
    }
}

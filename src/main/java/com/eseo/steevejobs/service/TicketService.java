package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrat de gestion des tickets d'assistance internes (ADMIN / RH).
 * <p>
 * Les implémentations gèrent le cycle de statut, les messages et les indicateurs
 * de lecture. Les notifications temps réel sont déclenchées via {@link WebSocketService}
 * côté présentation, pas dans cette interface.
 * </p>
 */
public interface TicketService {

    /**
     * Crée un ticket en statut {@link StatutTicket#EN_ATTENTE}.
     *
     * @param ticket entité à créer (sujet, description, service, auteur)
     * @return ticket persisté avec date d'ouverture
     */
    Ticket creerTicket(Ticket ticket);

    /**
     * Charge un ticket par identifiant.
     *
     * @param id identifiant ticket
     * @return ticket ou {@code null} selon le DAO
     */
    Ticket getTicketById(int id);

    /**
     * Liste tous les tickets triés par date d'ouverture décroissante.
     *
     * @return liste des tickets
     */
    List<Ticket> getAllTickets();

    /**
     * Liste les tickets d'un auteur.
     *
     * @param userId identifiant auteur
     * @return tickets de l'auteur, triés par date décroissante
     */
    List<Ticket> getTicketsByAuteur(int userId);

    /**
     * Ajoute un message à un ticket non fermé ; peut passer le statut à EN_COURS.
     *
     * @param ticketId identifiant du ticket
     * @param message  message à persister
     * @return message enregistré
     */
    Message ajouterMessage(int ticketId, Message message);

    /**
     * Modifie le statut d'un ticket.
     *
     * @param ticketId      identifiant ticket
     * @param nouveauStatut statut cible
     * @return ticket mis à jour
     */
    Ticket changerStatut(int ticketId, StatutTicket nouveauStatut);

    /**
     * Liste les messages d'un ticket triés chronologiquement.
     *
     * @param ticketId identifiant ticket
     * @return fil de discussion
     */
    List<Message> getMessagesDuTicket(int ticketId);

    /**
     * Formate une date d'ouverture pour affichage.
     *
     * @param dateOuverture date-heure ou {@code null}
     * @return chaîne {@code dd/MM/yyyy HH:mm} ou {@code N/A}
     */
    String formatTicketDate(LocalDateTime dateOuverture);

    /**
     * Compte les tickets non lus côté administrateur pour un service donné.
     *
     * @param service       service cible ({@code ADMIN} ou {@code RH})
     * @param idCurrentUser identifiant de l'utilisateur courant (exclusion lecture)
     * @return nombre de tickets non lus ; 0 en cas d'erreur SQL silencieuse
     */
    int getNombreTicketsNonLusAdmin(String service, int idCurrentUser);

    /**
     * Compte les tickets non lus pour un auteur.
     *
     * @param idAuteur identifiant auteur
     * @return nombre non lu ; 0 en cas d'erreur SQL silencieuse
     */
    int getNombreTicketsNonLusAuteur(int idAuteur);

    /**
     * Marque un ticket comme lu (admin ou auteur selon le flag).
     *
     * @param idTicket identifiant ticket
     * @param estAdmin {@code true} pour le marquage côté admin
     */
    void marquerTicketLu(int idTicket, boolean estAdmin);

    /**
     * Marque un ticket comme non lu pour la cible indiquée.
     *
     * @param idTicket    identifiant ticket
     * @param cibleAdmin  {@code true} pour notifier l'administrateur
     */
    void marquerTicketNonLu(int idTicket, boolean cibleAdmin);
}

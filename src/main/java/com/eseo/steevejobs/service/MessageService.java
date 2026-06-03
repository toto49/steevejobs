package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.Message;

import java.util.List;

/**
 * Contrat d'accès aux messages rattachés aux tickets d'assistance.
 * <p>
 * Les implémentations encapsulent les accès DAO ; les erreurs SQL peuvent être
 * converties en {@link RuntimeException} selon l'opération.
 * </p>
 */
public interface MessageService {

    /**
     * Charge un message par identifiant.
     *
     * @param id identifiant du message
     * @return message trouvé, ou {@code null} selon le DAO
     */
    Message getMessage(int id);

    /**
     * Liste les messages rédigés par un auteur.
     *
     * @param auteurId identifiant de l'utilisateur auteur
     * @return liste des messages (éventuellement vide)
     */
    List<Message> getMessagesByAuteur(int auteurId);

    /**
     * Liste les messages d'un ticket, ordre défini par le DAO.
     *
     * @param ticketId identifiant du ticket
     * @return liste des messages du fil de discussion
     */
    List<Message> getMessagesByTicketId(int ticketId);

    /**
     * Persiste un nouveau message (l'identifiant est renseigné par le DAO si applicable).
     *
     * @param message entité message à créer
     * @return message persisté
     */
    Message createMessage(Message message);

    /**
     * Supprime un message.
     *
     * @param id identifiant du message
     * @return {@code true} si la suppression a réussi
     */
    boolean deleteMessage(int id);
}

package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.MessageDAO;
import com.eseo.steevejobs.dao.TicketDAO;
import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class TicketServiceImpl implements TicketService{
    private final TicketDAO ticketDAO = new TicketDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    @Override
    public Ticket creerTicket(Ticket ticket) {
        try {
            ticket.setDateOuverture(LocalDateTime.now());
            ticket.setStatut(StatutTicket.EN_ATTENTE);
            ticketDAO.createTicket(ticket);
            return ticket;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du ticket", e);
        }
    }

    @Override
    public Ticket getTicketById(int id) {
        try {
            return ticketDAO.getById(id);
        }catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du ticket", e);
        }
    }

    @Override
    public List<Ticket> getAllTickets() {
        try {return ticketDAO.findAll();
        }catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tickets", e);
        }
    }

    @Override
    public List<Ticket> getTicketsByAuteur(int userId) {
        try {
            return ticketDAO.findByAuteurId(userId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tickets par auteur", e);
        }
    }


    @Override
    public Message ajouterMessage(int ticketId, Message message) {
        try {
            Ticket ticket = ticketDAO.getById(ticketId);
            if (ticket == null) {
                throw new IllegalArgumentException("Ticket inexistant");
            }

            message.setTicket(ticket);
            message.setDateEnvoi(LocalDateTime.now());
            messageDAO.createMessage(message);


            if (ticket.getStatut() == StatutTicket.EN_ATTENTE) {
                ticketDAO.updateStatut(ticketId, StatutTicket.EN_COURS);
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du message", e);
        }
    }

    @Override
    public Ticket changerStatut(int ticketId, StatutTicket nouveauStatut) {
        try {
            boolean update = ticketDAO.updateStatut(ticketId, nouveauStatut);
            if (!update) {
                throw new IllegalArgumentException("Ticket introuvable");
            }
            return ticketDAO.getById(ticketId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du changement de statut", e);
        }
    }

    @Override
    public List<Message> getMessagesDuTicket(int ticketId) {
        try {
            return messageDAO.findByTicketId(ticketId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des messages", e);
        }
    }
}

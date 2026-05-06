package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;

/** public class TicketServiceImpl implements TicketService {

    private final TicketDAO ticketDAO;
    private final MessageDAO messageDAO;

    public TicketServiceImpl(TicketDAO ticketDAO, MessageDAO messageDAO) {
        this.ticketDAO = ticketDAO;
        this.messageDAO = messageDAO;
    }

    @Override
    public Ticket creerTicket(Ticket ticket) {
        ticket.setDateOuverture(LocalDateTime.now());
        ticket.setStatut(StatutTicket.EN_ATTENTE);
        return ticketDAO.save(ticket);
    }

    @Override
    public Ticket getTicketById(int id) {
        return ticketDAO.findbyID(id)
                .orElseThrow(()-> new RuntimeException("Ticket introuvable"));
    }

    @Override
    public List<Ticket> getAllTickets() {
        return ticketDAO.findAll();
    }

    @Override
    public List<Ticket> getTicketsByAuteur(int userId) {
        return ticketDAO.findByAuteurId(userId);
    }

    @Override
    public Message ajouterMessage(int ticketId, Message message) {
        Ticket ticket = getTicketById(ticketId);

        message.setTicket(ticket);
        message.setDateEnvoi(LocalDateTime.now());

        ticket.addMessage(message);

        messageDAO.save(message);

        ticketDAO.save(ticket);

        return message;
    }

    @Override
    public Ticket changerStatut(int ticketId, StatutTicket nouveauStatut) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setStatut(nouveauStatut);
        return ticketDAO.save(ticket);
    }

    @Override
    public List<Message> getMessagesDuTicket(int ticketId) {
        Ticket ticket = getTicketById(ticketId);
        return ticket.getMessages();
    }
}
 */

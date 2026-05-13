package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketService {

    Ticket creerTicket(Ticket ticket);

    Ticket getTicketById(int id);

    List<Ticket> getAllTickets();

    List<Ticket> getTicketsByAuteur(int userId);

    Message ajouterMessage(int ticketId, Message message);

    Ticket changerStatut(int ticketId, StatutTicket nouveauStatut);

    List<Message> getMessagesDuTicket(int ticketId);

    String formatTicketDate(LocalDateTime dateOuverture);

    int getNombreTicketsNonLusAdmin(String service);

    int getNombreTicketsNonLusAuteur(int idAuteur);

    void marquerTicketLu(int idTicket, boolean estAdmin);

    void marquerTicketNonLu(int idTicket, boolean cibleAdmin);


}

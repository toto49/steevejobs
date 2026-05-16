package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.MessageDAO;
import com.eseo.steevejobs.dao.TicketDAO;
import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TicketServiceImpl implements TicketService {

    private final TicketDAO ticketDAO = new TicketDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du ticket : " + id, e);
        }
    }

    @Override
    public List<Ticket> getAllTickets() {
        try {
            List<Ticket> tickets = ticketDAO.findAll();

            return tickets.stream()
                    .sorted(Comparator.comparing(Ticket::getDateOuverture).reversed())
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tickets", e);
        }
    }

    @Override
    public List<Ticket> getTicketsByAuteur(int userId) {
        try {
            return ticketDAO.findByAuteurId(userId).stream()
                    .sorted(Comparator.comparing(Ticket::getDateOuverture).reversed())
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tickets de l'utilisateur : " + userId, e);
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
                throw new IllegalArgumentException("Impossible de modifier le statut : Ticket introuvable");
            }
            return ticketDAO.getById(ticketId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du changement de statut", e);
        }
    }

    @Override
    public List<Message> getMessagesDuTicket(int ticketId) {
        try {
            return messageDAO.findByTicketId(ticketId).stream()
                    .sorted(Comparator.comparing(Message::getDateEnvoi))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des messages", e);
        }
    }


    public String formatTicketDate(LocalDateTime date) {
        if (date == null) return "N/A";
        return date.format(DATE_FORMATTER);
    }

    public String getDureeOuverture(Ticket ticket) {
        if (ticket.getDateOuverture() == null) return "";

        long jours = ChronoUnit.DAYS.between(ticket.getDateOuverture(), LocalDateTime.now());
        if (jours > 0) {
            return "Ouvert il y a " + jours + " jour(s)";
        }

        long heures = ChronoUnit.HOURS.between(ticket.getDateOuverture(), LocalDateTime.now());
        return "Ouvert il y a " + heures + " heure(s)";
    }


    public int getNombreTicketsNonLusAdmin(String service, int idCurrentUser) {
        try {
            return ticketDAO.countTicketsNonLusAdmin(service, idCurrentUser);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getNombreTicketsNonLusAuteur(int idAuteur) {
        try {
            return ticketDAO.countTicketsNonLusAuteur(idAuteur);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void marquerTicketLu(int idTicket, boolean estAdmin) {
        try {
            ticketDAO.marquerTicketLu(idTicket, estAdmin);
        } catch (SQLException e) {
            System.err.println("Erreur lors du marquage du ticket comme lu : " + e.getMessage());
        }
    }

    public void marquerTicketNonLu(int idTicket, boolean cibleAdmin) {
        try {
            ticketDAO.marquerTicketNonLu(idTicket, cibleAdmin);
        } catch (SQLException e) {
            System.err.println("Erreur lors du marquage du ticket comme non lu : " + e.getMessage());
        }
    }


}
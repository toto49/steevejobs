package com.eseo.steevejobs.service;

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

/**
 * Implémentation du service tickets : validation, persistance et règles de statut.
 * <p>
 * Règles métier : service limité à ADMIN ou RH ; longueurs max sujet/description/message ;
 * pas de message sur ticket FERME ; premier message fait passer EN_ATTENTE → EN_COURS.
 * Aucun WebSocket direct : la couche présentation notifie via {@link WebSocketService}.
 * </p>
 */
public class TicketServiceImpl implements TicketService {

    /** Accès persistance aux tickets. */
    private final TicketDAO     ticketDAO;
    /** Service de gestion des messages rattachés aux tickets. */
    private final MessageService messageService;

    /** Format d'affichage des dates d'ouverture de ticket. */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Longueur maximale autorisée pour le sujet d'un ticket. */
    private static final int SUJET_MAX_LENGTH       = 255;
    /** Longueur maximale autorisée pour la description d'un ticket. */
    private static final int DESCRIPTION_MAX_LENGTH = 5000;
    /** Longueur maximale autorisée pour le contenu d'un message. */
    private static final int CONTENU_MAX_LENGTH     = 5000;

    /**
     * Constructeur par défaut.
     */
    public TicketServiceImpl() {
        this.ticketDAO = new TicketDAO();
        this.messageService = new MessageServiceImpl();
    }

    /**
     * Constructeur avec injection (tests).
     *
     * @param ticketDAO      accès tickets
     * @param messageService service messages
     */
    public TicketServiceImpl(TicketDAO ticketDAO, MessageService messageService) {
        this.ticketDAO = ticketDAO;
        this.messageService = messageService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Ticket creerTicket(Ticket ticket) {
        validerTicket(ticket);

        try {
            ticket.setDateOuverture(LocalDateTime.now());
            ticket.setStatut(StatutTicket.EN_ATTENTE);
            ticketDAO.createTicket(ticket);
            return ticket;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur BDD : Impossible de créer le ticket.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Ticket getTicketById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du ticket est invalide.");
        }
        try {
            return ticketDAO.getById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur BDD : Impossible de récupérer le ticket " + id + ".", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Ticket> getAllTickets() {
        try {
            return ticketDAO.findAll().stream()
                    .sorted(Comparator.comparing(Ticket::getDateOuverture).reversed())
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Erreur BDD : Impossible de récupérer les tickets.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Ticket> getTicketsByAuteur(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur est invalide.");
        }
        try {
            return ticketDAO.findByAuteurId(userId).stream()
                    .sorted(Comparator.comparing(Ticket::getDateOuverture).reversed())
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erreur BDD : Impossible de récupérer les tickets de l'utilisateur " + userId + ".", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message ajouterMessage(int ticketId, Message message) {
        if (ticketId <= 0) {
            throw new IllegalArgumentException("L'ID du ticket est invalide.");
        }
        validerMessage(message);

        try {
            Ticket ticket = ticketDAO.getById(ticketId);
            if (ticket == null) {
                throw new IllegalArgumentException("Le ticket " + ticketId + " est introuvable.");
            }

            if (ticket.getStatut() == StatutTicket.FERME) {
                throw new IllegalStateException(
                        "Impossible d'ajouter un message : le ticket est fermé.");
            }

            message.setTicket(ticket);
            message.setDateEnvoi(LocalDateTime.now());
            messageService.createMessage(message);

            if (ticket.getStatut() == StatutTicket.EN_ATTENTE) {
                ticketDAO.updateStatut(ticketId, StatutTicket.EN_COURS);
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter le message.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Ticket changerStatut(int ticketId, StatutTicket nouveauStatut) {
        if (ticketId <= 0) {
            throw new IllegalArgumentException("L'ID du ticket est invalide.");
        }
        if (nouveauStatut == null) {
            throw new IllegalArgumentException("Le nouveau statut est obligatoire.");
        }

        try {
            boolean updated = ticketDAO.updateStatut(ticketId, nouveauStatut);
            if (!updated) {
                throw new IllegalArgumentException(
                        "Ticket introuvable ou statut inchangé pour l'ID : " + ticketId);
            }
            return ticketDAO.getById(ticketId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur BDD : Impossible de changer le statut.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Message> getMessagesDuTicket(int ticketId) {
        if (ticketId <= 0) {
            throw new IllegalArgumentException("L'ID du ticket est invalide.");
        }
        return messageService.getMessagesByTicketId(ticketId).stream()
                .sorted(Comparator.comparing(Message::getDateEnvoi))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatTicketDate(LocalDateTime date) {
        if (date == null) return "N/A";
        return date.format(DATE_FORMATTER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNombreTicketsNonLusAdmin(String service, int idCurrentUser) {
        validerService(service);
        if (idCurrentUser <= 0) {
            throw new IllegalArgumentException("L'ID de l'utilisateur courant est invalide.");
        }
        try {
            return ticketDAO.countTicketsNonLusAdmin(service, idCurrentUser);
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNombreTicketsNonLusAuteur(int idAuteur) {
        if (idAuteur <= 0) {
            throw new IllegalArgumentException("L'ID de l'auteur est invalide.");
        }
        try {
            return ticketDAO.countTicketsNonLusAuteur(idAuteur);
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void marquerTicketLu(int idTicket, boolean estAdmin) {
        if (idTicket <= 0) {
            throw new IllegalArgumentException("L'ID du ticket est invalide.");
        }
        try {
            ticketDAO.marquerTicketLu(idTicket, estAdmin);
        } catch (SQLException e) {
            System.err.println("Erreur lors du marquage lu du ticket : " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void marquerTicketNonLu(int idTicket, boolean cibleAdmin) {
        if (idTicket <= 0) {
            throw new IllegalArgumentException("L'ID du ticket est invalide.");
        }
        try {
            ticketDAO.marquerTicketNonLu(idTicket, cibleAdmin);
        } catch (SQLException e) {
            System.err.println("Erreur lors du marquage non-lu du ticket : " + e.getMessage());
        }
    }

    /**
     * Retourne une description relative de l'ancienneté d'ouverture du ticket.
     *
     * @param ticket ticket source
     * @return libellé du type « Ouvert il y a X jour(s) » ou chaîne vide si ticket invalide
     */
    public String getDureeOuverture(Ticket ticket) {
        if (ticket == null || ticket.getDateOuverture() == null) return "";
        long jours = ChronoUnit.DAYS.between(ticket.getDateOuverture(), LocalDateTime.now());
        if (jours > 0) return "Ouvert il y a " + jours + " jour(s)";
        long heures = ChronoUnit.HOURS.between(ticket.getDateOuverture(), LocalDateTime.now());
        return "Ouvert il y a " + heures + " heure(s)";
    }

    /**
     * Valide les champs obligatoires d'un ticket avant création.
     *
     * @param ticket entité ticket à contrôler
     * @throws IllegalArgumentException si sujet, description, service ou auteur sont invalides
     */
    private void validerTicket(Ticket ticket) throws IllegalArgumentException {
        if (ticket == null) {
            throw new IllegalArgumentException("Les données du ticket sont vides.");
        }

        if (ticket.getSujet() == null || ticket.getSujet().trim().isEmpty()) {
            throw new IllegalArgumentException("Le sujet du ticket est obligatoire.");
        }
        if (ticket.getSujet().trim().length() > SUJET_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Le sujet ne peut pas dépasser " + SUJET_MAX_LENGTH + " caractères.");
        }

        if (ticket.getDescription() == null || ticket.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("La description du ticket est obligatoire.");
        }
        if (ticket.getDescription().trim().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "La description ne peut pas dépasser " + DESCRIPTION_MAX_LENGTH + " caractères.");
        }

        validerService(ticket.getService());

        if (ticket.getAuteur() == null || ticket.getAuteur().getId() <= 0) {
            throw new IllegalArgumentException("L'auteur du ticket est obligatoire.");
        }
    }

    /**
     * Valide le contenu et l'auteur d'un message avant persistance.
     *
     * @param message entité message à contrôler
     * @throws IllegalArgumentException si contenu ou auteur sont invalides
     */
    private void validerMessage(Message message) throws IllegalArgumentException {
        if (message == null) {
            throw new IllegalArgumentException("Les données du message sont vides.");
        }

        if (message.getContenu() == null || message.getContenu().trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu du message est obligatoire.");
        }
        if (message.getContenu().trim().length() > CONTENU_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Le message ne peut pas dépasser " + CONTENU_MAX_LENGTH + " caractères.");
        }

        if (message.getAuteur() == null || message.getAuteur().getId() <= 0) {
            throw new IllegalArgumentException("L'auteur du message est obligatoire.");
        }
    }

    /**
     * Valide le service destinataire d'un ticket ({@code ADMIN} ou {@code RH}).
     *
     * @param service libellé de service attendu
     * @throws IllegalArgumentException si le service est absent ou non autorisé
     */
    private void validerService(String service) throws IllegalArgumentException {
        if (service == null || service.trim().isEmpty()) {
            throw new IllegalArgumentException("Le service du ticket est obligatoire.");
        }
        if (!service.equalsIgnoreCase("ADMIN") && !service.equalsIgnoreCase("RH")) {
            throw new IllegalArgumentException(
                    "Le service doit être ADMIN ou RH. Valeur reçue : " + service);
        }
    }
}

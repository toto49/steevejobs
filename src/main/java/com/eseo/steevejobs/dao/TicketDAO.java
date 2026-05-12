package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des tickets.
 */
public class TicketDAO {

    /**
     * Créer un nouveau ticket
     */
    public void createTicket(Ticket ticket) throws SQLException {
        String sql = "INSERT INTO TICKETS (sujet, description, service, statut, date_ouverture, id_auteur) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, ticket.getSujet());
            stmt.setString(2, ticket.getDescription());
            stmt.setString(3, ticket.getService());
            stmt.setString(4, ticket.getStatut().name());
            stmt.setTimestamp(5, Timestamp.valueOf(ticket.getDateOuverture()));
            stmt.setInt(6, ticket.getAuteur().getId());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    ticket.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * Mettre à jour un ticket existant
     */
    public void updateTicket(Ticket ticket) throws SQLException {
        String sql = "UPDATE TICKETS SET sujet = ?, description = ?, service = ?, statut = ?, date_ouverture = ?, id_auteur = ? WHERE id_tickets = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ticket.getSujet());
            stmt.setString(2, ticket.getDescription());
            stmt.setString(3, ticket.getService());
            stmt.setString(4, ticket.getStatut().name());
            stmt.setTimestamp(5, Timestamp.valueOf(ticket.getDateOuverture()));
            stmt.setInt(6, ticket.getAuteur().getId());
            stmt.setInt(7, ticket.getId());

            stmt.executeUpdate();
        }
    }

    /**
     * Supprimer un ticket par son ID
     */
    public boolean deleteTicket(int id) throws SQLException {
        String sql = "DELETE FROM TICKETS WHERE id_tickets = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Récupérer un ticket par son ID (Avec la date de dernière activité)
     */
    public Ticket getById(int id) throws SQLException {
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "(SELECT MAX(date_envoi) FROM MESSAGES m WHERE m.id_ticket = t.id_tickets) AS date_derniere_activite " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "WHERE t.id_tickets = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapperTicket(rs);
                }
                return null;
            }
        }
    }

    /**
     * Récupérer tous les tickets d'un auteur (Triés par activité)
     */
    public List<Ticket> findByAuteurId(int auteurId) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "(SELECT MAX(date_envoi) FROM MESSAGES m WHERE m.id_ticket = t.id_tickets) AS date_derniere_activite " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "WHERE t.id_auteur = ? " +
                "ORDER BY COALESCE(date_derniere_activite, t.date_ouverture) DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auteurId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapperTicket(rs));
                }
            }
        }
        return tickets;
    }

    /**
     * Récupérer les tickets par statut (Triés par activité)
     */
    public List<Ticket> findByStatut(StatutTicket statut) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "(SELECT MAX(date_envoi) FROM MESSAGES m WHERE m.id_ticket = t.id_tickets) AS date_derniere_activite " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "WHERE t.statut = ? " +
                "ORDER BY COALESCE(date_derniere_activite, t.date_ouverture) DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapperTicket(rs));
                }
            }
        }
        return tickets;
    }

    /**
     * Récupérer tous les tickets (Triés par activité)
     */
    public List<Ticket> findAll() throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "(SELECT MAX(date_envoi) FROM MESSAGES m WHERE m.id_ticket = t.id_tickets) AS date_derniere_activite " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "ORDER BY COALESCE(date_derniere_activite, t.date_ouverture) DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tickets.add(mapperTicket(rs));
            }
        }
        return tickets;
    }

    /**
     * Mettre à jour le statut d'un ticket
     */
    public boolean updateStatut(int id, StatutTicket statut) throws SQLException {
        String sql = "UPDATE TICKETS SET statut = ? WHERE id_tickets = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut.name());
            stmt.setInt(2, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Compter le nombre total de tickets
     */
    public int countTickets() throws SQLException {
        String sql = "SELECT COUNT(*) FROM TICKETS";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de tickets par statut
     */
    public int countByStatut(StatutTicket statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TICKETS WHERE statut = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de tickets par auteur
     */
    public int countByAuteurId(int auteurId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TICKETS WHERE id_auteur = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auteurId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Ticket mapperTicket(ResultSet rs) throws SQLException {
        User auteur = new User(
                rs.getInt("id_user"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("mdp"),
                rs.getString("adresse"),
                rs.getString("role"),
                rs.getString("tel"),
                rs.getString("poste"),
                rs.getBoolean("actif")
        );

        Ticket ticket = new Ticket(
                rs.getInt("id_tickets"),
                rs.getString("sujet"),
                rs.getString("description"),
                rs.getString("service"),
                StatutTicket.valueOf(rs.getString("statut")),
                rs.getTimestamp("date_ouverture").toLocalDateTime(),
                auteur
        );
        Timestamp derniereActivite = rs.getTimestamp("date_derniere_activite");
        if (derniereActivite != null) {
            ticket.setDateDerniereActivite(derniereActivite.toLocalDateTime());
        } else {
            ticket.setDateDerniereActivite(ticket.getDateOuverture());
        }

        return ticket;
    }
}
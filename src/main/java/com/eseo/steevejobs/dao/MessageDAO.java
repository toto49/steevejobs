package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.model.Enum.StatutTicket;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des messages.
 * <p>
 * Contient les requêtes SQL (INSERT, SELECT, UPDATE, DELETE) permettant de lire
 * et sauvegarder les objets {@link com.eseo.steevejobs.model.Message} en base de données.
 * </p>
 */
public class MessageDAO {

    /**
     * Créer un nouveau message
     *
     * @param message le message à créer
     * @throws SQLException exception SQL
     */
    public void createMessage(Message message) throws SQLException {
        String sql = "INSERT INTO MESSAGES (contenu, piece_jointe, date_envoi, id_auteur, id_ticket) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getContenu());
            stmt.setString(2, message.getPieceJointe());
            stmt.setTimestamp(3, Timestamp.valueOf(message.getDateEnvoi()));
            stmt.setInt(4, message.getAuteur().getId());
            stmt.setInt(5, message.getTicket().getId());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * Mettre à jour un message existant
     *
     * @param message le message à mettre à jour
     * @throws SQLException exception SQL
     */
    public void updateMessage(Message message) throws SQLException {
        String sql = "UPDATE MESSAGES SET contenu = ?, piece_jointe = ?, date_envoi = ?, id_auteur = ?, id_ticket = ? WHERE id_messages = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getContenu());
            stmt.setString(2, message.getPieceJointe());
            stmt.setTimestamp(3, Timestamp.valueOf(message.getDateEnvoi()));
            stmt.setInt(4, message.getAuteur().getId());
            stmt.setInt(5, message.getTicket().getId());
            stmt.setInt(6, message.getId());

            stmt.executeUpdate();
        }
    }

    /**
     * Supprimer un message par son ID
     *
     * @param id l'ID du message
     * @return true si supprimé, false sinon
     * @throws SQLException exception SQL
     */
    public boolean deleteMessage(int id) throws SQLException {
        String sql = "DELETE FROM MESSAGES WHERE id_messages = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Récupérer un message par son ID
     *
     * @param id l'ID du message
     * @return le message trouvé, null sinon
     * @throws SQLException exception SQL
     */
    public Message getById(int id) throws SQLException {
        String sql = "SELECT m.*, " +
                "u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "t.id_tickets, t.service, t.statut, t.date_ouverture, " +
                "tu.id_user as ticket_user_id, tu.nom as ticket_user_nom, tu.prenom as ticket_user_prenom, " +
                "tu.email as ticket_user_email, tu.mdp as ticket_user_mdp, tu.adresse as ticket_user_adresse, " +
                "tu.tel as ticket_user_tel, tu.role as ticket_user_role, tu.poste as ticket_user_poste, tu.actif as ticket_user_actif " +
                "FROM MESSAGES m " +
                "INNER JOIN USER u ON m.id_auteur = u.id_user " +
                "INNER JOIN TICKETS t ON m.id_ticket = t.id_tickets " +
                "INNER JOIN USER tu ON t.id_auteur = tu.id_user " +
                "WHERE m.id_messages = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
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

                    User ticketAuteur = new User(
                            rs.getInt("ticket_user_id"),
                            rs.getString("ticket_user_nom"),
                            rs.getString("ticket_user_prenom"),
                            rs.getString("ticket_user_email"),
                            rs.getString("ticket_user_mdp"),
                            rs.getString("ticket_user_adresse"),
                            rs.getString("ticket_user_role"),
                            rs.getString("ticket_user_tel"),
                            rs.getString("ticket_user_poste"),
                            rs.getBoolean("ticket_user_actif")
                    );

                    Ticket ticket = new Ticket(
                            rs.getInt("id_tickets"),
                            rs.getString("service"),
                            StatutTicket.valueOf(rs.getString("statut")),
                            rs.getTimestamp("date_ouverture").toLocalDateTime(),
                            ticketAuteur
                    );

                    return new Message(
                            rs.getInt("id_messages"),
                            rs.getString("contenu"),
                            rs.getString("piece_jointe"),
                            rs.getTimestamp("date_envoi").toLocalDateTime(),
                            auteur,
                            ticket
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer tous les messages d'un ticket
     *
     * @param ticketId l'ID du ticket
     * @return la liste des messages du ticket
     * @throws SQLException exception SQL
     */
    public List<Message> findByTicketId(int ticketId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, " +
                "u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "t.id_tickets, t.service, t.statut, t.date_ouverture, " +
                "tu.id_user as ticket_user_id, tu.nom as ticket_user_nom, tu.prenom as ticket_user_prenom, " +
                "tu.email as ticket_user_email, tu.mdp as ticket_user_mdp, tu.adresse as ticket_user_adresse, " +
                "tu.tel as ticket_user_tel, tu.role as ticket_user_role, tu.poste as ticket_user_poste, tu.actif as ticket_user_actif " +
                "FROM MESSAGES m " +
                "INNER JOIN USER u ON m.id_auteur = u.id_user " +
                "INNER JOIN TICKETS t ON m.id_ticket = t.id_tickets " +
                "INNER JOIN USER tu ON t.id_auteur = tu.id_user " +
                "WHERE m.id_ticket = ? " +
                "ORDER BY m.date_envoi ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ticketId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
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

                    User ticketAuteur = new User(
                            rs.getInt("ticket_user_id"),
                            rs.getString("ticket_user_nom"),
                            rs.getString("ticket_user_prenom"),
                            rs.getString("ticket_user_email"),
                            rs.getString("ticket_user_mdp"),
                            rs.getString("ticket_user_adresse"),
                            rs.getString("ticket_user_role"),
                            rs.getString("ticket_user_tel"),
                            rs.getString("ticket_user_poste"),
                            rs.getBoolean("ticket_user_actif")
                    );

                    Ticket ticket = new Ticket(
                            rs.getInt("id_tickets"),
                            rs.getString("service"),
                            StatutTicket.valueOf(rs.getString("statut")),
                            rs.getTimestamp("date_ouverture").toLocalDateTime(),
                            ticketAuteur
                    );

                    messages.add(new Message(
                            rs.getInt("id_messages"),
                            rs.getString("contenu"),
                            rs.getString("piece_jointe"),
                            rs.getTimestamp("date_envoi").toLocalDateTime(),
                            auteur,
                            ticket
                    ));
                }
            }
        }
        return messages;
    }

    /**
     * Récupérer tous les messages d'un auteur
     *
     * @param auteurId l'ID de l'auteur
     * @return la liste des messages de l'auteur
     * @throws SQLException exception SQL
     */
    public List<Message> findByAuteurId(int auteurId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, " +
                "u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "t.id_tickets, t.service, t.statut, t.date_ouverture, " +
                "tu.id_user as ticket_user_id, tu.nom as ticket_user_nom, tu.prenom as ticket_user_prenom, " +
                "tu.email as ticket_user_email, tu.mdp as ticket_user_mdp, tu.adresse as ticket_user_adresse, " +
                "tu.tel as ticket_user_tel, tu.role as ticket_user_role, tu.poste as ticket_user_poste, tu.actif as ticket_user_actif " +
                "FROM MESSAGES m " +
                "INNER JOIN USER u ON m.id_auteur = u.id_user " +
                "INNER JOIN TICKETS t ON m.id_ticket = t.id_tickets " +
                "INNER JOIN USER tu ON t.id_auteur = tu.id_user " +
                "WHERE m.id_auteur = ? " +
                "ORDER BY m.date_envoi DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auteurId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
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

                    User ticketAuteur = new User(
                            rs.getInt("ticket_user_id"),
                            rs.getString("ticket_user_nom"),
                            rs.getString("ticket_user_prenom"),
                            rs.getString("ticket_user_email"),
                            rs.getString("ticket_user_mdp"),
                            rs.getString("ticket_user_adresse"),
                            rs.getString("ticket_user_role"),
                            rs.getString("ticket_user_tel"),
                            rs.getString("ticket_user_poste"),
                            rs.getBoolean("ticket_user_actif")
                    );

                    Ticket ticket = new Ticket(
                            rs.getInt("id_tickets"),
                            rs.getString("service"),
                            StatutTicket.valueOf(rs.getString("statut")),
                            rs.getTimestamp("date_ouverture").toLocalDateTime(),
                            ticketAuteur
                    );

                    messages.add(new Message(
                            rs.getInt("id_messages"),
                            rs.getString("contenu"),
                            rs.getString("piece_jointe"),
                            rs.getTimestamp("date_envoi").toLocalDateTime(),
                            auteur,
                            ticket
                    ));
                }
            }
        }
        return messages;
    }

    /**
     * Récupérer tous les messages
     *
     * @return la liste de tous les messages
     * @throws SQLException exception SQL
     */
    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, " +
                "u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "t.id_tickets, t.service, t.statut, t.date_ouverture, " +
                "tu.id_user as ticket_user_id, tu.nom as ticket_user_nom, tu.prenom as ticket_user_prenom, " +
                "tu.email as ticket_user_email, tu.mdp as ticket_user_mdp, tu.adresse as ticket_user_adresse, " +
                "tu.tel as ticket_user_tel, tu.role as ticket_user_role, tu.poste as ticket_user_poste, tu.actif as ticket_user_actif " +
                "FROM MESSAGES m " +
                "INNER JOIN USER u ON m.id_auteur = u.id_user " +
                "INNER JOIN TICKETS t ON m.id_ticket = t.id_tickets " +
                "INNER JOIN USER tu ON t.id_auteur = tu.id_user " +
                "ORDER BY m.date_envoi DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
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

                User ticketAuteur = new User(
                        rs.getInt("ticket_user_id"),
                        rs.getString("ticket_user_nom"),
                        rs.getString("ticket_user_prenom"),
                        rs.getString("ticket_user_email"),
                        rs.getString("ticket_user_mdp"),
                        rs.getString("ticket_user_adresse"),
                        rs.getString("ticket_user_role"),
                        rs.getString("ticket_user_tel"),
                        rs.getString("ticket_user_poste"),
                        rs.getBoolean("ticket_user_actif")
                );

                Ticket ticket = new Ticket(
                        rs.getInt("id_tickets"),
                        rs.getString("service"),
                        StatutTicket.valueOf(rs.getString("statut")),
                        rs.getTimestamp("date_ouverture").toLocalDateTime(),
                        ticketAuteur
                );

                messages.add(new Message(
                        rs.getInt("id_messages"),
                        rs.getString("contenu"),
                        rs.getString("piece_jointe"),
                        rs.getTimestamp("date_envoi").toLocalDateTime(),
                        auteur,
                        ticket
                ));
            }
        }
        return messages;
    }

    /**
     * Supprimer tous les messages d'un ticket
     *
     * @param ticketId l'ID du ticket
     * @return le nombre de messages supprimés
     * @throws SQLException exception SQL
     */
    public int deleteByTicketId(int ticketId) throws SQLException {
        String sql = "DELETE FROM MESSAGES WHERE id_ticket = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ticketId);

            return stmt.executeUpdate();
        }
    }

    /**
     * Compter le nombre total de messages
     *
     * @return le nombre total de messages
     * @throws SQLException exception SQL
     */
    public int countMessages() throws SQLException {
        String sql = "SELECT COUNT(*) FROM MESSAGES";

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
     * Compter le nombre de messages par ticket
     *
     * @param ticketId l'ID du ticket
     * @return le nombre de messages du ticket
     * @throws SQLException exception SQL
     */
    public int countByTicketId(int ticketId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM MESSAGES WHERE id_ticket = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ticketId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de messages par auteur
     *
     * @param auteurId l'ID de l'auteur
     * @return le nombre de messages de l'auteur
     * @throws SQLException exception SQL
     */
    public int countByAuteurId(int auteurId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM MESSAGES WHERE id_auteur = ?";

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
}
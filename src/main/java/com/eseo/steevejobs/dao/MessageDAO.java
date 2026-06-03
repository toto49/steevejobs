package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données de la table {@code MESSAGES}.
 * <p>
 * Les lectures joignent {@code USER} (auteur), {@code TICKETS} et {@code USER} (auteur du ticket).
 * Chaque opération s'exécute en auto-commit ; les {@link SQLException} sont propagées.
 * </p>
 */
public class MessageDAO {

    /**
     * Insère un message et récupère la clé générée.
     * <p>
     * SQL : {@code INSERT INTO MESSAGES} avec {@code RETURN_GENERATED_KEYS}.
     * </p>
     *
     * @param message message à persister ; l'identifiant est mis à jour après insertion
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Met à jour un message existant.
     *
     * @param message message avec identifiant et champs modifiés
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Supprime un message par identifiant.
     *
     * @param id identifiant du message
     * @return {@code true} si au moins une ligne a été supprimée
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Recherche un message par identifiant avec auteur et ticket jointés.
     *
     * @param id identifiant du message
     * @return message trouvé, ou {@code null}
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public Message getById(int id) throws SQLException {
        String sql = "SELECT m.*, " +
                "u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "t.id_tickets, t.sujet, t.description, t.service, t.statut, t.date_ouverture, " +
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
                            rs.getString("sujet"),
                            rs.getString("description"),
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
     * Liste les messages d'un ticket, triés par date d'envoi croissante.
     *
     * @param ticketId identifiant du ticket
     * @return liste des messages (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<Message> findByTicketId(int ticketId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, " +
                "u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "t.id_tickets, t.sujet, t.description, t.service, t.statut, t.date_ouverture, " +
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
                            rs.getString("sujet"),
                            rs.getString("description"),
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
     * Liste les messages rédigés par un auteur, triés par date d'envoi décroissante.
     *
     * @param auteurId identifiant de l'auteur
     * @return liste des messages (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<Message> findByAuteurId(int auteurId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, " +
                "u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "t.id_tickets, t.sujet, t.description, t.service, t.statut, t.date_ouverture, " +
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
                            rs.getString("sujet"),
                            rs.getString("description"),
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
     * Liste tous les messages, triés par date d'envoi décroissante.
     *
     * @return liste complète (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, " +
                "u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "t.id_tickets, t.sujet, t.description, t.service, t.statut, t.date_ouverture, " +
                "tu.id_user as ticket_user_id, tu.nom as ticket_user_nom, tu.prenom as ticket_user_prenom, " +
                "tu.email as ticket_user_email, tu.mdp as ticket_user_mdp, tu.adresse as ticket_user_adresse, " +
                "tu.tel as ticket_user_tel, tu.role as ticket_user_role, tu.poste as ticket_user_poste, tu.actif as ticket_user_actif " +
                "FROM MESSAGES m " +
                "INNER JOIN USER u ON m.id_auteur = u.id_user " +
                "INNER JOIN TICKETS t ON m.id_ticket = t.id_tickets " +
                "INNER JOIN USER tu ON t.id_auteur = tu.id_user " +
                "ORDER BY m.date_envoi DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

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
                        rs.getString("sujet"),
                        rs.getString("description"),
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
     * Supprime tous les messages associés à un ticket.
     * <p>
     * SQL : {@code DELETE FROM MESSAGES WHERE id_ticket = ?}.
     * </p>
     *
     * @param ticketId identifiant du ticket
     * @return nombre de lignes supprimées
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Compte le nombre total de messages enregistrés.
     *
     * @return nombre de messages ({@code 0} si la table est vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public int countMessages() throws SQLException {
        String sql = "SELECT COUNT(*) FROM MESSAGES";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compte le nombre de messages d'un ticket.
     *
     * @param ticketId identifiant du ticket
     * @return nombre de messages ({@code 0} si aucun)
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Compte le nombre de messages rédigés par un auteur.
     *
     * @param auteurId identifiant de l'auteur
     * @return nombre de messages ({@code 0} si aucun)
     * @throws SQLException en cas d'erreur d'accès à la base
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
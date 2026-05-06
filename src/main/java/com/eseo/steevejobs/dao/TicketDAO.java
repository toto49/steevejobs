package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.model.Enum.StatutTicket;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des tickets.
 * <p>
 * Contient les requêtes SQL (INSERT, SELECT, UPDATE, DELETE) permettant de lire
 * et sauvegarder les objets {@link com.eseo.steevejobs.model.Ticket} en base de données.
 * </p>
 */
public class TicketDAO {

    /**
     * Créer un nouveau ticket
     * @param ticket le ticket à créer
     * @throws SQLException exception SQL
     */
    public void createTicket(Ticket ticket) throws SQLException {
        String sql = "INSERT INTO TICKETS (service, statut, date_ouverture, id_auteur) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, ticket.getService());
            stmt.setString(2, ticket.getStatut().name());
            stmt.setTimestamp(3, Timestamp.valueOf(ticket.getDateOuverture()));
            stmt.setInt(4, ticket.getAuteur().getId());

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
     * @param ticket le ticket à mettre à jour
     * @throws SQLException exception SQL
     */
    public void updateTicket(Ticket ticket) throws SQLException {
        String sql = "UPDATE TICKETS SET service = ?, statut = ?, date_ouverture = ?, id_auteur = ? WHERE id_tickets = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ticket.getService());
            stmt.setString(2, ticket.getStatut().name());
            stmt.setTimestamp(3, Timestamp.valueOf(ticket.getDateOuverture()));
            stmt.setInt(4, ticket.getAuteur().getId());
            stmt.setInt(5, ticket.getId());

            stmt.executeUpdate();
        }
    }

    /**
     * Supprimer un ticket par son ID
     * @param id l'ID du ticket
     * @return true si supprimé, false sinon
     * @throws SQLException exception SQL
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
     * Récupérer un ticket par son ID
     * @param id l'ID du ticket
     * @return le ticket trouvé, null sinon
     * @throws SQLException exception SQL
     */
    public Ticket getById(int id) throws SQLException {
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "WHERE t.id_tickets = ?";

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
                    return new Ticket(
                            rs.getInt("id_tickets"),
                            rs.getString("service"),
                            StatutTicket.valueOf(rs.getString("statut")),
                            rs.getTimestamp("date_ouverture").toLocalDateTime(),
                            auteur
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer tous les tickets d'un auteur
     * @param auteurId l'ID de l'auteur
     * @return la liste des tickets de l'auteur
     * @throws SQLException exception SQL
     */
    public List<Ticket> findByAuteurId(int auteurId) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "WHERE t.id_auteur = ? " +
                "ORDER BY t.date_ouverture DESC";

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
                    tickets.add(new Ticket(
                            rs.getInt("id_tickets"),
                            rs.getString("service"),
                            StatutTicket.valueOf(rs.getString("statut")),
                            rs.getTimestamp("date_ouverture").toLocalDateTime(),
                            auteur
                    ));
                }
            }
        }
        return tickets;
    }

    /**
     * Récupérer les tickets par statut
     * @param statut le statut du ticket (EN_ATTENTE, EN_COURS, FERMÉ)
     * @return la liste des tickets correspondants
     * @throws SQLException exception SQL
     */
    public List<Ticket> findByStatut(StatutTicket statut) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "WHERE t.statut = ? " +
                "ORDER BY t.date_ouverture DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut.name());

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
                    tickets.add(new Ticket(
                            rs.getInt("id_tickets"),
                            rs.getString("service"),
                            StatutTicket.valueOf(rs.getString("statut")),
                            rs.getTimestamp("date_ouverture").toLocalDateTime(),
                            auteur
                    ));
                }
            }
        }
        return tickets;
    }

    /**
     * Récupérer tous les tickets
     * @return la liste de tous les tickets
     * @throws SQLException exception SQL
     */
    public List<Ticket> findAll() throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "ORDER BY t.date_ouverture DESC";

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
                tickets.add(new Ticket(
                        rs.getInt("id_tickets"),
                        rs.getString("service"),
                        StatutTicket.valueOf(rs.getString("statut")),
                        rs.getTimestamp("date_ouverture").toLocalDateTime(),
                        auteur
                ));
            }
        }
        return tickets;
    }

    /**
     * Mettre à jour le statut d'un ticket
     * @param id     l'ID du ticket
     * @param statut le nouveau statut
     * @return true si mis à jour, false sinon
     * @throws SQLException exception SQL
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
     * @return le nombre total de tickets
     * @throws SQLException exception SQL
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
     * @param statut le statut
     * @return le nombre de tickets avec ce statut
     * @throws SQLException exception SQL
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
     * @param auteurId l'ID de l'auteur
     * @return le nombre de tickets de l'auteur
     * @throws SQLException exception SQL
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
}
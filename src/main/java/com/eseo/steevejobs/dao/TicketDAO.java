package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données de la table {@code TICKETS}.
 * <p>
 * Les lectures joignent {@code USER} (auteur) et calculent la date de dernière activité
 * via une sous-requête sur {@code MESSAGES}. Le tri par activité utilise
 * {@code COALESCE(dernière activité, date_ouverture)}.
 * Chaque opération s'exécute en auto-commit ; les {@link SQLException} sont propagées.
 * </p>
 */
public class TicketDAO {

    private static final String SQL_DERNIERE_ACTIVITE =
            "(SELECT MAX(date_envoi) FROM MESSAGES m WHERE m.id_ticket = t.id_tickets)";
    private static final String SQL_ORDER_BY_ACTIVITE =
            "ORDER BY COALESCE(" + SQL_DERNIERE_ACTIVITE + ", t.date_ouverture) DESC";

    /**
     * Insère un ticket et récupère la clé générée.
     * <p>
     * SQL : {@code INSERT INTO TICKETS} avec {@code RETURN_GENERATED_KEYS}.
     * </p>
     *
     * @param ticket ticket à persister ; l'identifiant est mis à jour après insertion
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Met à jour un ticket existant.
     *
     * @param ticket ticket avec identifiant et champs modifiés
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Supprime un ticket par identifiant.
     *
     * @param id identifiant du ticket
     * @return {@code true} si au moins une ligne a été supprimée
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Recherche un ticket par identifiant avec auteur et date de dernière activité.
     *
     * @param id identifiant du ticket
     * @return ticket trouvé, ou {@code null}
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Liste les tickets d'un auteur, triés par dernière activité décroissante.
     *
     * @param auteurId identifiant de l'auteur
     * @return liste des tickets (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<Ticket> findByAuteurId(int auteurId) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "(SELECT MAX(date_envoi) FROM MESSAGES m WHERE m.id_ticket = t.id_tickets) AS date_derniere_activite " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "WHERE t.id_auteur = ? " +
                "ORDER BY COALESCE(" + SQL_DERNIERE_ACTIVITE + ", t.date_ouverture) DESC";

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
     * Liste les tickets filtrés par statut, triés par dernière activité décroissante.
     *
     * @param statut statut recherché
     * @return liste des tickets (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<Ticket> findByStatut(StatutTicket statut) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "(SELECT MAX(date_envoi) FROM MESSAGES m WHERE m.id_ticket = t.id_tickets) AS date_derniere_activite " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "WHERE t.statut = ? " +
                "ORDER BY COALESCE(" + SQL_DERNIERE_ACTIVITE + ", t.date_ouverture) DESC";

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
     * Liste tous les tickets, triés par dernière activité décroissante.
     *
     * @return liste complète (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<Ticket> findAll() throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT t.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif, " +
                "(SELECT MAX(date_envoi) FROM MESSAGES m WHERE m.id_ticket = t.id_tickets) AS date_derniere_activite " +
                "FROM TICKETS t " +
                "INNER JOIN USER u ON t.id_auteur = u.id_user " +
                "ORDER BY COALESCE(" + SQL_DERNIERE_ACTIVITE + ", t.date_ouverture) DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tickets.add(mapperTicket(rs));
            }
        }
        return tickets;
    }

    /**
     * Met à jour uniquement le statut d'un ticket.
     *
     * @param id     identifiant du ticket
     * @param statut nouveau statut
     * @return {@code true} si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Compte le nombre total de tickets enregistrés.
     *
     * @return nombre de tickets ({@code 0} si la table est vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public int countTickets() throws SQLException {
        String sql = "SELECT COUNT(*) FROM TICKETS";

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
     * Compte le nombre de tickets pour un statut donné.
     *
     * @param statut statut recherché
     * @return nombre de tickets ({@code 0} si aucun)
     * @throws SQLException en cas d'erreur d'accès à la base
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
     * Compte le nombre de tickets créés par un auteur.
     *
     * @param auteurId identifiant de l'auteur
     * @return nombre de tickets ({@code 0} si aucun)
     * @throws SQLException en cas d'erreur d'accès à la base
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

    /**
     * Compte les tickets non lus par l'administrateur d'un service, hors tickets de l'utilisateur courant.
     * <p>
     * SQL : {@code COUNT} sur {@code non_lu_admin = 1}, filtré par service et auteur différent.
     * </p>
     *
     * @param service       service concerné
     * @param idCurrentUser identifiant de l'utilisateur courant (exclu du décompte)
     * @return nombre de tickets non lus ({@code 0} si aucun)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public int countTicketsNonLusAdmin(String service, int idCurrentUser) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TICKETS WHERE non_lu_admin = 1 AND service = ? AND id_auteur != ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, service);
            stmt.setInt(2, idCurrentUser);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Compte les tickets non lus par leur auteur.
     *
     * @param idAuteur identifiant de l'auteur
     * @return nombre de tickets non lus ({@code 0} si aucun)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public int countTicketsNonLusAuteur(int idAuteur) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TICKETS WHERE non_lu_auteur = 1 AND id_auteur = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idAuteur);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Marque un ticket comme lu pour l'administrateur ou l'auteur.
     * <p>
     * SQL : {@code UPDATE TICKETS SET non_lu_admin/non_lu_auteur = 0} selon le rôle cible.
     * </p>
     *
     * @param idTicket identifiant du ticket
     * @param estAdmin {@code true} pour réinitialiser {@code non_lu_admin}, {@code false} pour {@code non_lu_auteur}
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public void marquerTicketLu(int idTicket, boolean estAdmin) throws SQLException {
        String colonne = estAdmin ? "non_lu_admin" : "non_lu_auteur";
        String sql = "UPDATE TICKETS SET " + colonne + " = 0 WHERE id_tickets = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idTicket);
            stmt.executeUpdate();
        }
    }


    /**
     * Marque un ticket comme non lu pour l'administrateur ou l'auteur.
     * <p>
     * SQL : {@code UPDATE TICKETS SET non_lu_admin/non_lu_auteur = 1} selon la cible.
     * </p>
     *
     * @param idTicket   identifiant du ticket
     * @param cibleAdmin {@code true} pour positionner {@code non_lu_admin}, {@code false} pour {@code non_lu_auteur}
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public void marquerTicketNonLu(int idTicket, boolean cibleAdmin) throws SQLException {
        String colonne = cibleAdmin ? "non_lu_admin" : "non_lu_auteur";
        String sql = "UPDATE TICKETS SET " + colonne + " = 1 WHERE id_tickets = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idTicket);
            stmt.executeUpdate();
        }
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
        ticket.setNonLuAdmin(rs.getBoolean("non_lu_admin"));
        ticket.setNonLuAuteur(rs.getBoolean("non_lu_auteur"));
        Timestamp derniereActivite = rs.getTimestamp("date_derniere_activite");
        if (derniereActivite != null) {
            ticket.setDateDerniereActivite(derniereActivite.toLocalDateTime());
        } else {
            ticket.setDateDerniereActivite(ticket.getDateOuverture());
        }

        return ticket;
    }
}
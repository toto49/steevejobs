package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.Enum.StatutDemandeConge;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux données de la table {@code DEMANDE_CONGE}.
 * <p>
 * Les lectures joignent {@code USER} pour hydrater l'employé demandeur.
 * Chaque opération s'exécute en auto-commit ; les {@link SQLException} sont propagées.
 * L'identifiant de planning est stocké en {@code NULL} lorsque {@code id_planning <= 0}.
 * </p>
 */
public class DemandeCongeDAO {

    /**
     * Insère une nouvelle demande de congé et récupère la clé générée.
     * <p>
     * SQL : {@code INSERT INTO DEMANDE_CONGE} avec {@code RETURN_GENERATED_KEYS}.
     * </p>
     *
     * @param demande demande à persister ; l'identifiant est mis à jour après insertion
     * @return {@code true} si l'insertion a réussi
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean create(DemandeConge demande) throws SQLException {
        String sql = """
                INSERT INTO DEMANDE_CONGE (jour_debut, jour_fin, statut, commentaire_employe, commentaire_rh, date_demande, id_user, id_planning)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setTimestamp(1, Timestamp.valueOf(demande.getJourDebut()));
            stmt.setTimestamp(2, Timestamp.valueOf(demande.getJourFin()));
            stmt.setString(3, demande.getStatut().name());
            stmt.setString(4, demande.getCommentaireEmploye());
            stmt.setString(5, demande.getCommentaireRh());
            stmt.setTimestamp(6, Timestamp.valueOf(demande.getDateDemande()));
            stmt.setInt(7, demande.getEmploye().getId());
            if (demande.getIdPlanning() > 0) {
                stmt.setInt(8, demande.getIdPlanning());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    demande.setId(generatedKeys.getInt(1));
                }
            }
            return true;
        }
    }

    /**
     * Met à jour une demande de congé existante.
     * <p>
     * SQL : {@code UPDATE DEMANDE_CONGE} filtré par {@code id_demande_conge}.
     * </p>
     *
     * @param demande demande avec identifiant et champs modifiés
     * @return {@code true} si au moins une ligne a été modifiée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean update(DemandeConge demande) throws SQLException {
        String sql = """
                UPDATE DEMANDE_CONGE
                SET jour_debut = ?, jour_fin = ?, statut = ?, commentaire_employe = ?, commentaire_rh = ?, date_demande = ?, id_user = ?, id_planning = ?
                WHERE id_demande_conge = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(demande.getJourDebut()));
            stmt.setTimestamp(2, Timestamp.valueOf(demande.getJourFin()));
            stmt.setString(3, demande.getStatut().name());
            stmt.setString(4, demande.getCommentaireEmploye());
            stmt.setString(5, demande.getCommentaireRh());
            stmt.setTimestamp(6, Timestamp.valueOf(demande.getDateDemande()));
            stmt.setInt(7, demande.getEmploye().getId());
            if (demande.getIdPlanning() > 0) {
                stmt.setInt(8, demande.getIdPlanning());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }
            stmt.setInt(9, demande.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Recherche une demande par identifiant.
     *
     * @param id identifiant de la demande
     * @return demande trouvée avec employé joint, ou {@code null}
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public DemandeConge findById(int id) throws SQLException {
        String sql = baseSelectSql() + " WHERE d.id_demande_conge = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Liste les demandes filtrées par statut, triées par date de demande décroissante.
     *
     * @param statut statut recherché
     * @return liste des demandes correspondantes (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<DemandeConge> findByStatut(StatutDemandeConge statut) throws SQLException {
        String sql = baseSelectSql() + " WHERE d.statut = ? ORDER BY d.date_demande DESC";
        List<DemandeConge> demandes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    demandes.add(mapRow(rs));
                }
            }
        }
        return demandes;
    }

    /**
     * Liste les demandes d'un employé, triées par date de demande décroissante.
     *
     * @param userId identifiant de l'employé
     * @return liste des demandes de l'employé (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<DemandeConge> findByUserId(int userId) throws SQLException {
        String sql = baseSelectSql() + " WHERE d.id_user = ? ORDER BY d.date_demande DESC";
        List<DemandeConge> demandes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    demandes.add(mapRow(rs));
                }
            }
        }
        return demandes;
    }

    /**
     * Liste toutes les demandes de congé, triées par date de demande décroissante.
     *
     * @return liste complète des demandes (éventuellement vide)
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public List<DemandeConge> findAll() throws SQLException {
        String sql = baseSelectSql() + " ORDER BY d.date_demande DESC";
        List<DemandeConge> demandes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                demandes.add(mapRow(rs));
            }
        }
        return demandes;
    }

    /**
     * Supprime une demande par identifiant.
     * <p>
     * SQL : {@code DELETE FROM DEMANDE_CONGE WHERE id_demande_conge = ?}.
     * </p>
     *
     * @param id identifiant de la demande
     * @return {@code true} si au moins une ligne a été supprimée
     * @throws SQLException en cas d'erreur d'accès à la base
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM DEMANDE_CONGE WHERE id_demande_conge = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private String baseSelectSql() {
        return """
                SELECT d.*, u.id_user, u.nom, u.prenom, u.email, u.mdp, u.adresse, u.tel, u.role, u.poste, u.actif
                FROM DEMANDE_CONGE d
                INNER JOIN USER u ON d.id_user = u.id_user
                """;
    }

    private DemandeConge mapRow(ResultSet rs) throws SQLException {
        User employe = new User(
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

        DemandeConge demande = new DemandeConge(
                rs.getInt("id_demande_conge"),
                rs.getTimestamp("jour_debut").toLocalDateTime(),
                rs.getTimestamp("jour_fin").toLocalDateTime(),
                StatutDemandeConge.valueOf(rs.getString("statut")),
                rs.getString("commentaire_employe"),
                rs.getString("commentaire_rh"),
                rs.getTimestamp("date_demande").toLocalDateTime(),
                employe,
                lireIdPlanning(rs)
        );
        return demande;
    }

    private int lireIdPlanning(ResultSet rs) throws SQLException {
        try {
            int id = rs.getInt("id_planning");
            return rs.wasNull() ? 0 : id;
        } catch (SQLException e) {
            return 0;
        }
    }
}

package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.Enum.StatutDemandeConge;
import com.eseo.steevejobs.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DemandeCongeDAO {

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

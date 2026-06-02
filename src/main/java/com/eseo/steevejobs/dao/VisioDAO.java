package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.util.TestRuntime;
import com.eseo.steevejobs.model.Enum.ReunionType;
import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.SalonAccesInfo;
import com.eseo.steevejobs.model.SalonEnCoursInfo;
import com.eseo.steevejobs.model.Visio;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VisioDAO {

    private static final ZoneId FUSEAU_HORAIRE = ZoneId.systemDefault();

    private static Timestamp localVersTimestamp(LocalDateTime dateHeure) {
        if (dateHeure == null) {
            return null;
        }
        return Timestamp.from(dateHeure.atZone(FUSEAU_HORAIRE).toInstant());
    }

    private static LocalDateTime timestampVersLocal(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atZone(FUSEAU_HORAIRE).toLocalDateTime();
    }

    private static LocalDateTime lireHeureProgrammee(ResultSet rs) throws SQLException {
        LocalDateTime depuisObjet = rs.getObject("heure_programmee", LocalDateTime.class);
        if (depuisObjet != null) {
            return depuisObjet;
        }
        return timestampVersLocal(rs.getTimestamp("heure_programmee"));
    }

    public boolean enregistrerSalonInstantane(Visio visio) {
        String sql = "INSERT INTO VISIO (room_name, createur_id, type_reunion, statut, heure_debut) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, visio.getRoom_name());
            stmt.setInt(2, visio.getCreateur_id());
            stmt.setString(3, visio.getType_reunion().name());
            stmt.setString(4, visio.getStatut().name());

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lors du stockage du salon instantané : " + e.getMessage());
            return false;
        }
    }

    public boolean planifierReunion(Visio visio, List<Integer> listeIdInvites) {
        String sqlVisio = "INSERT INTO VISIO (room_name, createur_id, statut, type_reunion, heure_programmee) "
                + "VALUES (?, ?, ?, ?, ?)";
        String sqlInvitation = "INSERT INTO VISIO_INVITATIONS (visio_id, employe_id) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtV = conn.prepareStatement(sqlVisio, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmtV.setString(1, visio.getRoom_name());
                stmtV.setInt(2, visio.getCreateur_id());
                stmtV.setString(3, visio.getStatut().name());
                stmtV.setString(4, visio.getType_reunion().name());
                stmtV.setTimestamp(5, localVersTimestamp(visio.getHeure_programmee()));
                stmtV.executeUpdate();

                try (ResultSet generatedKeys = stmtV.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newVisioId = generatedKeys.getInt(1);

                        if (listeIdInvites != null && !listeIdInvites.isEmpty()) {
                            try (PreparedStatement stmtI = conn.prepareStatement(sqlInvitation)) {
                                for (int idInvite : listeIdInvites) {
                                    stmtI.setInt(1, newVisioId);
                                    stmtI.setInt(2, idInvite);
                                    stmtI.addBatch();
                                }
                                stmtI.executeBatch();
                            }
                        }
                    } else {
                        throw new SQLException("Échec d'obtention de l'ID Visio.");
                    }
                }
            }
            conn.commit();
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lors de la planification : " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            return false;
        }
    }

    public int activerSalonsPlanifiesEligibles(LocalDateTime aPartirDe) {
        String sql = "UPDATE VISIO SET statut = ?, heure_debut = CURRENT_TIMESTAMP "
                + "WHERE statut = 'PROGRAMMEE' AND heure_programmee IS NOT NULL "
                + "AND heure_programmee <= ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, VisioStatut.EN_COURS.name());
            stmt.setTimestamp(2, localVersTimestamp(aPartirDe));
            return stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO activation automatique des salons : " + e.getMessage());
            return 0;
        }
    }

    public Optional<SalonAccesInfo> chargerInfosAccesSalon(String roomName, int userId) {
        String query = "SELECT v.statut, v.type_reunion, v.heure_programmee, v.createur_id, "
                + "(SELECT COUNT(*) FROM VISIO_INVITATIONS vi WHERE vi.visio_id = v.id AND vi.employe_id = ?) AS est_invite "
                + "FROM VISIO v WHERE v.room_name = ? AND v.statut != 'TERMINE'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setString(2, roomName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String typeStr = rs.getString("type_reunion");
                ReunionType type = null;
                if (typeStr != null && !typeStr.isBlank()) {
                    type = ReunionType.fromValeur(typeStr);
                }
                return Optional.of(new SalonAccesInfo(
                        VisioStatut.valueOf(rs.getString("statut")),
                        type,
                        rs.getInt("createur_id"),
                        rs.getInt("est_invite") > 0,
                        lireHeureProgrammee(rs)
                ));
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lecture accès salon : " + e.getMessage());
            return Optional.empty();
        }
    }

    public boolean existeEnBdd(String roomName) {
        String sql = "SELECT COUNT(*) FROM VISIO WHERE room_name = ? AND statut != 'TERMINE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public boolean isCreateur(String roomName, int userId) {
        String sql = "SELECT COUNT(*) FROM VISIO WHERE room_name = ? AND createur_id = ? AND statut != 'TERMINE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomName);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public void ouvrirSalon(String roomName) {
        String sql = "UPDATE VISIO SET statut = ?, heure_debut = CURRENT_TIMESTAMP "
                + "WHERE room_name = ? AND statut = 'PROGRAMMEE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, VisioStatut.EN_COURS.name());
            stmt.setString(2, roomName);
            stmt.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public void terminerSalonPlanifie(String roomName) {
        String sql = "UPDATE VISIO SET statut = ?, heure_fin = CURRENT_TIMESTAMP "
                + "WHERE room_name = ? AND statut = 'EN_COURS' AND type_reunion = 'PLANIFIEE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, VisioStatut.TERMINE.name());
            stmt.setString(2, roomName);
            stmt.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public Optional<SalonEnCoursInfo> chargerSalonEnCours(String roomName) {
        String sql = "SELECT type_reunion, heure_programmee FROM VISIO "
                + "WHERE room_name = ? AND statut = 'EN_COURS' ORDER BY id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new SalonEnCoursInfo(
                        rs.getString("type_reunion"),
                        timestampVersLocal(rs.getTimestamp("heure_programmee"))
                ));
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lecture salon en cours : " + e.getMessage());
            return Optional.empty();
        }
    }

    public boolean supprimerSalonInstantane(String roomName) {
        String selectSql = "SELECT id FROM VISIO WHERE room_name = ? "
                + "AND (type_reunion = 'INSTANTANEE' OR (type_reunion IS NULL AND heure_programmee IS NULL))";

        try (Connection conn = DatabaseConnection.getConnection()) {
            int visioId = -1;
            try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
                sel.setString(1, roomName);
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        visioId = rs.getInt("id");
                    }
                }
            }

            if (visioId < 0) {
                return false;
            }

            try (PreparedStatement inv = conn.prepareStatement(
                    "DELETE FROM VISIO_INVITATIONS WHERE visio_id = ?")) {
                inv.setInt(1, visioId);
                inv.executeUpdate();
            }

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM VISIO WHERE id = ?")) {
                del.setInt(1, visioId);
                int deleted = del.executeUpdate();
                if (deleted > 0) {
                    if (!TestRuntime.isEnabled()) {
                        System.out.println("🗑️ Salon instantané supprimé de la BDD : " + roomName);
                    }
                }
                return deleted > 0;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lors de la suppression du salon instantané : " + e.getMessage());
            return false;
        }
    }

    public List<Visio> listerReunionsDisponibles(int userId) {
        List<Visio> liste = new ArrayList<>();
        String sql = "SELECT DISTINCT v.* FROM VISIO v "
                + "LEFT JOIN VISIO_INVITATIONS vi ON v.id = vi.visio_id "
                + "WHERE (v.createur_id = ? OR vi.employe_id = ? OR (v.statut = 'EN_COURS' AND v.heure_programmee IS NULL)) "
                + "AND v.statut != 'TERMINE' "
                + "ORDER BY v.heure_programmee ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Visio v = new Visio();
                    v.setId(rs.getInt("id"));
                    v.setRoom_name(rs.getString("room_name"));
                    v.setCreateur_id(rs.getInt("createur_id"));
                    v.setStatut(VisioStatut.valueOf(rs.getString("statut")));

                    String typeStr = rs.getString("type_reunion");
                    if (typeStr != null && !typeStr.isBlank()) {
                        v.setType_reunion(ReunionType.fromValeur(typeStr));
                    }

                    LocalDateTime heureProg = lireHeureProgrammee(rs);
                    if (heureProg != null) {
                        v.setHeure_programmee(heureProg);
                    }

                    LocalDateTime heureDeb = timestampVersLocal(rs.getTimestamp("heure_debut"));
                    if (heureDeb != null) {
                        v.setHeure_debut(heureDeb);
                    }

                    liste.add(v);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lors du listing des visioconférences : " + e.getMessage());
        }
        return liste;
    }
}

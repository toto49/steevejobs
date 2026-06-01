package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Enum.ReunionType;
import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.Visio;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VisioDAO {

    public boolean enregistrerSalonInstantane(Visio visio) {
        String sql = "INSERT INTO VISIO (room_name, createur_id, type_reunion, statut, heure_debut) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, visio.getRoom_name());
            stmt.setInt(2, visio.getCreateur_id());
            stmt.setString(3, ReunionType.INSTANTANEE.name());
            stmt.setString(4, VisioStatut.EN_COURS.name());

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lors du stockage du salon instantané : " + e.getMessage());
            return false;
        }
    }

    public boolean planifierReunion(Visio visio, List<Integer> listeIdInvites) {
        String sqlVisio = "INSERT INTO VISIO (room_name, createur_id, statut, heure_programmee) VALUES (?, ?, ?, ?)";
        String sqlInvitation = "INSERT INTO VISIO_INVITATIONS (visio_id, employe_id) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtV = conn.prepareStatement(sqlVisio, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmtV.setString(1, visio.getRoom_name());
                stmtV.setInt(2, visio.getCreateur_id());
                stmtV.setString(3, visio.getStatut().name());
                stmtV.setTimestamp(4, Timestamp.valueOf(visio.getHeure_programmee()));
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

    public int verifierAccesSalon(String roomName, int userId) {
        String query = "SELECT v.statut, v.type_reunion, v.heure_programmee, v.createur_id, " +
                "(SELECT COUNT(*) FROM VISIO_INVITATIONS vi WHERE vi.visio_id = v.id AND vi.employe_id = ?) as est_invite " +
                "FROM VISIO v WHERE v.room_name = ? AND v.statut != 'TERMINE'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setString(2, roomName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    VisioStatut statut = VisioStatut.valueOf(rs.getString("statut"));
                    ReunionType type = ReunionType.fromValeur(rs.getString("type_reunion"));
                    int createurId = rs.getInt("createur_id");
                    int estInvite = rs.getInt("est_invite");
                    Timestamp tsProg = rs.getTimestamp("heure_programmee");
                    if (type == ReunionType.INSTANTANEE) {
                        return 1;
                    }

                    if (statut == VisioStatut.EN_COURS && tsProg == null) {
                        return 1;
                    }

                    if (userId != createurId && estInvite == 0) {
                        return 0;
                    }

                    if (VisioStatut.PROGRAMMEE == statut && tsProg != null) {
                        LocalDateTime heureProgrammee = tsProg.toLocalDateTime();
                        if (LocalDateTime.now().isBefore(heureProgrammee.minusMinutes(10))) {
                            return -1;
                        }
                    }
                    return 1;
                }

            }
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lors du contrôle d'accès : " + e.getMessage());
        }
        return 0;
    }

    public boolean existeEnBdd(String roomName) {
        String sql = "SELECT COUNT(*) FROM VISIO WHERE room_name = ? AND statut != 'TERMINE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
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
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public void ouvrirSalon(String roomName) {
        String sql = "UPDATE VISIO SET statut = ?, heure_debut = CURRENT_TIMESTAMP WHERE room_name = ? AND statut = 'PROGRAMMEE'";
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

    public boolean supprimerSalonInstantane(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            return false;
        }

        String selectSql = "SELECT id FROM VISIO WHERE room_name = ? "
                + "AND (type_reunion = 'INSTANTANEE' OR (type_reunion IS NULL AND heure_programmee IS NULL))";

        try (Connection conn = DatabaseConnection.getConnection()) {
            int visioId = -1;
            try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
                sel.setString(1, roomName.trim());
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
                    System.out.println("🗑️ Salon instantané supprimé de la BDD : " + roomName.trim());
                }
                return deleted > 0;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lors de la suppression du salon instantané : " + e.getMessage());
            return false;
        }
    }

    public boolean cloturerSalon(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            return false;
        }

        String typeSql = "SELECT type_reunion, heure_programmee FROM VISIO "
                + "WHERE room_name = ? AND statut = 'EN_COURS' ORDER BY id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(typeSql)) {
            stmt.setString(1, roomName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return supprimerSalonInstantane(roomName);
                }

                String typeReunion = rs.getString("type_reunion");
                Timestamp heureProgrammee = rs.getTimestamp("heure_programmee");
                if (estSalonInstantane(typeReunion, heureProgrammee)) {
                    return supprimerSalonInstantane(roomName);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lors de la clôture du salon : " + e.getMessage());
            return false;
        }

        terminerSalonPlanifie(roomName);
        return true;
    }

    private static boolean estSalonInstantane(String typeReunion, Timestamp heureProgrammee) {
        return ReunionType.INSTANTANEE.name().equals(typeReunion)
                || (typeReunion == null && heureProgrammee == null);
    }

    public List<Visio> listerReunionsDisponibles(int userId) {
        List<Visio> liste = new ArrayList<>();
        String sql = "SELECT DISTINCT v.* FROM VISIO v " +
                "LEFT JOIN VISIO_INVITATIONS vi ON v.id = vi.visio_id " +
                "WHERE (v.createur_id = ? OR vi.employe_id = ? OR (v.statut = 'EN_COURS' AND v.heure_programmee IS NULL)) " +
                "AND v.statut != 'TERMINE' " +
                "ORDER BY v.heure_programmee ASC";

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

                    Timestamp tsProg = rs.getTimestamp("heure_programmee");
                    if (tsProg != null) v.setHeure_programmee(tsProg.toLocalDateTime());

                    Timestamp tsDeb = rs.getTimestamp("heure_debut");
                    if (tsDeb != null) v.setHeure_debut(tsDeb.toLocalDateTime());

                    liste.add(v);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur DAO lors du listing des visioconférences : " + e.getMessage());
        }
        return liste;
    }
}
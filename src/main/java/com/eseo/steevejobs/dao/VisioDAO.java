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

/**
 * Accès aux données des tables {@code VISIO} et {@code VISIO_INVITATIONS}.
 * <p>
 * La planification de réunion utilise une transaction manuelle ({@code setAutoCommit(false)})
 * avec commit/rollback explicite. Les autres opérations s'exécutent en auto-commit.
 * Les erreurs SQL sont généralement interceptées : journalisation sur {@code System.err}
 * et retour de valeurs par défaut ({@code false}, {@code 0}, {@link Optional#empty()}, liste vide).
 * </p>
 */
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

    /**
     * Enregistre un salon de visioconférence instantané.
     * <p>
     * SQL : {@code INSERT INTO VISIO} avec {@code heure_debut = CURRENT_TIMESTAMP}.
     * </p>
     *
     * @param visio salon à persister (room, créateur, type, statut)
     * @return {@code true} si l'insertion a réussi, {@code false} en cas d'erreur interceptée
     */
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

    /**
     * Planifie une réunion et insère les invitations associées en transaction.
     * <p>
     * SQL : {@code INSERT INTO VISIO} puis batch {@code INSERT INTO VISIO_INVITATIONS}.
     * Rollback en cas d'échec ; commit si toutes les insertions réussissent.
     * </p>
     *
     * @param visio           réunion à planifier
     * @param listeIdInvites  identifiants des employés invités (nullable ou vide)
     * @return {@code true} si la transaction a été validée, {@code false} en cas d'erreur
     */
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

    /**
     * Active automatiquement les salons planifiés dont l'heure programmée est atteinte.
     * <p>
     * SQL : {@code UPDATE VISIO SET statut = EN_COURS, heure_debut = CURRENT_TIMESTAMP}
     * pour les lignes {@code PROGRAMMEE} avec {@code heure_programmee <= aPartirDe}.
     * </p>
     *
     * @param aPartirDe date/heure limite d'éligibilité
     * @return nombre de salons activés ({@code 0} en cas d'erreur interceptée)
     */
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

    /**
     * Charge les informations d'accès à un salon non terminé pour un utilisateur.
     * <p>
     * SQL : lecture de {@code VISIO} avec sous-requête de comptage d'invitation.
     * </p>
     *
     * @param roomName nom du salon
     * @param userId   identifiant de l'utilisateur demandeur
     * @return informations d'accès, ou {@link Optional#empty()} si salon introuvable ou erreur
     */
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

    /**
     * Vérifie l'existence d'un salon actif (non terminé) en base.
     *
     * @param roomName nom du salon
     * @return {@code true} si au moins un enregistrement actif existe, {@code false} sinon ou en cas d'erreur silencieuse
     */
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

    /**
     * Vérifie si un utilisateur est le créateur d'un salon actif.
     *
     * @param roomName nom du salon
     * @param userId   identifiant de l'utilisateur
     * @return {@code true} si l'utilisateur est créateur du salon non terminé, {@code false} sinon
     */
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

    /**
     * Ouvre un salon planifié en passant son statut à {@code EN_COURS}.
     * <p>
     * SQL : {@code UPDATE VISIO} filtré par {@code room_name} et statut {@code PROGRAMMEE}.
     * Les erreurs sont ignorées silencieusement.
     * </p>
     *
     * @param roomName nom du salon
     */
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

    /**
     * Termine un salon planifié en cours et enregistre l'heure de fin.
     * <p>
     * SQL : {@code UPDATE VISIO SET statut = TERMINE, heure_fin = CURRENT_TIMESTAMP}
     * pour les salons {@code EN_COURS} de type {@code PLANIFIEE}.
     * </p>
     *
     * @param roomName nom du salon
     */
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

    /**
     * Charge les métadonnées d'un salon actuellement en cours.
     *
     * @param roomName nom du salon
     * @return informations du salon en cours, ou {@link Optional#empty()} si aucun ou erreur
     */
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

    /**
     * Supprime un salon instantané et ses invitations associées.
     * <p>
     * SQL : sélection par {@code room_name} et type instantané, puis
     * {@code DELETE FROM VISIO_INVITATIONS} et {@code DELETE FROM VISIO}.
     * </p>
     *
     * @param roomName nom du salon
     * @return {@code true} si la suppression a réussi, {@code false} si salon introuvable ou erreur
     */
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

    /**
     * Liste les réunions disponibles pour un utilisateur (créateur, invité ou salons instantanés en cours).
     * <p>
     * SQL : {@code SELECT DISTINCT} sur {@code VISIO} avec jointure {@code VISIO_INVITATIONS},
     * excluant les salons terminés, triés par heure programmée.
     * </p>
     *
     * @param userId identifiant de l'utilisateur
     * @return liste des réunions accessibles (éventuellement vide en cas d'erreur)
     */
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

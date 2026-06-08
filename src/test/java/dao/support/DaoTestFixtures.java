package dao.support;

import com.eseo.steevejobs.dao.MessageDAO;
import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.dao.TicketDAO;
import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.dao.DatabaseConnection;
import com.eseo.steevejobs.model.Enum.StatutDemandeConge;
import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Enum.ReunionType;
import com.eseo.steevejobs.model.Enum.TiersType;
import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.model.Visio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Fixtures d'insertion DAO avec e-mail unique et enregistrement automatique du nettoyage via {@link DaoTestCleanup}.
 * <p>
 * Chaque méthode {@code insert*} persiste une entité minimale et enregistre la suppression inverse pour {@link DaoIntegrationExtension}.
 * </p>
 */
public final class DaoTestFixtures {

    private static final String EMAIL_PREFIX = "dao-test-";

    private DaoTestFixtures() {
    }

    /**
     * Génère une adresse e-mail unique préfixée pour éviter les collisions entre tests.
     *
     * @return adresse au format {@code dao-test-<uuid>@test.local}
     */
    public static String uniqueEmail() {
        return EMAIL_PREFIX + UUID.randomUUID() + "@test.local";
    }

    /**
     * Insère un utilisateur employé de test et enregistre sa suppression.
     *
     * @param userDAO accès persistance utilisateur
     * @return utilisateur créé avec identifiant renseigné
     * @throws SQLException en cas d'échec d'insertion
     */
    public static User insertUser(UserDAO userDAO) throws SQLException {
        User user = new User(
                0,
                "Cleanup",
                "Dao",
                uniqueEmail(),
                "$2a$12$dao.test.hash.placeholder",
                "1 rue Test",
                "EMPLOYE",
                "0600000000",
                "Testeur",
                true
        );
        userDAO.createUser(user);
        int userId = user.getId();
        DaoTestCleanup.register(() -> userDAO.deleteUser(userId));
        return user;
    }

    /**
     * Insère un produit catalogue de test et enregistre sa suppression.
     *
     * @param produitDAO accès persistance produit
     * @return produit créé
     * @throws SQLException en cas d'échec d'insertion
     */
    public static Produit insertProduit(ProduitDAO produitDAO) throws SQLException {
        Produit produit = new Produit(
                0,
                "Produit DAO " + UUID.randomUUID(),
                BigDecimal.valueOf(9.99),
                BigDecimal.valueOf(20),
                10,
                BigDecimal.ONE,
                true,
                2
        );
        produitDAO.createProduit(produit);
        int produitId = produit.getId();
        DaoTestCleanup.register(() -> produitDAO.deleteProduit(produitId));
        return produit;
    }

    /**
     * Insère un tiers client de test et enregistre sa suppression.
     *
     * @param tiersDAO accès persistance tiers
     * @return tiers créé
     * @throws SQLException en cas d'échec d'insertion
     */
    public static Tiers insertTiers(TiersDAO tiersDAO) throws SQLException {
        Tiers tiers = new Tiers();
        tiers.setNom("Société DAO");
        tiers.setPrenom("Jean");
        tiers.setType(TiersType.CLIENT);
        tiers.setEmail(uniqueEmail());
        tiers.setAdresse("2 avenue Test");
        tiers.setTel("0700000000");
        tiers.setSiret("12345678901234");
        tiers.setNum_tva("FR12345678901");
        tiers.setActif(true);
        tiersDAO.createTiers(tiers);
        int tiersId = tiers.getId();
        DaoTestCleanup.register(() -> tiersDAO.deleteTiers(tiersId));
        return tiers;
    }

    /**
     * Insère un ticket en attente lié à un auteur et enregistre sa suppression.
     *
     * @param ticketDAO accès persistance ticket
     * @param auteur    utilisateur auteur du ticket
     * @return ticket créé
     * @throws SQLException en cas d'échec d'insertion
     */
    public static Ticket insertTicket(TicketDAO ticketDAO, User auteur) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setSujet("Sujet DAO");
        ticket.setDescription("Description test DAO");
        ticket.setService("ADMIN");
        ticket.setStatut(StatutTicket.EN_ATTENTE);
        ticket.setDateOuverture(LocalDateTime.now());
        ticket.setAuteur(auteur);
        ticketDAO.createTicket(ticket);
        int ticketId = ticket.getId();
        DaoTestCleanup.register(() -> ticketDAO.deleteTicket(ticketId));
        return ticket;
    }

    /**
     * Insère un message sur un ticket et enregistre sa suppression.
     *
     * @param messageDAO accès persistance message
     * @param auteur     expéditeur
     * @param ticket     ticket parent
     * @return message créé
     * @throws SQLException en cas d'échec d'insertion
     */
    public static Message insertMessage(MessageDAO messageDAO, User auteur, Ticket ticket) throws SQLException {
        Message message = new Message();
        message.setContenu("Message DAO de test");
        message.setAuteur(auteur);
        message.setTicket(ticket);
        message.setDateEnvoi(LocalDateTime.now());
        messageDAO.createMessage(message);
        int messageId = message.getId();
        DaoTestCleanup.register(() -> messageDAO.deleteMessage(messageId));
        return message;
    }

    /**
     * Insère un créneau de type congé pour un utilisateur et enregistre sa suppression.
     *
     * @param planningDAO accès persistance planning
     * @param user        utilisateur associé
     * @return planning créé
     * @throws SQLException en cas d'échec d'insertion
     */
    public static Planning insertPlanning(PlanningDAO planningDAO, User user) throws SQLException {
        Planning planning = new Planning();
        planning.setJourDebut(LocalDateTime.now().plusDays(1));
        planning.setJourFin(LocalDateTime.now().plusDays(1).plusHours(8));
        planning.setType("CONGE");
        planning.setDescription("Planning DAO");
        planning.setCouleur("#7298E0");
        planning.setUser(user);
        planningDAO.createPlanning(planning);
        int planningId = planning.getId();
        DaoTestCleanup.register(() -> planningDAO.deletePlanning(planningId));
        return planning;
    }

    /**
     * Insère une demande de congé en attente et enregistre sa suppression.
     *
     * @param dao     accès persistance demande de congé
     * @param employe demandeur
     * @return demande créée
     * @throws SQLException en cas d'échec d'insertion
     */
    public static DemandeConge insertDemandeConge(com.eseo.steevejobs.dao.DemandeCongeDAO dao, User employe) throws SQLException {
        DemandeConge demande = new DemandeConge();
        demande.setJourDebut(LocalDateTime.now().plusDays(10));
        demande.setJourFin(LocalDateTime.now().plusDays(12));
        demande.setStatut(StatutDemandeConge.EN_ATTENTE);
        demande.setCommentaireEmploye("DAO test");
        demande.setDateDemande(LocalDateTime.now());
        demande.setEmploye(employe);
        dao.create(demande);
        int id = demande.getId();
        DaoTestCleanup.register(() -> dao.delete(id));
        return demande;
    }

    /**
     * Insère une fiche de paie de test et enregistre sa suppression.
     *
     * @param dao     accès persistance fiche de paie
     * @param employe salarié concerné
     * @return fiche créée
     * @throws SQLException en cas d'échec d'insertion
     */
    public static FichePaye insertFichePaye(com.eseo.steevejobs.dao.FichePayeDAO dao, User employe) throws SQLException {
        FichePaye fiche = new FichePaye(0, LocalDateTime.now(), "https://dao.test/paie.pdf", employe);
        dao.createFichePaye(fiche);
        int id = fiche.getId();
        DaoTestCleanup.register(() -> dao.deleteFichePaye(id));
        return fiche;
    }

    /**
     * Enregistre un salon de visioconférence instantané et planifie sa suppression.
     *
     * @param dao      accès persistance visio
     * @param createur utilisateur créateur
     * @return visio persistée
     */
    public static Visio insertVisioInstant(com.eseo.steevejobs.dao.VisioDAO dao, User createur) {
        Visio visio = new Visio();
        visio.setRoom_name("dao-room-" + UUID.randomUUID());
        visio.setCreateur_id(createur.getId());
        visio.setType_reunion(ReunionType.INSTANTANEE);
        visio.setStatut(VisioStatut.EN_COURS);
        dao.enregistrerSalonInstantane(visio);
        String room = visio.getRoom_name();
        DaoTestCleanup.register(() -> dao.supprimerSalonInstantane(room));
        return visio;
    }

    /**
     * Planifie une réunion avec invités et enregistre la suppression SQL du salon.
     *
     * @param dao        accès persistance visio
     * @param createur   organisateur
     * @param inviteIds  identifiants des invités
     * @return visio planifiée
     */
    public static Visio insertVisioPlanifie(com.eseo.steevejobs.dao.VisioDAO dao, User createur, List<Integer> inviteIds) {
        Visio visio = new Visio("dao-planif-" + UUID.randomUUID(), createur.getId(), LocalDateTime.now().plusDays(3));
        dao.planifierReunion(visio, inviteIds);
        String room = visio.getRoom_name();
        DaoTestCleanup.register(() -> {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM VISIO WHERE room_name = ?")) {
                stmt.setString(1, room);
                stmt.executeUpdate();
            }
        });
        return visio;
    }

    /**
     * Sauvegarde une journée type de pointage et enregistre la suppression par utilisateur et date.
     *
     * @param dao  accès persistance heures de travail
     * @param user salarié
     * @param date jour concerné
     * @throws SQLException en cas d'échec d'insertion
     */
    public static void insertHeuresTravail(com.eseo.steevejobs.dao.HeuresTravailDAO dao, User user, LocalDate date) throws SQLException {
        dao.sauvegarder(user.getId(), date, LocalTime.of(9, 0), LocalTime.of(12, 0),
                LocalTime.of(14, 0), LocalTime.of(17, 0), LocalTime.of(6, 0));
        int userId = user.getId();
        DaoTestCleanup.register(() -> {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "DELETE FROM HEURES_TRAVAIL WHERE id_user = ? AND date_jour = ?")) {
                stmt.setInt(1, userId);
                stmt.setDate(2, Date.valueOf(date));
                stmt.executeUpdate();
            }
        });
    }

    /**
     * Crée une permission éphémère, l'associe au rôle et enregistre le retrait complet.
     *
     * @param dao  implémentation DAO des permissions
     * @param role nom du rôle cible
     * @return identifiant de la permission créée
     */
    public static int insertPermission(com.eseo.steevejobs.dao.PermissionDAOImpl dao, String role) {
        String code = "DAO_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        dao.createPermission(code, "Permission test DAO");
        int permId = dao.getAllPermissions().stream()
                .filter(p -> code.equals(p.getCodeAction()))
                .findFirst()
                .orElseThrow()
                .getId();
        dao.insertRolePermission(role, permId);
        DaoTestCleanup.register(() -> {
            dao.deleteRolePermission(role, permId);
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM PERMISSION WHERE id_permission = ?")) {
                stmt.setInt(1, permId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return permId;
    }
}

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.TicketServiceImpl;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour TicketServiceImpl.
 *
 * STRATÉGIE : Le système de ticketing gère la communication interne.
 * Les règles critiques : statut initial correct, messages liés au bon ticket,
 * formatage des dates, et comportement des listes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketServiceImplTest {

    private TicketServiceImpl ticketService;
    private UserDAO userDAO;

    private static User utilisateurTest;
    private static int idTicketCree = -1;

    @BeforeAll
    static void setUpGlobal() throws SQLException {
        UserDAO dao = new UserDAO();
        List<User> users = dao.findAll();
        if (!users.isEmpty()) {
            utilisateurTest = users.get(0);
        } else {
            throw new IllegalStateException("Aucun utilisateur en BDD pour les tests de ticket");
        }
    }

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        ticketService = new TicketServiceImpl();
    }

    @AfterEach
    void tearDown() {
        // Note : On ne supprime pas en BDD pour les tickets car ils cascadent les messages.
        // En production, un service de cleanup devrait exister. Ici on accepte les résidus de test.
        idTicketCree = -1;
    }

    // Helper : crée un ticket de test
    private Ticket creerTicketDeTest(String sujet) {
        Ticket ticket = new Ticket();
        ticket.setSujet(sujet + " - " + System.currentTimeMillis());
        ticket.setDescription("Description du ticket de test JUnit");
        ticket.setService("ADMIN");
        ticket.setAuteur(utilisateurTest);
        return ticket;
    }

    // =========================================================
    // TEST 1 — Cas Nominal : Créer un ticket, statut initial = EN_ATTENTE
    // =========================================================

    /**
     * SCÉNARIO : On crée un nouveau ticket et on vérifie son statut initial.
     * POURQUOI : Un ticket DOIT démarrer en EN_ATTENTE. Si le statut est mauvais
     *            dès la création, les filtres d'affichage ("tickets en cours")
     *            ne fonctionneront pas et les admins manqueront des demandes.
     * ASSERTION : Statut == EN_ATTENTE et ID > 0 après création.
     */
    @Test
    @Order(1)
    @DisplayName("Cas nominal - Nouveau ticket doit avoir le statut EN_ATTENTE")
    void creerTicket_nouveauTicket_doitAvoirStatutEnAttente() {
        // Arrange
        Ticket ticket = creerTicketDeTest("Test Statut Initial");

        // Act
        Ticket ticketCree = ticketService.creerTicket(ticket);
        idTicketCree = ticketCree.getId();

        // Assert
        assertNotNull(ticketCree, "Le ticket créé ne doit pas être null");
        assertTrue(ticketCree.getId() > 0, "Le ticket doit avoir un ID positif");
        assertEquals(StatutTicket.EN_ATTENTE, ticketCree.getStatut(),
                "Un nouveau ticket doit obligatoirement démarrer en statut EN_ATTENTE");
    }

    // =========================================================
    // TEST 2 — Cas Nominal : La date d'ouverture est automatiquement renseignée
    // =========================================================

    /**
     * SCÉNARIO : On crée un ticket sans définir de date d'ouverture manuellement.
     * POURQUOI : La date d'ouverture sert à afficher "Ouvert il y a X jours/heures"
     *            dans le calendrier des tickets. Si elle est null, le tri et
     *            l'affichage planteront avec une NullPointerException.
     * ASSERTION : dateOuverture est non-null et approximativement = maintenant.
     */
    @Test
    @Order(2)
    @DisplayName("Cas nominal - La date d'ouverture doit être automatiquement renseignée à la création")
    void creerTicket_dateOuvertureDoit_etreDefinie() {
        // Arrange
        Ticket ticket = creerTicketDeTest("Test Date Ouverture");
        LocalDateTime avantCreation = LocalDateTime.now().minusSeconds(2);

        // Act
        Ticket ticketCree = ticketService.creerTicket(ticket);
        idTicketCree = ticketCree.getId();

        // Assert
        assertNotNull(ticketCree.getDateOuverture(),
                "La date d'ouverture ne doit pas être null");
        assertTrue(
                ticketCree.getDateOuverture().isAfter(avantCreation),
                "La date d'ouverture doit être postérieure au moment juste avant la création"
        );
    }

    // =========================================================
    // TEST 3 — Cas Nominal : Changer le statut d'un ticket
    // =========================================================

    /**
     * SCÉNARIO : On crée un ticket (EN_ATTENTE), puis on le ferme (FERME).
     * POURQUOI : Le workflow de résolution (EN_ATTENTE → EN_COURS → FERME) est
     *            le cœur fonctionnel du ticketing. Un bug ici empêcherait les
     *            admins de marquer les tickets comme résolus.
     * ASSERTION : Le statut du ticket récupéré depuis la BDD est bien FERME.
     */
    @Test
    @Order(3)
    @DisplayName("Cas nominal - Changer le statut d'un ticket vers FERME doit fonctionner")
    void changerStatut_versStatutFerme_doitPersister() {
        // Arrange
        Ticket ticket = creerTicketDeTest("Test Changement Statut");
        Ticket ticketCree = ticketService.creerTicket(ticket);
        idTicketCree = ticketCree.getId();

        // Act
        Ticket ticketMisAJour = ticketService.changerStatut(ticketCree.getId(), StatutTicket.FERME);

        // Assert
        assertNotNull(ticketMisAJour, "Le ticket après changement de statut ne doit pas être null");
        assertEquals(StatutTicket.FERME, ticketMisAJour.getStatut(),
                "Le statut doit être FERME après la mise à jour");
    }

    // =========================================================
    // TEST 4 — Cas Erreur : Changer le statut d'un ticket inexistant
    // =========================================================

    /**
     * SCÉNARIO : On appelle changerStatut() avec un ID de ticket qui n'existe pas.
     * POURQUOI : Un contrôleur pourrait recevoir un ID invalide (ex: suppression
     *            concurrente, race condition). Sans gestion, le DAO retournerait
     *            faux et aucune exception claire ne serait levée.
     * ASSERTION : RuntimeException levée pour un ID de ticket inexistant.
     */
    @Test
    @Order(4)
    @DisplayName("Cas erreur - Changer le statut d'un ticket inexistant doit lever une exception")
    void changerStatut_idInexistant_doitLeverException() {
        // Act & Assert — ID = 999999 ne devrait pas exister
        assertThrows(
                RuntimeException.class,
                () -> ticketService.changerStatut(999999, StatutTicket.EN_COURS),
                "Modifier le statut d'un ticket inexistant doit lever une exception"
        );
    }

    // =========================================================
    // TEST 5 — Cas Nominal : Ajouter un message passe le statut à EN_COURS
    // =========================================================

    /**
     * SCÉNARIO : On crée un ticket EN_ATTENTE, puis on y ajoute le premier message.
     * POURQUOI : La règle métier dans TicketServiceImpl dit : "Si le ticket est
     *            EN_ATTENTE et qu'un message est ajouté, passer en EN_COURS".
     *            Ce test vérifie cette transition automatique de statut.
     * ASSERTION : Après ajout du message, le ticket est EN_COURS.
     */
    @Test
    @Order(5)
    @DisplayName("Règle métier - Ajouter un message à un ticket EN_ATTENTE doit le passer EN_COURS")
    void ajouterMessage_surTicketEnAttente_doitPasserEnCours() {
        // Arrange
        Ticket ticket = creerTicketDeTest("Test Transition Statut");
        Ticket ticketCree = ticketService.creerTicket(ticket);
        idTicketCree = ticketCree.getId();

        assertEquals(StatutTicket.EN_ATTENTE, ticketCree.getStatut(),
                "Précondition : le ticket doit démarrer EN_ATTENTE");

        Message message = new Message();
        message.setContenu("Premier message de test");
        message.setAuteur(utilisateurTest);
        message.setDateEnvoi(LocalDateTime.now());

        // Act
        ticketService.ajouterMessage(ticketCree.getId(), message);

        // Assert — Recharge le ticket depuis la BDD pour vérifier le statut persisté
        Ticket ticketRecharge = ticketService.getTicketById(ticketCree.getId());
        assertEquals(StatutTicket.EN_COURS, ticketRecharge.getStatut(),
                "Après le premier message, le ticket doit automatiquement passer EN_COURS");
    }

    // =========================================================
    // TEST 6 — Cas Nominal : getAllTickets retourne une liste non-null
    // =========================================================

    /**
     * SCÉNARIO : On appelle getAllTickets() et on vérifie le retour.
     * POURQUOI : Cette méthode alimente la liste de tickets dans l'interface.
     *            Un retour null provoque un crash immédiat dans TicketsListController
     *            (stream() sur null = NullPointerException).
     * ASSERTION : La liste est non-null et trié (tickets les plus récents en premier).
     */
    @Test
    @Order(6)
    @DisplayName("Cas nominal - getAllTickets doit retourner une liste non-null et triée")
    void getAllTickets_doitRetournerListeNonNullTriee() {
        // Act
        List<Ticket> tickets = ticketService.getAllTickets();

        // Assert
        assertNotNull(tickets, "getAllTickets ne doit jamais retourner null");

        // Vérifie le tri : chaque ticket doit avoir une date >= au suivant
        for (int i = 0; i < tickets.size() - 1; i++) {
            LocalDateTime date1 = tickets.get(i).getDateOuverture();
            LocalDateTime date2 = tickets.get(i + 1).getDateOuverture();
            if (date1 != null && date2 != null) {
                assertTrue(
                        !date1.isBefore(date2),
                        "Les tickets doivent être triés du plus récent au plus ancien"
                );
            }
        }
    }

    // =========================================================
    // TEST 7 — Cas Nominal : formatTicketDate avec une date valide
    // =========================================================

    /**
     * SCÉNARIO : On formate une date connue et on vérifie le résultat.
     * POURQUOI : Cette méthode utilitaire est utilisée pour afficher les dates
     *            dans la liste des tickets. Un format incorrect rendrait
     *            l'interface illisible pour les utilisateurs.
     * ASSERTION : La date est au format "dd/MM/yyyy HH:mm".
     */
    @Test
    @Order(7)
    @DisplayName("Cas nominal - formatTicketDate doit retourner le bon format de date")
    void formatTicketDate_dateValide_doitRetournerBonFormat() {
        // Arrange
        LocalDateTime dateTest = LocalDateTime.of(2025, 6, 15, 14, 30);

        // Act
        String resultat = ticketService.formatTicketDate(dateTest);

        // Assert
        assertNotNull(resultat, "Le formatage ne doit pas retourner null");
        assertEquals("15/06/2025 14:30", resultat,
                "Le format de date doit être dd/MM/yyyy HH:mm");
    }

    // =========================================================
    // TEST 8 — Cas Limite : formatTicketDate avec null doit retourner "N/A"
    // =========================================================

    /**
     * SCÉNARIO : On appelle formatTicketDate(null).
     * POURQUOI : Si un ticket en BDD a une date null (import de données ancien,
     *            bug de migration), la méthode ne doit pas crasher. Elle retourne
     *            "N/A" pour indiquer l'absence de date sans NullPointerException.
     * ASSERTION : Retourne "N/A" pour une date null.
     */
    @Test
    @Order(8)
    @DisplayName("Cas limite - formatTicketDate avec null doit retourner 'N/A'")
    void formatTicketDate_dateNull_doitRetournerNA() {
        // Act
        String resultat = ticketService.formatTicketDate(null);

        // Assert
        assertEquals("N/A", resultat,
                "Une date null doit être affichée 'N/A' et non provoquer une NullPointerException");
    }
}

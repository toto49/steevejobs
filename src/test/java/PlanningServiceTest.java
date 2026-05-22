import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.PlanningService;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour PlanningService.
 *
 * STRATÉGIE : Le PlanningService a des règles de validation dates importantes
 * (fin après début, champs obligatoires, utilisateur requis). On cible ces
 * règles métier car elles sont silencieuses si non vérifiées.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlanningServiceTest {

    private PlanningService planningService;
    private PlanningDAO planningDAO;
    private UserDAO userDAO;

    // Un utilisateur de test dont on a besoin pour créer des plannings
    private static User utilisateurTest;
    private static int idPlanningCree = -1;

    @BeforeAll
    static void setUpGlobal() throws SQLException {
        // Récupère ou crée un utilisateur de test réutilisable
        UserDAO dao = new UserDAO();
        List<User> users = dao.findAll();
        if (!users.isEmpty()) {
            utilisateurTest = users.get(0); // Prend le premier user disponible
        } else {
            throw new IllegalStateException("Aucun utilisateur en BDD pour les tests de planning");
        }
    }

    @BeforeEach
    void setUp() {
        planningDAO = new PlanningDAO();
        planningService = new PlanningService(planningDAO);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (idPlanningCree > 0) {
            try {
                planningDAO.deletePlanning(idPlanningCree);
            } catch (Exception ignored) {}
            idPlanningCree = -1;
        }
    }

    // =========================================================
    // TEST 1 — Cas Nominal : Ajout d'un planning valide
    // =========================================================

    /**
     * SCÉNARIO : On crée un planning avec des dates cohérentes (fin > début).
     * POURQUOI : Valide le chemin heureux complet. Si ce test échoue, aucun
     *            employé ne peut créer d'événement dans le calendrier.
     * ASSERTION : Pas d'exception levée et le planning est persisté.
     */
    @Test
    @Order(1)
    @DisplayName("Cas nominal - Ajout d'un planning valide ne doit pas lever d'exception")
    void ajouterPlanning_planningValide_doitReussir() throws SQLException {
        // Arrange
        LocalDateTime debut = LocalDateTime.now().plusHours(1);
        LocalDateTime fin   = LocalDateTime.now().plusHours(3);

        Planning planning = new Planning(
                0, debut, fin, "Réunion", "Réunion d'équipe sprint", "#7298E0",
                utilisateurTest
        );

        // Act — Ne doit pas lancer d'exception
        assertDoesNotThrow(() -> {
            planningService.ajouterPlanning(planning);
            idPlanningCree = planning.getId();
        }, "L'ajout d'un planning valide ne doit pas provoquer d'exception");

        // Assert
        assertTrue(planning.getId() > 0, "Le planning doit recevoir un ID après persistance");
    }

    // =========================================================
    // TEST 2 — Règle Métier : Date de fin antérieure à la date de début
    // =========================================================

    /**
     * SCÉNARIO : On crée un planning où fin < début (incohérence temporelle).
     * POURQUOI : Un planning avec une fin avant le début s'afficherait avec
     *            une durée négative dans le calendrier, causant des bugs
     *            d'affichage et de calcul de durée. C'est une règle fondamentale.
     * ASSERTION : IllegalArgumentException levée avec message sur les dates.
     */
    @Test
    @Order(2)
    @DisplayName("Règle métier - Date de fin avant date de début doit lever une exception")
    void ajouterPlanning_finAvantDebut_doitLeverException() {
        // Arrange — Fin AVANT début : incohérent
        LocalDateTime debut = LocalDateTime.now().plusHours(5);
        LocalDateTime fin   = LocalDateTime.now().plusHours(2); // Fin < Début !

        Planning planningIncoherent = new Planning(
                0, debut, fin, "Cours", "Cours JavaFX", "#FF0000",
                utilisateurTest
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> planningService.ajouterPlanning(planningIncoherent),
                "Une date de fin antérieure à la date de début doit être refusée"
        );
    }

    // =========================================================
    // TEST 3 — Cas Limite : Date de fin égale à la date de début
    // =========================================================

    /**
     * SCÉNARIO : On crée un planning où début == fin (durée nulle).
     * POURQUOI : Un événement instantané (0 seconde) est techniquement valide
     *            pour la règle "fin >= début", mais il s'afficherait
     *            comme un point invisible dans le calendrier. Ce test documente
     *            le comportement attendu (autoriser ou refuser).
     *            Ici on vérifie que le service l'accepte (non bloqué).
     * ASSERTION : Pas d'exception levée pour fin == début.
     */
    @Test
    @Order(3)
    @DisplayName("Cas limite - Date de fin égale à la date de début ne doit pas lever d'exception")
    void ajouterPlanning_finEgaleDebut_doitEtreAccepte() {
        // Arrange
        LocalDateTime dateIdentique = LocalDateTime.now().plusDays(1);

        Planning planningInstant = new Planning(
                0, dateIdentique, dateIdentique, "Rappel", "Rappel ponctuel", "#FFCC00",
                utilisateurTest
        );

        // Act & Assert — fin == début est à la frontière, ne doit pas planter
        assertDoesNotThrow(
                () -> {
                    planningService.ajouterPlanning(planningInstant);
                    if (planningInstant.getId() > 0) {
                        idPlanningCree = planningInstant.getId();
                    }
                },
                "Un planning avec fin == début doit être accepté (durée zéro)"
        );
    }

    // =========================================================
    // TEST 4 — Règle Métier : Type de planning obligatoire
    // =========================================================

    /**
     * SCÉNARIO : On crée un planning avec un type null.
     * POURQUOI : Le type est utilisé pour colorer et filtrer les événements dans
     *            le calendrier (Cours, Réunion, Congé...). Un type null causerait
     *            une NullPointerException lors du rendu graphique du calendrier.
     * ASSERTION : IllegalArgumentException levée pour type null.
     */
    @Test
    @Order(4)
    @DisplayName("Règle métier - Type de planning null doit lever une exception")
    void ajouterPlanning_typeNull_doitLeverException() {
        // Arrange
        LocalDateTime debut = LocalDateTime.now().plusDays(2);
        LocalDateTime fin   = LocalDateTime.now().plusDays(2).plusHours(2);

        Planning planningsSansType = new Planning(
                0, debut, fin, null, "Sans type", "#AAAAAA", // type = null
                utilisateurTest
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> planningService.ajouterPlanning(planningsSansType),
                "Un type null doit être refusé pour éviter les NPE dans le calendrier"
        );
    }

    // =========================================================
    // TEST 5 — Règle Métier : Utilisateur obligatoire
    // =========================================================

    /**
     * SCÉNARIO : On crée un planning sans utilisateur associé (null).
     * POURQUOI : Le planning est lié à un utilisateur par une FK en BDD.
     *            Un planning sans utilisateur violerait la contrainte référentielle
     *            et la requête SQL échouerait avec une erreur cryptique.
     *            Le service doit détecter ça proprement.
     * ASSERTION : IllegalArgumentException levée pour user null.
     */
    @Test
    @Order(5)
    @DisplayName("Règle métier - Planning sans utilisateur doit lever une exception")
    void ajouterPlanning_utilisateurNull_doitLeverException() {
        // Arrange
        LocalDateTime debut = LocalDateTime.now().plusDays(3);
        LocalDateTime fin   = LocalDateTime.now().plusDays(3).plusHours(1);

        Planning planningsSansUser = new Planning(
                0, debut, fin, "Vacances", "Vacances d'été", "#4CAF50",
                null // Utilisateur null = invalide
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> planningService.ajouterPlanning(planningsSansUser),
                "Un planning sans utilisateur doit être refusé"
        );
    }

    // =========================================================
    // TEST 6 — Cas Nominal : Suppression d'un planning existant
    // =========================================================

    /**
     * SCÉNARIO : On crée un planning, on le supprime, puis on vérifie
     *            qu'il n'est plus récupérable.
     * POURQUOI : La suppression doit être atomique et irréversible. Si ce test
     *            échoue, le calendrier afficherait des événements "fantômes" que
     *            les employés ne peuvent plus supprimer.
     * ASSERTION : Le planning n'existe plus après suppression.
     */
    @Test
    @Order(6)
    @DisplayName("Cas nominal - Supprimer un planning doit le retirer de la BDD")
    void deletePlanning_planningExistant_doitEtreSupprimeCorrectement() throws SQLException {
        // Arrange — Crée un planning à supprimer
        LocalDateTime debut = LocalDateTime.now().plusDays(5);
        LocalDateTime fin   = LocalDateTime.now().plusDays(5).plusHours(2);

        Planning planning = new Planning(
                0, debut, fin, "Cours", "A supprimer", "#FF5733",
                utilisateurTest
        );
        planningService.ajouterPlanning(planning);
        int idASupprimer = planning.getId();

        // Act
        assertDoesNotThrow(
                () -> planningService.supprimerPlanning(idASupprimer),
                "La suppression d'un planning existant ne doit pas lever d'exception"
        );

        // Assert — Le planning n'existe plus
        Planning planningApresSupp = planningDAO.getById(idASupprimer);
        assertNull(planningApresSupp,
                "Le planning supprimé ne doit plus être trouvable en BDD");

        idPlanningCree = -1; // Déjà supprimé, pas besoin de le refaire dans tearDown
    }

    // =========================================================
    // TEST 7 — Cas Nominal : Récupérer les plannings d'un utilisateur
    // =========================================================

    /**
     * SCÉNARIO : On récupère la liste des plannings pour un utilisateur donné.
     * POURQUOI : Cette méthode alimente le calendrier JavaFX. Elle ne doit jamais
     *            retourner null (sinon forEach crashe), même si l'utilisateur n'a
     *            aucun événement planifié.
     * ASSERTION : Retourne une liste non-null (potentiellement vide).
     */
    @Test
    @Order(7)
    @DisplayName("Cas nominal - Récupérer les plannings d'un user doit retourner une liste non-null")
    void findByUserId_utilisateurExistant_doitRetournerListeNonNull() throws SQLException {
        // Act
        List<Planning> plannings = planningService.obtenirPlanningsParUtilisateur(utilisateurTest.getId());

        // Assert
        assertNotNull(plannings,
                "La liste des plannings ne doit jamais être null pour éviter les NPE dans le calendrier");
    }
}

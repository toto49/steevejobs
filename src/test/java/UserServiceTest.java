import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.UserService;
import org.junit.jupiter.api.*;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour UserService.
 *
 * STRATÉGIE : UserService est la classe la plus critique du projet car elle
 * gère l'authentification, le blocage de compte, et le hashage des mots de passe.
 * Un bug ici = faille de sécurité.
 *
 * Approche : Tests d'intégration avec la BDD de test. On crée des utilisateurs
 * temporaires et on les nettoie systématiquement après chaque test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    private UserService userService;
    private UserDAO userDAO;

    // Email unique par run de test pour éviter les conflits
    private static final String EMAIL_TEST = "junit.test." + System.currentTimeMillis() + "@test.fr";
    private static int idUserCree = -1;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        userService = new UserService(userDAO);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (idUserCree > 0) {
            try {
                userDAO.deleteUser(idUserCree);
            } catch (Exception ignored) {}
            idUserCree = -1;
        }
    }

    // =========================================================
    // TEST 1 — Sécurité : Le hashage SHA-256 est déterministe
    // =========================================================

    /**
     * SCÉNARIO : On hashe deux fois le même mot de passe et on compare.
     * POURQUOI : Si le hashage n'est pas déterministe, l'authentification
     *            échouera TOUJOURS car le hash stocké ne correspondra jamais
     *            au hash calculé au login. C'est le test de base de toute sécurité.
     * ASSERTION : hashPassword("monMotDePasse") == hashPassword("monMotDePasse").
     */
    @Test
    @Order(1)
    @DisplayName("Sécurité - Le hashage du même mot de passe doit toujours produire le même résultat")
    void hashPassword_memeMdp_doitProduireLeMemeHash() {
        // Arrange
        String motDePasse = "MotDePasseTest123!";

        // Act
        String hash1 = userService.hashPassword(motDePasse);
        String hash2 = userService.hashPassword(motDePasse);

        // Assert
        assertNotNull(hash1, "Le hash ne doit pas être null");
        assertFalse(hash1.isEmpty(), "Le hash ne doit pas être vide");
        assertEquals(hash1, hash2, "Le hashage doit être déterministe");
    }

    // =========================================================
    // TEST 2 — Sécurité : Deux mots de passe différents = deux hash différents
    // =========================================================

    /**
     * SCÉNARIO : On hashe "abc" et "ABC" et on vérifie qu'ils sont différents.
     * POURQUOI : Un algorithme de hash qui produit le même résultat pour des
     *            entrées différentes est inutilisable pour la sécurité. Ce test
     *            confirme que SHA-256 est sensible à la casse et aux variations.
     * ASSERTION : hash("abc") != hash("ABC").
     */
    @Test
    @Order(2)
    @DisplayName("Sécurité - Des mots de passe différents doivent produire des hashs différents")
    void hashPassword_mdpDifferents_doitProduireHashDifferents() {
        // Act
        String hash1 = userService.hashPassword("motdepasse123");
        String hash2 = userService.hashPassword("MotDePasse123"); // Casse différente

        // Assert
        assertNotEquals(hash1, hash2,
                "Deux mots de passe différents doivent produire des hashs distincts");
    }

    // =========================================================
    // TEST 3 — Règle Métier : Email obligatoire à la création
    // =========================================================

    /**
     * SCÉNARIO : On tente de créer un utilisateur avec un email null.
     * POURQUOI : L'email est l'identifiant unique de connexion. Sans email,
     *            l'utilisateur ne pourra jamais se connecter et la contrainte
     *            UNIQUE en BDD sera violée silencieusement.
     * ASSERTION : IllegalArgumentException levée avant tout appel au DAO.
     */
    @Test
    @Order(3)
    @DisplayName("Règle métier - Créer un utilisateur sans email doit lever une exception")
    void createUser_emailNull_doitLeverException() {
        // Arrange
        User userSansEmail = new User();
        userSansEmail.setNom("Test");
        userSansEmail.setEmail(null); // Email null = invalide
        userSansEmail.setPasswordHash(userService.hashPassword("password"));
        userSansEmail.setRole("Employe");
        userSansEmail.setPoste("Dev");
        userSansEmail.setActif(true);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(userSansEmail),
                "Un email null doit être refusé par la validation"
        );
    }

    // =========================================================
    // TEST 4 — Règle Métier : Rôle obligatoire à la création
    // =========================================================

    /**
     * SCÉNARIO : On tente de créer un utilisateur avec un rôle vide.
     * POURQUOI : Le rôle pilote TOUT le système de permissions. Un utilisateur
     *            sans rôle n'aurait accès à aucun module de l'application et
     *            les requêtes de permissions retourneraient des résultats erronés.
     * ASSERTION : IllegalArgumentException levée avec message sur le rôle.
     */
    @Test
    @Order(4)
    @DisplayName("Règle métier - Créer un utilisateur sans rôle doit lever une exception")
    void createUser_rolVide_doitLeverException() {
        // Arrange
        User userSansRole = new User();
        userSansRole.setNom("Test");
        userSansRole.setEmail("sans.role@test.fr");
        userSansRole.setPasswordHash(userService.hashPassword("password"));
        userSansRole.setRole(""); // Rôle vide = invalide
        userSansRole.setPoste("Dev");
        userSansRole.setActif(true);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(userSansRole),
                "Un rôle vide doit être refusé"
        );
    }

    // =========================================================
    // TEST 5 — Cas Nominal : Création et récupération d'un utilisateur
    // =========================================================

    /**
     * SCÉNARIO : On crée un utilisateur valide et on le récupère par son ID.
     * POURQUOI : Ce test de bout en bout valide que la persistance fonctionne
     *            correctement. Il sert aussi de base pour tous les autres tests
     *            qui nécessitent un utilisateur existant.
     * ASSERTION : L'utilisateur récupéré a le même email que celui créé.
     */
    @Test
    @Order(5)
    @DisplayName("Cas nominal - Créer puis récupérer un utilisateur doit fonctionner")
    void createUser_etGetById_doitPersisterEtRecupererCorrectement() throws SQLException {
        // Arrange
        User nouvelUser = new User();
        nouvelUser.setNom("Dupont");
        nouvelUser.setPrenom("Jean");
        nouvelUser.setEmail(EMAIL_TEST);
        nouvelUser.setPasswordHash(userService.hashPassword("TestPassword1!"));
        nouvelUser.setRole("Employe");
        nouvelUser.setPoste("Développeur");
        nouvelUser.setActif(true);

        // Act
        userService.createUser(nouvelUser);
        idUserCree = nouvelUser.getId();

        User userRecupere = userService.getUserById(idUserCree);

        // Assert
        assertNotNull(userRecupere, "L'utilisateur créé doit être récupérable par son ID");
        assertEquals(EMAIL_TEST, userRecupere.getEmail(),
                "L'email doit être préservé après la persistance");
        assertEquals("Dupont", userRecupere.getNom(),
                "Le nom doit être préservé après la persistance");
    }

    // =========================================================
    // TEST 6 — Règle Métier : Duplication d'email interdite
    // =========================================================

    /**
     * SCÉNARIO : On crée un utilisateur, puis on tente d'en créer un second
     *            avec le même email.
     * POURQUOI : L'email est la clé d'authentification. Deux comptes avec le
     *            même email créeraient une ambiguïté au login (quel compte connecter ?).
     *            La contrainte UNIQUE est en BDD, mais le Service doit la vérifier
     *            AVANT pour renvoyer un message d'erreur lisible.
     * ASSERTION : IllegalArgumentException avec message explicite sur le doublon.
     */
    @Test
    @Order(6)
    @DisplayName("Règle métier - Créer deux utilisateurs avec le même email doit lever une exception")
    void createUser_emailDuplique_doitLeverException() throws SQLException {
        // Arrange — Crée le premier utilisateur
        String emailDuplique = "doublon." + System.currentTimeMillis() + "@test.fr";

        User user1 = new User();
        user1.setNom("User1"); user1.setPrenom("Test");
        user1.setEmail(emailDuplique);
        user1.setPasswordHash(userService.hashPassword("pass1"));
        user1.setRole("Employe"); user1.setPoste("Dev"); user1.setActif(true);
        userService.createUser(user1);
        idUserCree = user1.getId();

        // Arrange — Prépare le doublon
        User user2 = new User();
        user2.setNom("User2"); user2.setPrenom("Test");
        user2.setEmail(emailDuplique); // MÊME email
        user2.setPasswordHash(userService.hashPassword("pass2"));
        user2.setRole("Employe"); user2.setPoste("Dev"); user2.setActif(true);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(user2),
                "Un email déjà utilisé doit être refusé"
        );
    }

    // =========================================================
    // TEST 7 — Cas Erreur : Récupérer un utilisateur avec ID invalide
    // =========================================================

    /**
     * SCÉNARIO : On appelle getUserById(0) avec un ID invalide.
     * POURQUOI : Un contrôleur pourrait appeler cette méthode avec un ID non
     *            initialisé (0 par défaut en Java). Sans garde-fou, ça générerait
     *            une requête SQL "WHERE id = 0" qui retourne null silencieusement,
     *            provoquant une NullPointerException en cascade dans l'interface.
     * ASSERTION : IllegalArgumentException lancée immédiatement, avant tout accès BDD.
     */
    @Test
    @Order(7)
    @DisplayName("Cas erreur - Récupérer un utilisateur avec ID invalide (0) doit lever une exception")
    void getUserById_idInvalide_doitLeverException() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(0),
                "Un ID égal à 0 doit être refusé"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(-5),
                "Un ID négatif doit être refusé"
        );
    }

    // =========================================================
    // TEST 8 — Sécurité : Authentification avec email inexistant retourne null
    // =========================================================

    /**
     * SCÉNARIO : On tente de s'authentifier avec un email qui n'existe pas en BDD.
     * POURQUOI : La méthode authenticate() doit retourner null (et non lancer
     *            une exception) quand l'utilisateur n'existe pas. Ce comportement
     *            est attendu par le BienvenueController pour afficher le bon
     *            message d'erreur sans crash.
     * ASSERTION : Retourne null sans exception pour un email inconnu.
     */
    @Test
    @Order(8)
    @DisplayName("Sécurité - Authentification avec un email inexistant doit retourner null")
    void authenticate_emailInexistant_doitRetournerNull() throws Exception {
        // Arrange
        String emailFantome = "utilisateur.qui.nexiste.pas@jamais.fr";
        String hashMdp = userService.hashPassword("nimportequoi");

        // Act
        User resultat = userService.authenticate(emailFantome, hashMdp);

        // Assert
        assertNull(resultat,
                "Un email inconnu doit retourner null, pas lancer une exception");
    }
}

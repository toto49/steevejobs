import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.TiersType;
import com.eseo.steevejobs.service.TiersService;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour TiersService.
 *
 * STRATÉGIE : Le TiersService valide des données légales et commerciales
 * importantes (SIRET 14 chiffres, format email, unicité). Une erreur dans
 * ces validations génère des factures et devis avec des données incorrectes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TiersServiceTest {

    private TiersService tiersService;
    private TiersDAO tiersDAO;

    private static int idTiersCree = -1;

    @BeforeEach
    void setUp() {
        tiersDAO = new TiersDAO();
        tiersService = new TiersService(tiersDAO);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (idTiersCree > 0) {
            try {
                tiersDAO.deleteTiers(idTiersCree);
            } catch (Exception ignored) {}
            idTiersCree = -1;
        }
    }

    // =========================================================
    // TEST 1 — Cas Nominal : Ajout d'un tiers client valide
    // =========================================================

    /**
     * SCÉNARIO : On crée un client avec toutes les données légalement correctes.
     * POURQUOI : Valide le chemin complet pour la création d'un client.
     *            Sans ce test passant, aucun devis ou facture ne peut être créé.
     * ASSERTION : Le tiers reçoit un ID positif après persistance.
     */
    @Test
    @Order(1)
    @DisplayName("Cas nominal - Ajout d'un tiers client valide doit réussir")
    void ajouterTiers_tiersValide_doitPersisterAvecId() throws SQLException {
        // Arrange
        Tiers client = new Tiers(
                0,
                "Entreprise JUnit SA",
                null,
                TiersType.CLIENT,
                "junit." + System.currentTimeMillis() + "@entreprise.fr",
                "1 rue des Tests, 75001 Paris",
                "0102030405",
                null, null
        );

        // Act
        tiersService.ajouterTiers(client);
        idTiersCree = client.getId();

        // Assert
        assertTrue(client.getId() > 0,
                "Le tiers créé doit avoir un ID positif attribué par la BDD");
    }

    // =========================================================
    // TEST 2 — Règle Métier : Nom du tiers obligatoire
    // =========================================================

    /**
     * SCÉNARIO : On tente de créer un tiers sans nom.
     * POURQUOI : Le nom apparaît sur chaque facture et devis. Un tiers sans nom
     *            rendrait les documents commerciaux invalides légalement et
     *            incompréhensibles pour les clients.
     * ASSERTION : IllegalArgumentException levée pour nom vide.
     */
    @Test
    @Order(2)
    @DisplayName("Règle métier - Nom du tiers vide doit lever une exception")
    void ajouterTiers_nomVide_doitLeverException() {
        // Arrange
        Tiers tierssSansNom = new Tiers(
                0,
                "", // Nom vide = invalide
                "Jean",
                TiersType.CLIENT,
                "test@test.fr",
                "1 rue Test",
                "0102030405",
                null, null
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> tiersService.ajouterTiers(tierssSansNom),
                "Un nom vide doit être refusé pour garantir l'intégrité des documents commerciaux"
        );
    }

    // =========================================================
    // TEST 3 — Règle Légale : Format SIRET invalide (moins de 14 chiffres)
    // =========================================================

    /**
     * SCÉNARIO : On fournit un SIRET de 13 chiffres (un de moins que la norme).
     * POURQUOI : Le SIRET est une obligation légale française pour les entreprises.
     *            Un SIRET invalide sur une facture peut entraîner des problèmes
     *            fiscaux graves. La validation doit être exacte : 14 chiffres.
     * ASSERTION : IllegalArgumentException pour SIRET de mauvaise longueur.
     */
    @Test
    @Order(3)
    @DisplayName("Règle légale - SIRET de 13 chiffres (trop court) doit lever une exception")
    void ajouterTiers_siretTropCourt_doitLeverException() {
        // Arrange
        Tiers tierssMauvaisSiret = new Tiers(
                0,
                "Société Test SARL",
                null,
                TiersType.FOURNISSEUR,
                "siret.court@test.fr",
                "2 rue Test",
                "0102030405",
                "1234567890123", // 13 chiffres au lieu de 14
                null
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> tiersService.ajouterTiers(tierssMauvaisSiret),
                "Un SIRET de moins de 14 chiffres doit être refusé"
        );
    }

    // =========================================================
    // TEST 4 — Règle Légale : SIRET avec des lettres invalide
    // =========================================================

    /**
     * SCÉNARIO : On fournit un "SIRET" contenant des lettres.
     * POURQUOI : Un SIRET est composé UNIQUEMENT de chiffres en France. Un SIRET
     *            avec des lettres (ex: saisi à la main) est une erreur de saisie
     *            qui doit être détectée avant stockage en BDD.
     * ASSERTION : IllegalArgumentException pour SIRET non numérique.
     */
    @Test
    @Order(4)
    @DisplayName("Règle légale - SIRET avec des lettres doit lever une exception")
    void ajouterTiers_siretAvecLettres_doitLeverException() {
        // Arrange
        Tiers tierssSiretLettres = new Tiers(
                0,
                "Société Lettres SAS",
                null,
                TiersType.CLIENT,
                "siret.lettres@test.fr",
                "3 rue Test",
                "0102030405",
                "1234ABCD567890", // Contient des lettres = invalide
                null
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> tiersService.ajouterTiers(tierssSiretLettres),
                "Un SIRET contenant des lettres doit être refusé (uniquement des chiffres)"
        );
    }

    // =========================================================
    // TEST 5 — Règle de Format : Email sans arobase invalide
    // =========================================================

    /**
     * SCÉNARIO : On fournit un email sans le symbole '@'.
     * POURQUOI : Un email invalide empêche l'envoi de documents commerciaux
     *            automatiques (factures, devis). La validation basique doit au
     *            moins vérifier la présence de '@' et d'un '.'.
     * ASSERTION : IllegalArgumentException pour email malformé.
     */
    @Test
    @Order(5)
    @DisplayName("Règle format - Email sans arobase doit lever une exception")
    void ajouterTiers_emailSansArobase_doitLeverException() {
        // Arrange
        Tiers tierssEmailInvalide = new Tiers(
                0,
                "Tiers Email Invalide",
                null,
                TiersType.CLIENT,
                "email-sans-arobase-point", // Pas d'arobase, pas de point
                "4 rue Test",
                "0102030405",
                null, null
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> tiersService.ajouterTiers(tierssEmailInvalide),
                "Un email sans '@' et sans '.' doit être refusé"
        );
    }

    // =========================================================
    // TEST 6 — Cas Nominal : SIRET null est accepté (optionnel pour particuliers)
    // =========================================================

    /**
     * SCÉNARIO : On crée un client particulier sans SIRET (null).
     * POURQUOI : Le SIRET est obligatoire pour les entreprises mais optionnel
     *            pour les clients particuliers. Si null est refusé, il devient
     *            impossible d'ajouter un client particulier au système.
     * ASSERTION : Aucune exception levée pour SIRET null.
     */
    @Test
    @Order(6)
    @DisplayName("Cas nominal - SIRET null (client particulier) doit être accepté")
    void ajouterTiers_siretNull_doitEtreAccepte() throws SQLException {
        // Arrange
        Tiers clientParticulier = new Tiers(
                0,
                "Martin",
                "Pierre",
                TiersType.CLIENT,
                "pierre.martin." + System.currentTimeMillis() + "@perso.fr",
                "5 allée des Lilas",
                "0607080910",
                null, // Pas de SIRET pour un particulier = valide
                null
        );

        // Act & Assert
        assertDoesNotThrow(
                () -> {
                    tiersService.ajouterTiers(clientParticulier);
                    idTiersCree = clientParticulier.getId();
                },
                "Un client particulier sans SIRET doit pouvoir être créé"
        );

        assertTrue(clientParticulier.getId() > 0,
                "Le client particulier doit recevoir un ID");
    }

    // =========================================================
    // TEST 7 — Cas Nominal : Récupérer tous les tiers retourne une liste non-null
    // =========================================================

    /**
     * SCÉNARIO : On appelle obtenirTousLesTiers() et on inspecte le résultat.
     * POURQUOI : Cette méthode alimente les ComboBox "Client" dans les formulaires
     *            de création de documents. Un retour null ferait planter le
     *            remplissage des ComboBox avec une NullPointerException.
     * ASSERTION : Retourne une liste non-null (vide ou non).
     */
    @Test
    @Order(7)
    @DisplayName("Cas nominal - obtenirTousLesTiers ne doit jamais retourner null")
    void obtenirTousLesTiers_doitRetournerListeNonNull() throws SQLException {
        // Act
        List<Tiers> tiers = tiersService.obtenirTousLesTiers();

        // Assert
        assertNotNull(tiers, "La liste des tiers ne doit jamais être null");
    }
}

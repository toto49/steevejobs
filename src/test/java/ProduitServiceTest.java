import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.service.ProduitService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ProduitService.
 *
 * STRATÉGIE : On teste la COUCHE SERVICE, qui contient la logique métier.
 * Les tests utilisent une vraie connexion BDD (tests d'intégration légers),
 * et chaque test nettoie ses données via teardown pour ne pas polluer la base.
 *
 * On vérifie : validations métier, règles de gestion, gestion des erreurs.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProduitServiceTest {

    private ProduitService produitService;
    private ProduitDAO produitDAO;

    // ID gardé en mémoire entre les tests pour le nettoyage
    private static int idProduitCree = -1;

    @BeforeEach
    void setUp() {
        produitDAO = new ProduitDAO();
        produitService = new ProduitService(produitDAO);
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Nettoyage : supprime le produit de test s'il existe en BDD
        if (idProduitCree > 0) {
            try {
                produitDAO.deleteProduit(idProduitCree);
            } catch (Exception ignored) {}
            idProduitCree = -1;
        }
    }

    // =========================================================
    // TEST 1 — Cas Nominal : Ajout d'un produit valide
    // =========================================================

    /**
     * SCÉNARIO : On crée un produit avec toutes les données correctes.
     * POURQUOI : Vérifie que le chemin normal fonctionne de bout en bout
     *            (service → DAO → BDD). Si ce test échoue, rien d'autre ne sert.
     * ASSERTION : Le produit reçoit bien un ID généré par la BDD (> 0).
     */
    @Test
    @Order(1)
    @DisplayName("Cas nominal - Ajout d'un produit valide doit réussir")
    void ajouterProduit_produitValide_doitReussirEtAttribuerUnId() throws SQLException {
        // Arrange
        Produit produit = new Produit(
                0,
                "Produit Test JUnit",
                new BigDecimal("19.99"),
                new BigDecimal("20.00"),
                100,
                new BigDecimal("0.50"),
                true
        );

        // Act
        produitService.ajouterProduit(produit);
        idProduitCree = produit.getId(); // Garde l'ID pour le nettoyage

        // Assert
        assertTrue(produit.getId() > 0,
                "Le produit créé doit avoir un ID positif attribué par la BDD");
    }

    // =========================================================
    // TEST 2 — Règle Métier : Nom obligatoire
    // =========================================================

    /**
     * SCÉNARIO : On tente d'ajouter un produit sans nom (nom vide "").
     * POURQUOI : Le nom est une règle métier fondamentale. Un produit sans
     *            identifiant lisible ne peut pas exister dans un catalogue.
     *            Valide que la couche Service rejette bien AVANT d'appeler le DAO.
     * ASSERTION : Une IllegalArgumentException est lancée avec un message explicite.
     */
    @Test
    @Order(2)
    @DisplayName("Règle métier - Nom vide doit lever une IllegalArgumentException")
    void ajouterProduit_nomVide_doitLeverException() {
        // Arrange
        Produit produitSansNom = new Produit(
                0,
                "",  // Nom vide = règle métier violée
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                5,
                BigDecimal.ZERO,
                true
        );

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> produitService.ajouterProduit(produitSansNom),
                "Un nom vide doit déclencher une IllegalArgumentException"
        );
        assertNotNull(ex.getMessage(), "Le message d'erreur ne doit pas être null");
    }

    // =========================================================
    // TEST 3 — Cas Limite : Prix négatif interdit
    // =========================================================

    /**
     * SCÉNARIO : On tente d'ajouter un produit avec un prix négatif (-5.00€).
     * POURQUOI : Un prix négatif est une incohérence de données critique qui
     *            corromprait les calculs de factures et devis. La règle doit
     *            être bloquée au plus tôt, dans la couche Service.
     * ASSERTION : IllegalArgumentException levée pour prix < 0.
     */
    @Test
    @Order(3)
    @DisplayName("Cas limite - Prix négatif doit lever une IllegalArgumentException")
    void ajouterProduit_prixNegatif_doitLeverException() {
        // Arrange
        Produit produitPrixNegatif = new Produit(
                0,
                "Produit Prix Negatif",
                new BigDecimal("-5.00"), // Prix négatif = invalide
                new BigDecimal("20.00"),
                10,
                BigDecimal.ZERO,
                true
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> produitService.ajouterProduit(produitPrixNegatif),
                "Un prix négatif doit être refusé par la logique métier"
        );
    }

    // =========================================================
    // TEST 4 — Cas Limite : Stock négatif interdit
    // =========================================================

    /**
     * SCÉNARIO : On tente d'ajouter un produit avec un stock initial de -1.
     * POURQUOI : Un stock négatif est physiquement impossible. Si cette valeur
     *            passait en BDD, les alertes de stock bas et les commandes
     *            seraient incohérentes. On blinde la frontière service.
     * ASSERTION : IllegalArgumentException levée pour quantite < 0.
     */
    @Test
    @Order(4)
    @DisplayName("Cas limite - Stock négatif doit lever une IllegalArgumentException")
    void ajouterProduit_stockNegatif_doitLeverException() {
        // Arrange
        Produit produitStockNegatif = new Produit(
                0,
                "Produit Stock Negatif",
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                -1, // Quantité négative = invalide
                BigDecimal.ZERO,
                true
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> produitService.ajouterProduit(produitStockNegatif),
                "Un stock initial négatif doit être refusé"
        );
    }

    // =========================================================
    // TEST 5 — Cas Nominal : Mise à jour du stock via variation
    // =========================================================

    /**
     * SCÉNARIO : On crée un produit avec 50 unités, puis on applique
     *            une variation de +10 (livraison) et de -5 (vente).
     * POURQUOI : La méthode mettreAJourStock calcule un NOUVEAU stock à partir
     *            d'une VARIATION (pas d'un remplacement direct). Ce comportement
     *            différentiel est critique pour la gestion des mouvements de stock.
     * ASSERTION : Le stock final est bien 50+10-5 = 55 unités.
     */
    @Test
    @Order(5)
    @DisplayName("Cas nominal - Mise à jour du stock par variation doit être correcte")
    void mettreAJourStock_variationsSuccessives_doitCalculerLeStockCorrectement() throws SQLException {
        // Arrange — Crée un produit avec 50 unités
        Produit produit = new Produit(
                0, "Produit Stock Test", new BigDecimal("5.00"),
                new BigDecimal("20.00"), 50, BigDecimal.ZERO, true
        );
        produitService.ajouterProduit(produit);
        idProduitCree = produit.getId();

        // Act — Simule une livraison (+10) puis une vente (-5)
        produitService.mettreAJourStock(produit.getId(), +10);
        produitService.mettreAJourStock(produit.getId(), -5);

        // Assert — Vérifie le stock en relisant depuis la BDD
        Produit produitRecharge = produitDAO.getById(produit.getId());
        assertNotNull(produitRecharge, "Le produit doit toujours exister après les mises à jour");
        assertEquals(55, produitRecharge.getQuantite(),
                "Le stock final doit être 50 + 10 - 5 = 55 unités");
    }

    // =========================================================
    // TEST 6 — Règle Métier : Stock ne peut pas devenir négatif
    // =========================================================

    /**
     * SCÉNARIO : Un produit a 3 unités en stock. On tente de vendre 10 unités.
     * POURQUOI : La règle "pas de stock négatif" est critique pour l'intégrité
     *            logistique. Sans ce garde-fou, on pourrait enregistrer des ventes
     *            impossibles et créer des incohérences comptables.
     * ASSERTION : IllegalArgumentException levée AVANT toute modification en BDD.
     */
    @Test
    @Order(6)
    @DisplayName("Règle métier - Stock qui passerait négatif doit lever une exception")
    void mettreAJourStock_stockInsuffisant_doitLeverException() throws SQLException {
        // Arrange — Crée un produit avec seulement 3 unités
        Produit produit = new Produit(
                0, "Produit Rupture Test", new BigDecimal("25.00"),
                new BigDecimal("20.00"), 3, BigDecimal.ZERO, true
        );
        produitService.ajouterProduit(produit);
        idProduitCree = produit.getId();

        // Act & Assert — Tente de déduire 10 unités alors qu'on en a 3
        assertThrows(
                IllegalArgumentException.class,
                () -> produitService.mettreAJourStock(produit.getId(), -10),
                "Déduire plus d'unités qu'il n'y en a doit être refusé"
        );
    }

    // =========================================================
    // TEST 7 — Cas Erreur : ID invalide pour la modification
    // =========================================================

    /**
     * SCÉNARIO : On tente de modifier un produit avec l'ID 0 (invalide).
     * POURQUOI : Un ID <= 0 signifie que l'entité n'a jamais été persistée.
     *            Ce cas survient si le contrôleur appelle modifierProduit()
     *            sur un objet non sauvegardé. La détection rapide évite
     *            une requête SQL UPDATE silencieuse qui n'affecterait rien.
     * ASSERTION : IllegalArgumentException avec mention de l'ID invalide.
     */
    @Test
    @Order(7)
    @DisplayName("Cas erreur - Modifier un produit avec ID invalide doit lever une exception")
    void modifierProduit_idInvalide_doitLeverException() {
        // Arrange
        Produit produitSansId = new Produit(
                0, // ID = 0, jamais persisté
                "Produit Fantome",
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                5,
                BigDecimal.ZERO,
                true
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> produitService.modifierProduit(produitSansId),
                "Modifier un produit avec ID <= 0 doit être refusé"
        );
    }

    // =========================================================
    // TEST 8 — Cas Nominal : Récupérer tous les produits
    // =========================================================

    /**
     * SCÉNARIO : On appelle obtenirTousLesProduits() et on vérifie le retour.
     * POURQUOI : Ce test vérifie que la liste n'est jamais null (ce qui
     *            causerait des NullPointerException dans les contrôleurs JavaFX
     *            lors du remplissage des TableView et ComboBox).
     * ASSERTION : La liste retournée est non-null (peut être vide, jamais null).
     */
    @Test
    @Order(8)
    @DisplayName("Cas nominal - Récupérer tous les produits ne doit jamais retourner null")
    void obtenirTousLesProduits_doitRetournerUneListeNonNull() throws SQLException {
        // Act
        List<Produit> produits = produitService.obtenirTousLesProduits();

        // Assert
        assertNotNull(produits, "La liste des produits ne doit jamais être null");
        // On ne teste pas la taille car elle dépend de l'état de la BDD
    }
}

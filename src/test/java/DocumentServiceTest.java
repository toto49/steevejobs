import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.*;
import com.eseo.steevejobs.model.Enum.*;
import com.eseo.steevejobs.service.DocumentService;
import com.eseo.steevejobs.service.FichePayeService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour DocumentService.
 *
 * STRATÉGIE : DocumentService orchestre la création de devis/factures/bons
 * de commande. Les validations métier (type, tiers, date obligatoires) et
 * les calculs de totaux HT/TTC sont les zones critiques.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentServiceTest {

    private DocumentService documentService;
    private DocumentDAO documentDAO;
    private TiersDAO tiersDAO;
    private UserDAO userDAO;

    private static User utilisateurTest;
    private static Tiers tiersTest;
    private static int idDocumentCree = -1;

    @BeforeAll
    static void setUpGlobal() throws SQLException {
        UserDAO udao = new UserDAO();
        TiersDAO tdao = new TiersDAO();

        List<User> users = udao.findAll();
        List<Tiers> tiers = tdao.findAll();

        if (users.isEmpty()) {
            throw new IllegalStateException("Aucun utilisateur en BDD pour les tests de document");
        }
        if (tiers.isEmpty()) {
            throw new IllegalStateException("Aucun tiers en BDD pour les tests de document. Créez-en un d'abord.");
        }

        utilisateurTest = users.get(0);
        tiersTest = tiers.get(0);
    }

    @BeforeEach
    void setUp() {
        documentDAO = new DocumentDAO();
        documentService = new DocumentService(documentDAO);
        tiersDAO = new TiersDAO();
        userDAO = new UserDAO();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (idDocumentCree > 0) {
            try {
                documentService.supprimerDocument(idDocumentCree);
            } catch (Exception ignored) {}
            idDocumentCree = -1;
        }
    }

    // Helper : crée un document de test basique
    private Document creerDocumentDeTest(DocumentType type) {
        return new Document(
                0,
                type,
                LocalDateTime.now(),
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                DocumentStatut.EN_ATTENTE,
                "",
                tiersTest,
                utilisateurTest
        );
    }

    // =========================================================
    // TEST 1 — Cas Nominal : Création d'un devis valide
    // =========================================================

    /**
     * SCÉNARIO : On crée un devis avec type, tiers et date correctement renseignés.
     * POURQUOI : C'est le cas nominal de base pour tout le module commercial.
     *            Si ce test échoue, aucune génération de document n'est possible.
     * ASSERTION : Le document reçoit un ID positif après persistance.
     */
    @Test
    @Order(1)
    @DisplayName("Cas nominal - Création d'un devis valide doit réussir")
    void ajouterDocument_devisValide_doitPersisterAvecId() throws SQLException {
        // Arrange
        Document devis = creerDocumentDeTest(DocumentType.DEVIS);
        List<Composer> lignesVides = new ArrayList<>();

        // Act
        documentService.ajouterDocument(devis, lignesVides);
        idDocumentCree = devis.getId();

        // Assert
        assertTrue(devis.getId() > 0,
                "Le devis créé doit avoir un ID positif attribué par la BDD");
    }

    // =========================================================
    // TEST 2 — Règle Métier : Type de document obligatoire
    // =========================================================

    /**
     * SCÉNARIO : On crée un document sans type (type null).
     * POURQUOI : Le type (DEVIS, FACTURE, BON_COMMANDE) détermine le comportement
     *            légal du document et son rendu PDF. Sans type, le document est
     *            inutilisable et le PdfGeneratorService planterait avec une NPE.
     * ASSERTION : IllegalArgumentException levée pour type null.
     */
    @Test
    @Order(2)
    @DisplayName("Règle métier - Document sans type doit lever une exception")
    void ajouterDocument_typNull_doitLeverException() {
        // Arrange
        Document docSansType = new Document(
                0,
                null, // Type null = invalide
                LocalDateTime.now(),
                new BigDecimal("50.00"),
                new BigDecimal("60.00"),
                DocumentStatut.EN_ATTENTE,
                "",
                tiersTest,
                utilisateurTest
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> documentService.ajouterDocument(docSansType, new ArrayList<>()),
                "Un document sans type doit être refusé"
        );
    }

    // =========================================================
    // TEST 3 — Règle Métier : Tiers obligatoire
    // =========================================================

    /**
     * SCÉNARIO : On crée un document sans tiers (destinataire null).
     * POURQUOI : Un document commercial SANS destinataire (client ou fournisseur)
     *            est juridiquement invalide. Le PDF généré serait vide dans la
     *            section "Client" et inutilisable.
     * ASSERTION : IllegalArgumentException levée pour tiers null.
     */
    @Test
    @Order(3)
    @DisplayName("Règle métier - Document sans tiers doit lever une exception")
    void ajouterDocument_sansClient_doitLeverException() {
        // Arrange
        Document docSansTiers = new Document(
                0,
                DocumentType.FACTURE,
                LocalDateTime.now(),
                new BigDecimal("200.00"),
                new BigDecimal("240.00"),
                DocumentStatut.A_PAYER,
                "",
                null, // Tiers null = invalide
                utilisateurTest
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> documentService.ajouterDocument(docSansTiers, new ArrayList<>()),
                "Une facture sans client/tiers doit être refusée"
        );
    }

    // =========================================================
    // TEST 4 — Règle Métier : Date obligatoire
    // =========================================================

    /**
     * SCÉNARIO : On crée un document avec une date null.
     * POURQUOI : La date d'un document commercial est obligatoire légalement.
     *            Sans date, le tri des documents, les calculs d'échéance de
     *            paiement et le rendu PDF seraient impossibles.
     * ASSERTION : IllegalArgumentException levée pour date null.
     */
    @Test
    @Order(4)
    @DisplayName("Règle métier - Document sans date doit lever une exception")
    void ajouterDocument_sansDate_doitLeverException() {
        // Arrange
        Document docSansDate = new Document(
                0,
                DocumentType.BON_COMMANDE,
                null, // Date null = invalide
                new BigDecimal("300.00"),
                new BigDecimal("360.00"),
                DocumentStatut.EN_ATTENTE,
                "",
                tiersTest,
                utilisateurTest
        );

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> documentService.ajouterDocument(docSansDate, new ArrayList<>()),
                "Un document sans date doit être refusé"
        );
    }

    // =========================================================
    // TEST 5 — Cas Nominal : Mise à jour du statut d'un document
    // =========================================================

    /**
     * SCÉNARIO : On crée un document EN_ATTENTE et on le passe à PAYE.
     * POURQUOI : Le changement de statut (EN_ATTENTE → À_PAYER → PAYÉ) est
     *            le workflow de vie d'une facture. Sans cette fonctionnalité,
     *            la gestion comptable est impossible.
     * ASSERTION : Le statut persisté en BDD est bien PAYE.
     */
    @Test
    @Order(5)
    @DisplayName("Cas nominal - Mise à jour du statut d'un document doit être persistée")
    void updateStatut_documentExistant_doitPersisterLeNouveauStatut() throws SQLException {
        // Arrange
        Document doc = creerDocumentDeTest(DocumentType.FACTURE);
        documentService.ajouterDocument(doc, new ArrayList<>());
        idDocumentCree = doc.getId();

        // Act
        boolean succes = documentService.updateStatut(doc.getId(), DocumentStatut.PAYE);

        // Assert
        assertTrue(succes, "La mise à jour du statut doit retourner true");

        Document docRecharge = documentDAO.getById(doc.getId());
        assertNotNull(docRecharge, "Le document doit toujours exister après la mise à jour");
        assertEquals(DocumentStatut.PAYE, docRecharge.getStatut(),
                "Le statut persisté doit être PAYE");
    }

    // =========================================================
    // TEST 6 — Cas Nominal : findAll retourne une liste non-null
    // =========================================================

    /**
     * SCÉNARIO : On appelle findAll() et on inspecte le résultat.
     * POURQUOI : La TableView des documents est chargée depuis findAll(). Un
     *            retour null provoque un crash immédiat dans DocumentController.
     * ASSERTION : Retourne une liste non-null.
     */
    @Test
    @Order(6)
    @DisplayName("Cas nominal - findAll ne doit jamais retourner null")
    void findAll_doitRetournerListeNonNull() throws SQLException {
        // Act
        List<Document> documents = documentService.findAll();

        // Assert
        assertNotNull(documents, "La liste des documents ne doit jamais être null");
    }
}


// =============================================================================
// CLASSE SÉPARÉE - Tests pour FichePayeService
// =============================================================================

/**
 * Tests unitaires pour FichePayeService.
 *
 * STRATÉGIE : Le service de fiche de paie effectue des calculs financiers
 * (salaire brut, cotisations, net). Les calculs mathématiques doivent être
 * testés rigoureusement car une erreur affecte directement la paie des employés.
 */
class FichePayeServiceTest {

    private FichePayeService fichePayeService;

    @BeforeEach
    void setUp() {
        fichePayeService = new FichePayeService();
    }

    // =========================================================
    // TEST 1 — Règle Métier : Salaire base <= 0 interdit
    // =========================================================

    /**
     * SCÉNARIO : On génère une fiche avec un salaire de base négatif (-100€).
     * POURQUOI : Un salaire négatif produirait un bulletin de paie erroné avec
     *            un "net à payer" négatif. La règle doit être bloquée avant
     *            toute génération de PDF ou écriture en BDD.
     * ASSERTION : IllegalArgumentException pour salaireBase <= 0.
     */
    @Test
    @DisplayName("Règle métier - Salaire de base négatif doit lever une exception")
    void genererFichePaye_salaireNegatif_doitLeverException() {
        // Arrange — Il faut un user réel pour la fiche de paie
        User employe;
        try {
            UserDAO dao = new UserDAO();
            List<User> users = dao.findAll();
            if (users.isEmpty()) return; // Skip si pas de données
            employe = users.get(0);
        } catch (Exception e) {
            return; // Skip si BDD inaccessible
        }

        LocalDateTime mois = LocalDateTime.of(2025, 1, 1, 0, 0);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> fichePayeService.genererFichePaye(employe, mois, -100.0, 0.22),
                "Un salaire de base négatif doit être refusé"
        );
    }

    // =========================================================
    // TEST 2 — Règle Métier : Taux cotisations hors [0,1) interdit
    // =========================================================

    /**
     * SCÉNARIO : On génère une fiche avec un taux de cotisations de 1.5 (150%).
     * POURQUOI : Un taux >= 1 signifie que les cotisations dépassent le salaire
     *            brut, ce qui produirait un "net à payer" négatif ou nul.
     *            Légalement impossible, doit être bloqué proprement.
     * ASSERTION : IllegalArgumentException pour taux >= 1.
     */
    @Test
    @DisplayName("Règle métier - Taux de cotisations >= 1.0 doit lever une exception")
    void genererFichePaye_tauxCotisationsTropEleve_doitLeverException() {
        // Arrange
        User employe;
        try {
            UserDAO dao = new UserDAO();
            List<User> users = dao.findAll();
            if (users.isEmpty()) return;
            employe = users.get(0);
        } catch (Exception e) {
            return;
        }

        LocalDateTime mois = LocalDateTime.of(2025, 2, 1, 0, 0);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> fichePayeService.genererFichePaye(employe, mois, 2500.0, 1.5), // 150% = invalide
                "Un taux de cotisations >= 100% doit être refusé"
        );
    }

    // =========================================================
    // TEST 3 — Règle Métier : Taux négatif interdit
    // =========================================================

    /**
     * SCÉNARIO : On utilise un taux de cotisations négatif (-0.10).
     * POURQUOI : Un taux négatif signifie que l'employé *reçoit* des cotisations
     *            en plus de son salaire, ce qui est absurde. Cette valeur ne peut
     *            venir que d'une erreur de saisie ou d'un bug.
     * ASSERTION : IllegalArgumentException pour taux < 0.
     */
    @Test
    @DisplayName("Règle métier - Taux de cotisations négatif doit lever une exception")
    void genererFichePaye_tauxCotisationsNegatif_doitLeverException() {
        // Arrange
        User employe;
        try {
            UserDAO dao = new UserDAO();
            List<User> users = dao.findAll();
            if (users.isEmpty()) return;
            employe = users.get(0);
        } catch (Exception e) {
            return;
        }

        LocalDateTime mois = LocalDateTime.of(2025, 3, 1, 0, 0);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> fichePayeService.genererFichePaye(employe, mois, 2500.0, -0.10), // Négatif = invalide
                "Un taux de cotisations négatif doit être refusé"
        );
    }
}

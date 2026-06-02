package dao;

import com.eseo.steevejobs.dao.ComposerDAO;
import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.User;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestCleanup;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import service.support.TestDataFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DaoIntegrationExtension.class)
class ComposerDAOTest {

    private UserDAO userDAO;
    private TiersDAO tiersDAO;
    private ProduitDAO produitDAO;
    private DocumentDAO documentDAO;
    private ComposerDAO composerDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        tiersDAO = new TiersDAO();
        produitDAO = new ProduitDAO();
        documentDAO = new DocumentDAO();
        composerDAO = new ComposerDAO();
    }

    @Test
    void createLigne_puisFindByDocumentId_retourneLigne() throws SQLException {
        int docId = creerDocumentAvecLigne();

        List<Composer> lignes = composerDAO.findByDocumentId(docId);

        assertEquals(1, lignes.size());
        assertEquals(0, BigDecimal.valueOf(2).compareTo(lignes.get(0).getQuantite()));
    }

    @Test
    void deleteByDocumentId_supprimeLignes() throws SQLException {
        int docId = creerDocumentAvecLigne();

        assertTrue(composerDAO.deleteByDocumentId(docId));
        assertTrue(composerDAO.findByDocumentId(docId).isEmpty());
    }

    @Test
    void createLigne_deuxProduits_retourneDeuxLignes() throws SQLException {
        User vendeur = DaoTestFixtures.insertUser(userDAO);
        Tiers tiers = DaoTestFixtures.insertTiers(tiersDAO);
        Produit produit1 = DaoTestFixtures.insertProduit(produitDAO);
        Produit produit2 = DaoTestFixtures.insertProduit(produitDAO);
        int docId = creerDocument(vendeur, tiers);

        assertTrue(composerDAO.createLigne(new Composer(docId, produit1, BigDecimal.ONE, BigDecimal.TEN)));
        assertTrue(composerDAO.createLigne(new Composer(docId, produit2, BigDecimal.ONE, BigDecimal.TEN)));

        assertEquals(2, composerDAO.findByDocumentId(docId).size());
    }

    private int creerDocumentAvecLigne() throws SQLException {
        User vendeur = DaoTestFixtures.insertUser(userDAO);
        Tiers tiers = DaoTestFixtures.insertTiers(tiersDAO);
        Produit produit = DaoTestFixtures.insertProduit(produitDAO);
        int docId = creerDocument(vendeur, tiers);
        composerDAO.createLigne(new Composer(docId, produit, BigDecimal.valueOf(2), BigDecimal.valueOf(19.98)));
        return docId;
    }

    private int creerDocument(User vendeur, Tiers tiers) throws SQLException {
        Document template = TestDataFactory.documentValide();
        Document document = new Document(
                0,
                template.getType(),
                template.getDate(),
                template.getPrixHt(),
                template.getPrixTtc(),
                template.getStatut(),
                template.getUrl(),
                tiers,
                vendeur
        );
        documentDAO.createDocument(document);
        int docId = document.getId();
        DaoTestCleanup.register(() -> documentDAO.deleteDocument(docId));
        return docId;
    }
}

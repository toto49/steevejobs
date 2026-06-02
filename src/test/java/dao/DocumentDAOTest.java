package dao;

import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.User;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestCleanup;
import dao.support.DaoTestFixtures;
import service.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DaoIntegrationExtension.class)
class DocumentDAOTest {

    private UserDAO userDAO;
    private TiersDAO tiersDAO;
    private DocumentDAO documentDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        tiersDAO = new TiersDAO();
        documentDAO = new DocumentDAO();
    }

    @Test
    void createDocument_puisGetById_retourneDocument() throws SQLException {
        Document document = creerDocument();

        Document lu = documentDAO.getById(document.getId());

        assertNotNull(lu);
        assertEquals(0, document.getPrixTtc().compareTo(lu.getPrixTtc()));
    }

    @Test
    void updateStatut_changeStatut() throws SQLException {
        Document document = creerDocument();

        assertTrue(documentDAO.updateStatut(document.getId(), DocumentStatut.PAYE));
        assertEquals(DocumentStatut.PAYE, documentDAO.getById(document.getId()).getStatut());
    }

    @Test
    void findByTiersId_retourneDocuments() throws SQLException {
        Document document = creerDocument();

        List<Document> documents = documentDAO.findByTiersId(document.getTiers().getId());

        assertFalse(documents.isEmpty());
        assertEquals(document.getId(), documents.get(0).getId());
    }

    private Document creerDocument() throws SQLException {
        User vendeur = DaoTestFixtures.insertUser(userDAO);
        Tiers tiers = DaoTestFixtures.insertTiers(tiersDAO);
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
        DaoTestCleanup.register(() -> documentDAO.deleteDocument(document.getId()));
        return document;
    }
}

package dao;

import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Tiers;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du {@link com.eseo.steevejobs.dao.TiersDAO}.
 * <p>
 * Couvre création, unicité d'e-mail et mise à jour du nom.
 * </p>
 */
@ExtendWith(DaoIntegrationExtension.class)
class TiersDAOTest {

    private TiersDAO tiersDAO;

    @BeforeEach
    void setUp() {
        tiersDAO = new TiersDAO();
    }

    @Test
    void createTiers_puisGetById_retourneTiers() throws SQLException {
        Tiers tiers = DaoTestFixtures.insertTiers(tiersDAO);

        Tiers lu = tiersDAO.getById(tiers.getId());

        assertNotNull(lu);
        assertEquals(tiers.getEmail(), lu.getEmail());
    }

    @Test
    void emailExists_retourneTruePourEmailExistant() throws SQLException {
        Tiers tiers = DaoTestFixtures.insertTiers(tiersDAO);

        assertTrue(tiersDAO.emailExists(tiers.getEmail()));
    }

    @Test
    void updateTiers_modifieNom() throws SQLException {
        Tiers tiers = DaoTestFixtures.insertTiers(tiersDAO);
        tiers.setNom("Société modifiée DAO");

        assertTrue(tiersDAO.updateTiers(tiers));

        assertEquals("Société modifiée DAO", tiersDAO.getById(tiers.getId()).getNom());
    }
}

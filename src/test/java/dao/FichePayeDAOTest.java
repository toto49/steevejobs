package dao;

import com.eseo.steevejobs.dao.FichePayeDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du {@link com.eseo.steevejobs.dao.FichePayeDAO}.
 * <p>
 * Couvre création, lecture par identifiant, liste par employé et comptage.
 * </p>
 */
@ExtendWith(DaoIntegrationExtension.class)
class FichePayeDAOTest {

    private UserDAO userDAO;
    private FichePayeDAO fichePayeDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        fichePayeDAO = new FichePayeDAO();
    }

    @Test
    void createFichePaye_puisGetById_retourneFiche() throws SQLException {
        User employe = DaoTestFixtures.insertUser(userDAO);
        FichePaye fiche = DaoTestFixtures.insertFichePaye(fichePayeDAO, employe);

        FichePaye lu = fichePayeDAO.getById(fiche.getId());

        assertNotNull(lu);
        assertEquals("https://dao.test/paie.pdf", lu.getUrl());
    }

    @Test
    void findByEmployeId_retourneFiches() throws SQLException {
        User employe = DaoTestFixtures.insertUser(userDAO);
        FichePaye fiche = DaoTestFixtures.insertFichePaye(fichePayeDAO, employe);

        List<FichePaye> fiches = fichePayeDAO.findByEmployeId(employe.getId());

        assertFalse(fiches.isEmpty());
        assertEquals(fiche.getId(), fiches.get(0).getId());
    }

    @Test
    void countByEmployeId_retourneNombreCorrect() throws SQLException {
        User employe = DaoTestFixtures.insertUser(userDAO);
        DaoTestFixtures.insertFichePaye(fichePayeDAO, employe);

        assertEquals(1, fichePayeDAO.countByEmployeId(employe.getId()));
    }
}

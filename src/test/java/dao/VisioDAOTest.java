package dao;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.dao.VisioDAO;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.model.Visio;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du {@link com.eseo.steevejobs.dao.VisioDAO}.
 * <p>
 * Scénarios salon instantané, réunion planifiée et vérification du créateur.
 * </p>
 */
@ExtendWith(DaoIntegrationExtension.class)
class VisioDAOTest {

    private UserDAO userDAO;
    private VisioDAO visioDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        visioDAO = new VisioDAO();
    }

    @Test
    void enregistrerSalonInstantane_puisExisteEnBdd() throws SQLException {
        User createur = DaoTestFixtures.insertUser(userDAO);
        Visio visio = DaoTestFixtures.insertVisioInstant(visioDAO, createur);

        assertTrue(visioDAO.existeEnBdd(visio.getRoom_name()));
    }

    @Test
    void planifierReunion_puisExisteEnBdd() throws SQLException {
        User createur = DaoTestFixtures.insertUser(userDAO);
        User invite = DaoTestFixtures.insertUser(userDAO);
        Visio visio = DaoTestFixtures.insertVisioPlanifie(visioDAO, createur, List.of(invite.getId()));

        assertTrue(visioDAO.existeEnBdd(visio.getRoom_name()));
    }

    @Test
    void isCreateur_retourneTruePourCreateur() throws SQLException {
        User createur = DaoTestFixtures.insertUser(userDAO);
        Visio visio = DaoTestFixtures.insertVisioInstant(visioDAO, createur);

        assertTrue(visioDAO.isCreateur(visio.getRoom_name(), createur.getId()));
    }
}

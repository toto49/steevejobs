package dao;

import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Planning;
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
 * Tests d'intégration du {@link com.eseo.steevejobs.dao.PlanningDAO}.
 * <p>
 * Couvre création, lecture par utilisateur et suppression.
 * </p>
 */
@ExtendWith(DaoIntegrationExtension.class)
class PlanningDAOTest {

    private UserDAO userDAO;
    private PlanningDAO planningDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        planningDAO = new PlanningDAO();
    }

    @Test
    void createPlanning_puisGetById_retournePlanning() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);
        Planning planning = DaoTestFixtures.insertPlanning(planningDAO, user);

        Planning lu = planningDAO.getById(planning.getId());

        assertNotNull(lu);
        assertEquals("CONGE", lu.getType());
        assertEquals(user.getId(), lu.getUser().getId());
    }

    @Test
    void findByUserId_retournePlannings() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);
        Planning planning = DaoTestFixtures.insertPlanning(planningDAO, user);

        List<Planning> plannings = planningDAO.findByUserId(user.getId());

        assertFalse(plannings.isEmpty());
        assertTrue(plannings.stream().anyMatch(p -> p.getId() == planning.getId()));
    }

    @Test
    void deletePlanning_supprimePlanning() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);
        Planning planning = DaoTestFixtures.insertPlanning(planningDAO, user);

        assertTrue(planningDAO.deletePlanning(planning.getId()));
        assertNull(planningDAO.getById(planning.getId()));
    }
}

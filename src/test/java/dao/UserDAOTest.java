package dao;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du {@link com.eseo.steevejobs.dao.UserDAO} sur H2.
 * <p>
 * Extension : {@link dao.support.DaoIntegrationExtension} ; fixtures : {@link dao.support.DaoTestFixtures}.
 * Cycle de vie : nouvelle instance DAO par test ({@code @BeforeEach}).
 * </p>
 */
@ExtendWith(DaoIntegrationExtension.class)
class UserDAOTest {

    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
    }

    @Test
    void createUser_puisGetByEmail_retourneUtilisateur() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);

        User lu = userDAO.getByEmail(user.getEmail());

        assertNotNull(lu);
        assertEquals(user.getId(), lu.getId());
        assertEquals("Cleanup", lu.getNom());
    }

    @Test
    void deactivateUser_desactiveUtilisateur() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);

        assertTrue(userDAO.deactivateUser(user.getId()));
        assertFalse(userDAO.getById(user.getId()).isActif());
    }
}

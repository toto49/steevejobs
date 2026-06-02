package dao;

import com.eseo.steevejobs.dao.PermissionDAOImpl;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DaoIntegrationExtension.class)
class PermissionDAOTest {

    private UserDAO userDAO;
    private PermissionDAOImpl permissionDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        permissionDAO = new PermissionDAOImpl();
    }

    @Test
    void insertRolePermission_retourneCodePourUtilisateur() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);
        DaoTestFixtures.insertPermission(permissionDAO, user.getRole());

        List<String> codes = permissionDAO.getPermissionCodesByUserId(user.getId());

        assertFalse(codes.isEmpty());
    }

    @Test
    void getPermissionIdsByRole_retournePermissionInseree() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);
        int permId = DaoTestFixtures.insertPermission(permissionDAO, user.getRole());

        List<Integer> ids = permissionDAO.getPermissionIdsByRole(user.getRole());

        assertTrue(ids.contains(permId));
    }

    @Test
    void deleteRolePermission_retireLaPermission() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);
        int permId = DaoTestFixtures.insertPermission(permissionDAO, user.getRole());

        assertTrue(permissionDAO.deleteRolePermission(user.getRole(), permId));
        assertFalse(permissionDAO.getPermissionIdsByRole(user.getRole()).contains(permId));
    }
}

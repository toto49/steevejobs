package dao;

import com.eseo.steevejobs.dao.HeuresTravailDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.HeuresTravail;
import com.eseo.steevejobs.model.User;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DaoIntegrationExtension.class)
class HeuresTravailDAOTest {

    private UserDAO userDAO;
    private HeuresTravailDAO heuresTravailDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        heuresTravailDAO = new HeuresTravailDAO();
    }

    @Test
    void sauvegarder_puisGetHeuresParDate_retourneLesHeures() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);
        LocalDate date = LocalDate.now();
        DaoTestFixtures.insertHeuresTravail(heuresTravailDAO, user, date);

        HeuresTravail lu = heuresTravailDAO.getHeuresParDate(user.getId(), date);

        assertNotNull(lu);
        assertEquals(date, lu.getDateJour());
    }

    @Test
    void getHeuresParDate_sansEnregistrement_retourneNull() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);
        LocalDate date = LocalDate.now().plusDays(30);

        assertNull(heuresTravailDAO.getHeuresParDate(user.getId(), date));
    }

    @Test
    void sauvegarder_deuxFois_memeDate_metAJourLesHeures() throws SQLException {
        User user = DaoTestFixtures.insertUser(userDAO);
        LocalDate date = LocalDate.now().plusDays(1);
        DaoTestFixtures.insertHeuresTravail(heuresTravailDAO, user, date);

        assertTrue(heuresTravailDAO.sauvegarder(
                user.getId(), date,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                LocalTime.of(13, 0), LocalTime.of(18, 0),
                LocalTime.of(9, 0)));

        HeuresTravail lu = heuresTravailDAO.getHeuresParDate(user.getId(), date);

        assertNotNull(lu);
        assertEquals(LocalTime.of(8, 0), lu.getDebutMatin());
        assertEquals(LocalTime.of(18, 0), lu.getFinAprem());
    }
}

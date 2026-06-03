package dao;

import com.eseo.steevejobs.dao.TicketDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du {@link com.eseo.steevejobs.dao.TicketDAO} (CRUD, statut, liste).
 * <p>
 * Fixtures utilisateur et ticket via {@link dao.support.DaoTestFixtures} ; nettoyage après chaque test.
 * </p>
 */
@ExtendWith(DaoIntegrationExtension.class)
class TicketDAOTest {

    private UserDAO userDAO;
    private TicketDAO ticketDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        ticketDAO = new TicketDAO();
    }

    @Test
    void createTicket_puisGetById_retourneTicket() throws SQLException {
        User auteur = DaoTestFixtures.insertUser(userDAO);
        Ticket ticket = DaoTestFixtures.insertTicket(ticketDAO, auteur);

        Ticket lu = ticketDAO.getById(ticket.getId());

        assertNotNull(lu);
        assertEquals("Sujet DAO", lu.getSujet());
        assertEquals(auteur.getId(), lu.getAuteur().getId());
    }

    @Test
    void updateStatut_changeStatut() throws SQLException {
        User auteur = DaoTestFixtures.insertUser(userDAO);
        Ticket ticket = DaoTestFixtures.insertTicket(ticketDAO, auteur);

        assertTrue(ticketDAO.updateStatut(ticket.getId(), StatutTicket.FERME));

        assertEquals(StatutTicket.FERME, ticketDAO.getById(ticket.getId()).getStatut());
    }

    @Test
    void findAll_contientTicketCree() throws SQLException {
        User auteur = DaoTestFixtures.insertUser(userDAO);
        Ticket ticket = DaoTestFixtures.insertTicket(ticketDAO, auteur);

        assertTrue(ticketDAO.findAll().stream().anyMatch(t -> t.getId() == ticket.getId()));
    }
}

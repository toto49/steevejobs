package dao;

import com.eseo.steevejobs.dao.MessageDAO;
import com.eseo.steevejobs.dao.TicketDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;
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
class MessageDAOTest {

    private UserDAO userDAO;
    private TicketDAO ticketDAO;
    private MessageDAO messageDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        ticketDAO = new TicketDAO();
        messageDAO = new MessageDAO();
    }

    @Test
    void createMessage_puisGetById_retourneMessage() throws SQLException {
        User auteur = DaoTestFixtures.insertUser(userDAO);
        Ticket ticket = DaoTestFixtures.insertTicket(ticketDAO, auteur);
        Message message = DaoTestFixtures.insertMessage(messageDAO, auteur, ticket);

        Message lu = messageDAO.getById(message.getId());

        assertNotNull(lu);
        assertEquals("Message DAO de test", lu.getContenu());
    }

    @Test
    void findByTicketId_retourneMessages() throws SQLException {
        User auteur = DaoTestFixtures.insertUser(userDAO);
        Ticket ticket = DaoTestFixtures.insertTicket(ticketDAO, auteur);
        Message message = DaoTestFixtures.insertMessage(messageDAO, auteur, ticket);

        List<Message> messages = messageDAO.findByTicketId(ticket.getId());

        assertEquals(1, messages.size());
        assertEquals(message.getId(), messages.get(0).getId());
    }

    @Test
    void deleteMessage_supprimeMessage() throws SQLException {
        User auteur = DaoTestFixtures.insertUser(userDAO);
        Ticket ticket = DaoTestFixtures.insertTicket(ticketDAO, auteur);
        Message message = DaoTestFixtures.insertMessage(messageDAO, auteur, ticket);

        assertTrue(messageDAO.deleteMessage(message.getId()));
        assertNull(messageDAO.getById(message.getId()));
    }
}

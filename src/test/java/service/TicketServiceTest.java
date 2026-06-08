package service;

import com.eseo.steevejobs.dao.TicketDAO;
import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.MessageService;
import com.eseo.steevejobs.service.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.TicketServiceImpl}.
 * <p>
 * Mocks ticket et message ; vérifie cycle de vie ticket, messages et formatage de date.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private TicketDAO ticketDAO;

    @Mock
    private MessageService messageService;

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(ticketDAO, messageService);
    }

    private Ticket ticketValide() {
        Ticket ticket = new Ticket();
        ticket.setSujet("Problème VPN");
        ticket.setDescription("Impossible de se connecter au VPN depuis le bureau.");
        ticket.setService("RH");
        ticket.setAuteur(new User(1, "Dupont", "Jean", "jean@test.fr", "hash", "", "EMPLOYE", "", "Dev", true));
        return ticket;
    }

    private Message messageValide() {
        Message message = new Message();
        message.setContenu("Première réponse");
        message.setAuteur(new User(2, "Support", "IT", "it@test.fr", "hash", "", "ADMIN", "", "Support", true));
        return message;
    }

    @Test
    void creerTicket_doitInitialiserStatutEtDate() throws SQLException {
        Ticket ticket = ticketValide();

        doAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setId(42);
            return null;
        }).when(ticketDAO).createTicket(any(Ticket.class));

        Ticket resultat = service.creerTicket(ticket);

        assertNotNull(resultat.getDateOuverture());
        assertEquals(StatutTicket.EN_ATTENTE, resultat.getStatut());
        verify(ticketDAO).createTicket(ticket);
    }

    @Test
    void ajouterMessage_ticketInexistant_doitLeverException() throws SQLException {
        when(ticketDAO.getById(99)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.ajouterMessage(99, messageValide()));
        assertEquals("Le ticket 99 est introuvable.", ex.getMessage());
        verify(messageService, never()).createMessage(any());
    }

    @Test
    void ajouterMessage_ticketEnAttente_doitPasserEnCours() throws SQLException {
        Ticket ticket = ticketValide();
        ticket.setId(1);
        ticket.setStatut(StatutTicket.EN_ATTENTE);
        Message message = messageValide();

        when(ticketDAO.getById(1)).thenReturn(ticket);

        Message resultat = service.ajouterMessage(1, message);

        assertNotNull(resultat.getDateEnvoi());
        assertSame(ticket, resultat.getTicket());
        verify(messageService).createMessage(message);
        verify(ticketDAO).updateStatut(1, StatutTicket.EN_COURS);
    }

    @Test
    void changerStatut_ticketInexistant_doitLeverException() throws SQLException {
        when(ticketDAO.updateStatut(99, StatutTicket.FERME)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.changerStatut(99, StatutTicket.FERME)
        );
        assertEquals("Ticket introuvable ou statut inchangé pour l'ID : 99", ex.getMessage());
    }

    @Test
    void formatTicketDate_dateNull_retourneNA() {
        assertEquals("N/A", service.formatTicketDate(null));
        assertTrue(service.formatTicketDate(LocalDateTime.of(2026, 5, 10, 14, 30)).contains("10/05/2026"));
    }

    @Test
    void getTicketById_retourneLeTicket() throws SQLException {
        Ticket ticket = ticketValide();
        ticket.setId(7);
        when(ticketDAO.getById(7)).thenReturn(ticket);

        assertSame(ticket, service.getTicketById(7));
    }

}

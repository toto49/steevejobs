package service;

import com.eseo.steevejobs.dao.MessageDAO;
import com.eseo.steevejobs.dao.TicketDAO;
import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.TicketServiceImpl;
import service.support.MockitoJava25Support;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private TicketDAO ticketDAO;

    @Mock
    private MessageDAO messageDAO;

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(ticketDAO, messageDAO);
    }

    @Test
    void creerTicket_doitInitialiserStatutEtDate() throws SQLException {
        // Arrange
        Ticket ticket = new Ticket();
        ticket.setSujet("Problème VPN");
        ticket.setAuteur(new User(1, "Dupont", "Jean", "jean@test.fr", "hash", "", "EMPLOYE", "", "Dev", true));

        doAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setId(42);
            return null;
        }).when(ticketDAO).createTicket(any(Ticket.class));

        // Act
        Ticket resultat = service.creerTicket(ticket);

        // Assert
        assertNotNull(resultat.getDateOuverture());
        assertEquals(StatutTicket.EN_ATTENTE, resultat.getStatut());
        verify(ticketDAO).createTicket(ticket);
    }

    @Test
    void ajouterMessage_ticketInexistant_doitLeverException() throws SQLException {
        // Arrange
        when(ticketDAO.getById(99)).thenReturn(null);
        Message message = new Message();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.ajouterMessage(99, message));
        assertEquals("Ticket inexistant", ex.getMessage());
        verify(messageDAO, never()).createMessage(any());
    }

    @Test
    void ajouterMessage_ticketEnAttente_doitPasserEnCours() throws SQLException {
        // Arrange
        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setStatut(StatutTicket.EN_ATTENTE);

        Message message = new Message();
        message.setContenu("Première réponse");
        message.setAuteur(new User(2, "Support", "IT", "it@test.fr", "hash", "", "ADMIN", "", "Support", true));

        when(ticketDAO.getById(1)).thenReturn(ticket);

        // Act
        Message resultat = service.ajouterMessage(1, message);

        // Assert
        assertNotNull(resultat.getDateEnvoi());
        assertSame(ticket, resultat.getTicket());
        verify(messageDAO).createMessage(message);
        verify(ticketDAO).updateStatut(1, StatutTicket.EN_COURS);
    }

    @Test
    void changerStatut_ticketInexistant_doitLeverException() throws SQLException {
        // Arrange
        when(ticketDAO.updateStatut(99, StatutTicket.FERME)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.changerStatut(99, StatutTicket.FERME)
        );
        assertEquals("Impossible de modifier le statut : Ticket introuvable", ex.getMessage());
    }

    @Test
    void formatTicketDate_dateNull_retourneNA() {
        // Act & Assert
        assertEquals("N/A", service.formatTicketDate(null));
        assertTrue(service.formatTicketDate(LocalDateTime.of(2026, 5, 10, 14, 30)).contains("10/05/2026"));
    }

    @Test
    void getTicketById_retourneLeTicket() throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(7);
        when(ticketDAO.getById(7)).thenReturn(ticket);

        assertSame(ticket, service.getTicketById(7));
    }

    @Test
    void changerStatut_ticketExistant_retourneTicketMisAJour() throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(3);
        ticket.setStatut(StatutTicket.FERME);
        when(ticketDAO.updateStatut(3, StatutTicket.FERME)).thenReturn(true);
        when(ticketDAO.getById(3)).thenReturn(ticket);

        Ticket resultat = service.changerStatut(3, StatutTicket.FERME);

        assertEquals(StatutTicket.FERME, resultat.getStatut());
    }

    @Test
    void getDureeOuverture_ticketRecent_retourneHeures() {
        Ticket ticket = new Ticket();
        ticket.setDateOuverture(LocalDateTime.now().minusHours(2));

        assertTrue(service.getDureeOuverture(ticket).contains("heure"));
    }

    @Test
    void ajouterMessage_ticketDejaEnCours_neChangePasLeStatut() throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setStatut(StatutTicket.EN_COURS);
        Message message = new Message();
        when(ticketDAO.getById(1)).thenReturn(ticket);

        service.ajouterMessage(1, message);

        verify(ticketDAO, never()).updateStatut(anyInt(), any());
    }
}

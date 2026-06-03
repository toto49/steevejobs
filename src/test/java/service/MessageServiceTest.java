package service;

import com.eseo.steevejobs.dao.MessageDAO;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.service.MessageService;
import com.eseo.steevejobs.service.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.MessageServiceImpl}.
 * <p>
 * Vérifie lecture, liste par auteur, suppression et encapsulation des erreurs SQL.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private MessageDAO messageDAO;

    private MessageService service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(messageDAO);
    }

    @Test
    void getMessage_retourneLeMessage() throws SQLException {
        Message message = new Message();
        when(messageDAO.getById(1)).thenReturn(message);

        assertSame(message, service.getMessage(1));
    }

    @Test
    void getMessage_erreurSql_doitLeverRuntimeException() throws SQLException {
        when(messageDAO.getById(1)).thenThrow(new SQLException("Erreur BDD"));

        assertThrows(RuntimeException.class, () -> service.getMessage(1));
    }

    @Test
    void getMessagesByAuteur_retourneLaListe() throws SQLException {
        when(messageDAO.findByAuteurId(2)).thenReturn(List.of(new Message(), new Message()));

        assertEquals(2, service.getMessagesByAuteur(2).size());
    }

    @Test
    void deleteMessage_succes_retourneTrue() throws SQLException {
        when(messageDAO.deleteMessage(5)).thenReturn(true);

        assertTrue(service.deleteMessage(5));
    }

    @Test
    void deleteMessage_echec_retourneFalse() throws SQLException {
        when(messageDAO.deleteMessage(5)).thenReturn(false);

        assertFalse(service.deleteMessage(5));
    }
}

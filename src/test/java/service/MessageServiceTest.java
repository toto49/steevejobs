package service;

import com.eseo.steevejobs.dao.MessageDAO;
import com.eseo.steevejobs.model.Message;
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

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private MessageDAO messageDAO;

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(messageDAO);
    }

    @Test
    void getMessageByID_retourneLeMessage() throws SQLException {
        Message message = new Message();
        when(messageDAO.getById(1)).thenReturn(message);

        assertSame(message, service.getMessageByID(1));
    }

    @Test
    void getMessageByID_erreurSql_doitLeverRuntimeException() throws SQLException {
        when(messageDAO.getById(1)).thenThrow(new SQLException("Erreur BDD"));

        assertThrows(RuntimeException.class, () -> service.getMessageByID(1));
    }

    @Test
    void getMessagesByAuteur_retourneLaListe() throws SQLException {
        when(messageDAO.findByAuteurId(2)).thenReturn(List.of(new Message(), new Message()));

        assertEquals(2, service.getMessagesByAuteur(2).size());
    }

    @Test
    void supprimerMessage_succes_retourneTrue() throws SQLException {
        when(messageDAO.deleteMessage(5)).thenReturn(true);

        assertTrue(service.supprimerMessage(5));
    }

    @Test
    void supprimerMessage_echec_retourneFalse() throws SQLException {
        when(messageDAO.deleteMessage(5)).thenReturn(false);

        assertFalse(service.supprimerMessage(5));
    }
}

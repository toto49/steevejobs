package service;

import com.eseo.steevejobs.dao.HeuresTravailDAO;
import com.eseo.steevejobs.model.HeuresTravail;
import com.eseo.steevejobs.service.HeuresTravailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;
import service.support.TestDataFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.HeuresTravailService} (délégation au DAO).
 * <p>
 * Mockito sur {@code HeuresTravailDAO} ; pas d'accès base réelle.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class HeuresTravailServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private HeuresTravailDAO heuresTravailDAO;

    private HeuresTravailService service;

    @BeforeEach
    void setUp() {
        service = new HeuresTravailService(heuresTravailDAO);
    }

    @Test
    void sauvegarderHeures_delegueAuDao() throws SQLException {
        LocalDate date = LocalDate.of(2026, 5, 10);
        LocalTime debutM = LocalTime.of(8, 0);
        LocalTime finM = LocalTime.of(12, 0);
        LocalTime debutA = LocalTime.of(13, 0);
        LocalTime finA = LocalTime.of(17, 0);
        LocalTime total = LocalTime.of(8, 0);

        when(heuresTravailDAO.sauvegarder(1, date, debutM, finM, debutA, finA, total)).thenReturn(true);

        assertTrue(service.sauvegarderHeures(1, date, debutM, finM, debutA, finA, total));
    }

    @Test
    void getHeuresParDate_retourneLesHeures() throws SQLException {
        LocalDate date = LocalDate.of(2026, 5, 10);
        HeuresTravail heures = new HeuresTravail(
                1, date,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                LocalTime.of(13, 0), LocalTime.of(17, 0),
                LocalTime.of(8, 0),
                TestDataFactory.utilisateurActif(1, "user@mail.fr")
        );
        when(heuresTravailDAO.getHeuresParDate(1, date)).thenReturn(heures);

        assertSame(heures, service.getHeuresParDate(1, date));
    }

    @Test
    void sauvegarderHeures_echecDao_retourneFalse() throws SQLException {
        when(heuresTravailDAO.sauvegarder(anyInt(), any(), any(), any(), any(), any(), any())).thenReturn(false);

        assertFalse(service.sauvegarderHeures(1, LocalDate.now(),
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                LocalTime.of(13, 0), LocalTime.of(17, 0),
                LocalTime.of(8, 0)));
    }
}

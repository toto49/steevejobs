package service;

import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.PlanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;
import service.support.TestDataFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private PlanningDAO planningDAO;

    private PlanningService service;

    @BeforeEach
    void setUp() {
        service = new PlanningService(planningDAO);
    }

    private Planning planningValide() {
        return new Planning(
                0,
                LocalDateTime.of(2026, 5, 10, 10, 0),
                LocalDateTime.of(2026, 5, 20, 10, 0),
                "Réunion",
                "Desc",
                "#FFFFFF",
                TestDataFactory.utilisateurActif(1, "user@mail.fr")
        );
    }

    @Test
    void ajouterPlanning_donneesValides_neDoitPasLeverException() throws SQLException {
        Planning planning = planningValide();
        when(planningDAO.createPlanning(planning)).thenReturn(true);

        assertDoesNotThrow(() -> service.ajouterPlanning(planning));
        verify(planningDAO).createPlanning(planning);
    }

    @Test
    void ajouterPlanning_dateFinAvantDebut_doitLeverException() {
        Planning planning = new Planning(
                0,
                LocalDateTime.of(2026, 5, 20, 10, 0),
                LocalDateTime.of(2026, 5, 10, 10, 0),
                "Réunion",
                "Desc",
                "#FFFFFF",
                TestDataFactory.utilisateurActif(1, "user@mail.fr")
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.ajouterPlanning(planning));
        assertEquals("La date de fin doit être postérieure à la date de début.", ex.getMessage());
    }

    @Test
    void ajouterPlanning_userNull_doitLeverException() {
        Planning planning = new Planning(
                0,
                LocalDateTime.of(2026, 5, 10, 10, 0),
                LocalDateTime.of(2026, 5, 20, 10, 0),
                "Réunion",
                "Desc",
                "#FFFFFF",
                null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.ajouterPlanning(planning));
        assertEquals("L'utilisateur associé au planning est obligatoire.", ex.getMessage());
    }

    @Test
    void supprimerPlanning_idInexistant_doitLeverRuntimeException() throws SQLException {
        when(planningDAO.deletePlanning(99)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.supprimerPlanning(99));
        assertEquals("Erreur BDD : Impossible de supprimer cet événement.", ex.getMessage());
    }

    @Test
    void ajouterPlanning_echecBdd_doitLeverRuntimeException() throws SQLException {
        Planning planning = planningValide();
        when(planningDAO.createPlanning(planning)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.ajouterPlanning(planning));
        assertEquals("Erreur BDD : Impossible d'ajouter cet événement au planning.", ex.getMessage());
    }

    @Test
    void modifierPlanning_donneesValides_neDoitPasLeverException() throws SQLException {
        Planning planning = planningValide();
        planning.setId(5);
        when(planningDAO.updatePlanning(planning)).thenReturn(true);

        assertDoesNotThrow(() -> service.modifierPlanning(planning));
    }

    @Test
    void ajouterPlanning_dateDebutNull_doitLeverException() {
        Planning planning = new Planning(
                0, null,
                LocalDateTime.of(2026, 5, 20, 10, 0),
                "Réunion", "Desc", "#FFFFFF", TestDataFactory.utilisateurActif(1, "user@mail.fr")
        );

        assertThrows(IllegalArgumentException.class, () -> service.ajouterPlanning(planning));
    }
}

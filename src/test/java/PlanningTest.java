import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.PlanningService;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanningTest {

    @Test
    void ajouterPlanning_donneesValides_neDoitPasPlanter() {
        // Cas Nominal
        PlanningDAO fakeDAO = new PlanningDAO() {
            @Override public boolean createPlanning(Planning p) { return true; }
        };
        PlanningService service = new PlanningService(fakeDAO);

        Planning p = new Planning(0,
                LocalDateTime.of(2025, 5, 10, 9, 0),  // Début
                LocalDateTime.of(2025, 5, 10, 17, 0), // Fin (bien après le début)
                "Travail", "Journée normale", "#FFFFFF", new User());

        // 1er type : assertDoesNotThrow
        assertDoesNotThrow(() -> service.ajouterPlanning(p), "Un planning totalement valide ne doit générer aucune erreur");
    }

    @Test
    void findByUserId_doitRetournerListePourUtilisateur() throws SQLException {
        // Cas Nominal
        PlanningDAO fakeDAO = new PlanningDAO() {
            @Override public List<Planning> findByUserId(int userId) {
                return Arrays.asList(new Planning(), new Planning());
            }
        };
        PlanningService service = new PlanningService(fakeDAO);

        List<Planning> resultats = service.findByUserId(5);

        // 2ème type : assertEquals
        assertEquals(2, resultats.size(), "Doit retourner les 2 plannings associés à l'utilisateur");
    }


    @Test
    void ajouterPlanning_dateFinAvantDebut_doitLeverException() {
        // Cas d'Erreur : Règle temporelle
        PlanningService service = new PlanningService(null);
        Planning p = new Planning(0,
                LocalDateTime.of(2026, 5, 20, 10, 0), // Date de début plus tardive
                LocalDateTime.of(2026, 5, 10, 10, 0), // Date de fin plus précoce
                "Réunion", "Desc", "#FFFFFF", new User());

        // 4ème type : assertThrows
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.ajouterPlanning(p));
        assertEquals("La date de fin doit être plus tard que la date de début", exception.getMessage());
    }

    @Test
    void ajouterPlanning_userNull_doitLeverException() {
        // Cas d'Erreur : Entité incomplète
        PlanningService service = new PlanningService(null);
        Planning p = new Planning(0,
                LocalDateTime.of(2026, 5, 10, 10, 0),
                LocalDateTime.of(2026, 5, 20, 10, 0),
                "Réunion", "Desc", "#FFFFFF", null); // User est NULL

        assertThrows(IllegalArgumentException.class, () -> service.ajouterPlanning(p));
    }
}
package service;

import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.PlanningService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PlanningServiceTest {

    // On passe null au DAO car les exceptions interviendront avant d'y accéder
    private final PlanningService service = new PlanningService(null);

    @Test
    void ajouterPlanning_dateFinAvantDebut_doitLeverException() {
        Planning p = new Planning(0, LocalDateTime.of(2026, 5, 20, 10, 0), LocalDateTime.of(2026, 5, 10, 10, 0), "Réunion", "Desc", "#FFFFFF", new User());
        assertThrows(IllegalArgumentException.class, () -> service.ajouterPlanning(p));
    }

    @Test
    void ajouterPlanning_userNull_doitLeverException() {
        Planning p = new Planning(0, LocalDateTime.of(2026, 5, 10, 10, 0), LocalDateTime.of(2026, 5, 20, 10, 0), "Réunion", "Desc", "#FFFFFF", null);
        assertThrows(IllegalArgumentException.class, () -> service.ajouterPlanning(p));
    }

    @Test
    void ajouterPlanning_typeVide_doitLeverException() {
        Planning p = new Planning(0, LocalDateTime.of(2026, 5, 10, 10, 0), LocalDateTime.of(2026, 5, 20, 10, 0), "", "Desc", "#FFFFFF", new User());
        assertThrows(IllegalArgumentException.class, () -> service.ajouterPlanning(p));
    }

    @Test
    void ajouterPlanning_jourDebutNull_doitLeverException() {
        Planning p = new Planning(0, null, LocalDateTime.of(2026, 5, 20, 10, 0), "Réunion", "Desc", "#FFFFFF", new User());
        assertThrows(IllegalArgumentException.class, () -> service.ajouterPlanning(p));
    }
}
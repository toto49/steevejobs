package service;

import com.eseo.steevejobs.service.CongeUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires des utilitaires de calcul de congés {@link com.eseo.steevejobs.service.CongeUtil}.
 * <p>
 * Aucune dépendance externe ; validation des types de congé et du décompte de jours ouvrés sur période.
 * </p>
 */
class CongeUtilTest {

    @Test
    void estTypeConge_reconnaitLesVariantes() {
        assertTrue(CongeUtil.estTypeConge("Congé"));
        assertTrue(CongeUtil.estTypeConge("conge"));
        assertFalse(CongeUtil.estTypeConge("Réunion"));
        assertFalse(CongeUtil.estTypeConge(null));
    }

    @Test
    void compterJoursSurPeriode_memeJour_retourneUn() {
        LocalDateTime debut = LocalDateTime.of(2026, 6, 10, 8, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 6, 10, 18, 0);

        assertEquals(1, CongeUtil.compterJoursSurPeriode(debut, fin, 2026));
    }

    @Test
    void compterJoursSurPeriode_troisJoursOuvres_retourneTrois() {
        LocalDateTime debut = LocalDateTime.of(2026, 6, 2, 8, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 6, 4, 18, 0);

        assertEquals(3, CongeUtil.compterJoursSurPeriode(debut, fin, 2026));
    }

    @Test
    void compterJoursSurPeriode_limiteAnnee_decoupeSurExercice() {
        LocalDateTime debut = LocalDateTime.of(2025, 12, 30, 8, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 1, 2, 18, 0);

        assertEquals(2, CongeUtil.compterJoursSurPeriode(debut, fin, 2026));
    }

}

package service;

import com.eseo.steevejobs.service.ConnexionService;
import org.junit.jupiter.api.Test;
import service.support.MockitoJava25Support;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.ConnexionService} (génération de mot de passe aléatoire).
 * <p>
 * Bloc statique : activation Mockito JDK 25 via {@link service.support.MockitoJava25Support}.
 * </p>
 */
class ConnexionServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Test
    void generateRandomMdp_longueurDemandee_respecteLaTaille() {
        String mdp = ConnexionService.generateRandomMdp(12);
        assertEquals(12, mdp.length());
    }

    @Test
    void generateRandomMdp_contientUniquementCaracteresAutorises() {
        String mdp = ConnexionService.generateRandomMdp(20);
        assertTrue(mdp.matches("[A-Za-z0-9]{20}"));
    }

}

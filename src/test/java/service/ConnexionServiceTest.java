package service;

import com.eseo.steevejobs.service.ConnexionService;
import org.junit.jupiter.api.Test;
import service.support.MockitoJava25Support;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void generateRandomMdp_deuxAppels_produisentDesValeursDifferentes() {
        String mdp1 = ConnexionService.generateRandomMdp(16);
        String mdp2 = ConnexionService.generateRandomMdp(16);
        assertNotEquals(mdp1, mdp2);
    }

    @Test
    void generateRandomMdp_longueurZero_retourneChaineVide() {
        assertEquals("", ConnexionService.generateRandomMdp(0));
    }
}

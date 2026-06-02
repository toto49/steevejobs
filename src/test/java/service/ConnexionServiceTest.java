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

}

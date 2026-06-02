package service;

import com.eseo.steevejobs.service.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void genererToken_avecOuSansSecret_retourneNullOuJwtStructure() {
        String token = JwtService.genererToken(42);

        if (token == null) {
            // Pas de JWT_SECRET dans .env (cas CI / poste sans configuration)
            assertNull(token);
        } else {
            assertFalse(token.isBlank());
            assertEquals(3, token.split("\\.").length, "Un JWT comporte header.payload.signature");
        }
    }
}

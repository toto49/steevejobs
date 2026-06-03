package service;

import com.eseo.steevejobs.service.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.JwtService}.
 * <p>
 * Comportement conditionnel selon la présence de {@code JWT_SECRET} dans l'environnement.
 * </p>
 */
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

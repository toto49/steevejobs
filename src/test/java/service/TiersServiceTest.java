package service;

import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.service.TiersService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TiersServiceTest {

    private final TiersService service = new TiersService();

    @Test
    void ajouterTiers_siretMauvaisFormatTropCourt_doitLeverException() {
        // SIRET à 5 chiffres au lieu de 14
        Tiers t = new Tiers(0, "Apple", "Steve", null, "contact@apple.com", "Cupertino", "0102030405", "12345", "FR123");
        assertThrows(IllegalArgumentException.class, () -> service.ajouterTiers(t));
    }

    @Test
    void ajouterTiers_siretAvecLettres_doitLeverException() {
        // SIRET avec des lettres (le service vérifie avec \d+)
        Tiers t = new Tiers(0, "Apple", "Steve", null, "contact@apple.com", "Cupertino", "0102030405", "1234567890ABCD", "FR123");
        assertThrows(IllegalArgumentException.class, () -> service.ajouterTiers(t));
    }

    @Test
    void ajouterTiers_emailInvalide_doitLeverException() {
        // Il manque le point et l'arobase
        Tiers t = new Tiers(0, "Apple", "Steve", null, "contactapple", "Cupertino", "0102030405", "12345678901234", "FR123");
        assertThrows(IllegalArgumentException.class, () -> service.ajouterTiers(t));
    }

    @Test
    void ajouterTiers_nomVide_doitLeverException() {
        Tiers t = new Tiers(0, "", "Steve", null, "contact@apple.com", "Cupertino", "0102030405", "12345678901234", "FR123");
        assertThrows(IllegalArgumentException.class, () -> service.ajouterTiers(t));
    }
}
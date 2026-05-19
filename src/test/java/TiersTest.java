import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.service.TiersService;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TiersTest {

    @Test
    void ajouterTiers_donneesValides_neDoitPasPlanter() {
        // Cas Nominal : Tout va bien
        TiersDAO fakeDAO = new TiersDAO() {
            @Override public boolean createTiers(Tiers t) { return true; }
        };
        TiersService service = new TiersService(fakeDAO); // Via le nouveau constructeur
        Tiers t = new Tiers(0, "Apple", "Steve", null, "contact@apple.com", "Cupertino", "0102030405", "12345678901234", "FR123");

        // 1er type : assertDoesNotThrow
        assertDoesNotThrow(() -> service.ajouterTiers(t), "L'ajout d'un tiers valide ne doit lever aucune exception");
    }

    @Test
    void obtenirTousLesTiers_doitRetournerListeComplete() throws SQLException {
        // Cas Nominal : Vérifier que la liste est bien renvoyée
        TiersDAO fakeDAO = new TiersDAO() {
            @Override public List<Tiers> findAll() {
                return Arrays.asList(new Tiers(), new Tiers()); // Simule 2 tiers en base
            }
        };
        TiersService service = new TiersService(fakeDAO);

        List<Tiers> resultats = service.obtenirTousLesTiers();

        // 2ème type : assertNotNull
        assertNotNull(resultats, "La liste ne doit pas être nulle");
        // 3ème type : assertEquals
        assertEquals(2, resultats.size(), "Il devrait y avoir exactement 2 tiers dans la liste");
    }


    @Test
    void ajouterTiers_siretMauvaisFormat_doitLeverException() {
        // Cas d'Erreur
        TiersService service = new TiersService(null);
        Tiers t = new Tiers(0, "Apple", "Steve", null, "contact@apple.com", "Cupertino", "0102030405", "123A", "FR123"); // Mauvais SIRET

        // 5ème type : assertThrows
        assertThrows(IllegalArgumentException.class, () -> service.ajouterTiers(t));
    }

    @Test
    void validerTiers_nomVide_doitLeverException() {
        // Cas d'Erreur
        TiersService service = new TiersService(null);
        Tiers t = new Tiers(0, "", "Steve", null, "contact@apple.com", "Cupertino", "0102030405", "12345678901234", "FR123"); // Nom vide

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.ajouterTiers(t));
        assertEquals("Le nom du tiers est obligatoire.", exception.getMessage());
    }
}
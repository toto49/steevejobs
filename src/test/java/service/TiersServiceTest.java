package service;

import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Enum.TiersType;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.service.TiersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TiersServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private TiersDAO tiersDAO;

    private TiersService service;

    @BeforeEach
    void setUp() {
        service = new TiersService(tiersDAO);
    }

    private Tiers tiersValide() {
        Tiers tiers = new Tiers();
        tiers.setNom("Fournisseur A");
        tiers.setType(TiersType.FOURNISSEUR);
        tiers.setEmail("contact@fournisseur.fr");
        tiers.setSiret("12345678901234");
        return tiers;
    }

    @Test
    void ajouterTiers_donneesValides_neDoitPasLeverException() throws SQLException {
        Tiers tiers = tiersValide();
        when(tiersDAO.emailExists(tiers.getEmail())).thenReturn(false);
        when(tiersDAO.siretExists(tiers.getSiret())).thenReturn(false);
        when(tiersDAO.createTiers(tiers)).thenReturn(true);

        assertDoesNotThrow(() -> service.ajouterTiers(tiers));
        verify(tiersDAO).createTiers(tiers);
    }

    @Test
    void ajouterTiers_tiersNull_doitLeverException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.ajouterTiers(null));
        assertEquals("Les données du tiers sont vides.", ex.getMessage());
    }

    @Test
    void validerTiers_emailInvalide_doitLeverException() {
        Tiers tiers = tiersValide();
        tiers.setEmail("contact-fournisseur.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.ajouterTiers(tiers));
        assertEquals("Le format de l'adresse email est invalide.", ex.getMessage());
    }

    @Test
    void validerTiers_siretInvalide_doitLeverException() {
        Tiers tiers = tiersValide();
        tiers.setSiret("1234567890123A");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.ajouterTiers(tiers));
        assertEquals("Le SIRET doit contenir exactement 14 chiffres.", ex.getMessage());
    }

    @Test
    void supprimerTiers_idInvalide_doitLeverException() throws SQLException {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.supprimerTiers(0));
        assertEquals("L'ID du tiers est invalide.", ex.getMessage());
        verify(tiersDAO, never()).deleteTiers(anyInt());
    }

}

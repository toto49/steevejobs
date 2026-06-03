package service;

import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.service.ProduitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.ProduitService}.
 * <p>
 * Couvre ajout, validation du nom, mise à jour de stock et contrôles d'identifiant.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private ProduitDAO produitDAO;

    private ProduitService service;

    @BeforeEach
    void setUp() {
        service = new ProduitService(produitDAO);
    }

    private Produit produitUnitaire(int id, int quantite) {
        return new Produit(id, "Pomme", BigDecimal.TEN, BigDecimal.valueOf(20), quantite, BigDecimal.ZERO, true, 5);
    }

    @Test
    void ajouterProduit_donneesValides_neDoitPasLeverException() throws SQLException {
        Produit produit = produitUnitaire(0, 10);
        when(produitDAO.createProduit(produit)).thenReturn(true);

        assertDoesNotThrow(() -> service.ajouterProduit(produit));
        verify(produitDAO).createProduit(produit);
    }

    @Test
    void ajouterProduit_nomVide_doitLeverException() {
        Produit produit = new Produit(0, "", BigDecimal.TEN, BigDecimal.valueOf(20), 10, BigDecimal.ZERO, true, 5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.ajouterProduit(produit));
        assertEquals("Le nom du produit est obligatoire.", ex.getMessage());
    }

    @Test
    void mettreAJourStock_stockNegatif_doitLeverException() throws SQLException {
        when(produitDAO.getById(1)).thenReturn(produitUnitaire(1, 5));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.mettreAJourStock(1, -10));
        assertEquals("Le stock ne peut pas être négatif !", ex.getMessage());
        verify(produitDAO, never()).updateStock(anyInt(), anyInt());
    }

    @Test
    void mettreAJourStock_variationPositive_doitMettreAJourLeStock() throws SQLException {
        when(produitDAO.getById(1)).thenReturn(produitUnitaire(1, 5));
        when(produitDAO.updateStock(1, 8)).thenReturn(true);

        assertDoesNotThrow(() -> service.mettreAJourStock(1, 3));
        verify(produitDAO).updateStock(1, 8);
    }

    @Test
    void modifierProduit_idInvalide_doitLeverException() {
        Produit produit = produitUnitaire(-1, 5);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.modifierProduit(produit));
        assertTrue(ex.getMessage().contains("ID du produit est invalide"));
    }

    @Test
    void supprimerProduit_idInvalide_doitLeverException() {
        assertThrows(IllegalArgumentException.class, () -> service.supprimerProduit(0));
    }

}

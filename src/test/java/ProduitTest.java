import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.service.ProduitService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProduitTest {

    @Test
    void ajouterProduit_prixNegatif_doitLeverException() {
        ProduitService service = new ProduitService(null);
        Produit p = new Produit(0, "MacBook", BigDecimal.valueOf(-100), BigDecimal.valueOf(20), 5, BigDecimal.ONE, true);

        assertThrows(IllegalArgumentException.class, () -> service.ajouterProduit(p));
    }

    @Test
    void ajouterProduit_donneesValides_neDoitPasPlanter() {
        ProduitDAO fakeDAO = new ProduitDAO() {
            @Override public boolean createProduit(Produit p) { return true; }
        };
        ProduitService service = new ProduitService(fakeDAO);
        Produit p = new Produit(0, "MacBook", BigDecimal.valueOf(1000), BigDecimal.valueOf(20), 5, BigDecimal.ONE, true);

        // assertDoesNotThrow : Vérifie que le chemin nominal s'exécute sans exception
        assertDoesNotThrow(() -> service.ajouterProduit(p));
    }

    @Test
    void mettreAJourStock_ajoutPositif_doitCalculerLeBonStock() throws SQLException {
        class SpyProduitDAO extends ProduitDAO {
            public int stockEnregistre = -1;
            @Override public Produit getById(int id) {
                return new Produit(1, "Clavier", BigDecimal.TEN, BigDecimal.valueOf(20), 5, BigDecimal.ONE, true); // Stock initial = 5
            }
            @Override public boolean updateStock(int idProduit, int nouveauStock) {
                this.stockEnregistre = nouveauStock;
                return true;
            }
        }

        SpyProduitDAO spyDAO = new SpyProduitDAO();
        ProduitService service = new ProduitService(spyDAO);

        service.mettreAJourStock(1, 10); // 5 + 10 = 15

        assertEquals(15, spyDAO.stockEnregistre, "Le DAO a dû recevoir la valeur 15");
    }

    @Test
    void supprimerProduit_idInvalide_doitLeverException() {
        ProduitService service = new ProduitService(null);
        assertThrows(IllegalArgumentException.class, () -> service.supprimerProduit(-5));
    }

    @Test
    void obtenirTousLesProduits_doitRetournerListeGeree() throws SQLException {
        ProduitDAO fakeDAO = new ProduitDAO() {
            @Override public List<Produit> findAll() {
                return List.of(new Produit(1, "Souris", BigDecimal.TEN, BigDecimal.ZERO, 10, BigDecimal.ONE, true),new Produit(2, "Clavier", BigDecimal.TEN, BigDecimal.ZERO, 12, BigDecimal.ONE, true));
            }
        };
        ProduitService service = new ProduitService(fakeDAO);

        List<Produit> resultats = service.obtenirTousLesProduits();

        assertNotNull(resultats);
        assertEquals(2, resultats.size(), "La liste doit contenir 2 élément");
    }
}
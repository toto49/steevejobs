package dao;

import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.model.Produit;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du {@link com.eseo.steevejobs.dao.ProduitDAO}.
 * <p>
 * Vérifie création, mise à jour de stock et recherche par nom.
 * </p>
 */
@ExtendWith(DaoIntegrationExtension.class)
class ProduitDAOTest {

    private ProduitDAO produitDAO;

    @BeforeEach
    void setUp() {
        produitDAO = new ProduitDAO();
    }

    @Test
    void createProduit_puisGetById_retourneProduit() throws SQLException {
        Produit produit = DaoTestFixtures.insertProduit(produitDAO);

        Produit lu = produitDAO.getById(produit.getId());

        assertNotNull(lu);
        assertEquals(produit.getNom(), lu.getNom());
    }

    @Test
    void updateStock_modifieQuantite() throws SQLException {
        Produit produit = DaoTestFixtures.insertProduit(produitDAO);

        assertTrue(produitDAO.updateStock(produit.getId(), 99));

        assertEquals(99, produitDAO.getById(produit.getId()).getQuantite());
    }

    @Test
    void getByNom_retourneProduit() throws SQLException {
        Produit produit = DaoTestFixtures.insertProduit(produitDAO);

        Produit lu = produitDAO.getByNom(produit.getNom());

        assertNotNull(lu);
        assertEquals(produit.getId(), lu.getId());
    }
}

package service;

import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.service.ProduitService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProduitServiceTest {

    @Test
    void ajouterProduit_nomVide_doitLeverException() {
        // On passe null car l'exception de validation se déclenche avant d'atteindre le DAO
        ProduitService service = new ProduitService(null);
        Produit p = new Produit(0, "", BigDecimal.TEN, BigDecimal.valueOf(20), 5, BigDecimal.ONE, true);

        assertThrows(IllegalArgumentException.class, () -> service.ajouterProduit(p));
    }

    @Test
    void ajouterProduit_prixNegatif_doitLeverException() {
        ProduitService service = new ProduitService(null);
        Produit p = new Produit(0, "MacBook", BigDecimal.valueOf(-100), BigDecimal.valueOf(20), 5, BigDecimal.ONE, true);

        assertThrows(IllegalArgumentException.class, () -> service.ajouterProduit(p));
    }

    @Test
    void ajouterProduit_quantiteNegative_doitLeverException() {
        ProduitService service = new ProduitService(null);
        Produit p = new Produit(0, "MacBook", BigDecimal.TEN, BigDecimal.valueOf(20), -5, BigDecimal.ONE, true);

        assertThrows(IllegalArgumentException.class, () -> service.ajouterProduit(p));
    }

    @Test
    void mettreAJourStock_stockNegatifResultant_doitLeverException() throws SQLException {
        // On crée un faux DAO manuel juste pour ce test
        ProduitDAO fakeDAO = new ProduitDAO() {
            @Override
            public Produit getById(int id) {
                // Simule que le produit existe en base avec un stock initial de 5
                return new Produit(1, "Clavier", BigDecimal.TEN, BigDecimal.valueOf(20), 5, BigDecimal.ONE, true);
            }
        };

        // On injecte notre faux DAO dans le service
        ProduitService service = new ProduitService(fakeDAO);

        // Retrait de 10 sur un stock de 5 : doit déclencher l'exception
        assertThrows(IllegalArgumentException.class, () -> service.mettreAJourStock(1, -10));
    }
}
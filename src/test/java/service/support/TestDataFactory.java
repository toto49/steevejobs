package service.support;

import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static User utilisateurActif(int id, String email) {
        return new User(id, "Dupont", "Jean", email, "hash123", "Adresse", "EMPLOYE", "0102030405", "Dev", true);
    }

    public static Produit produitUnitaire(int id, int quantite) {
        return new Produit(id, "Pomme", BigDecimal.TEN, BigDecimal.valueOf(20), quantite, BigDecimal.ZERO, true, 5);
    }

    public static Tiers tiersValide() {
        Tiers tiers = new Tiers();
        tiers.setId(1);
        tiers.setNom("Client A");
        tiers.setEmail("client@mail.fr");
        tiers.setSiret("12345678901234");
        return tiers;
    }

    public static Document documentValide() {
        Tiers tiers = tiersValide();
        User editeur = utilisateurActif(1, "editeur@mail.fr");
        return new Document(
                0,
                DocumentType.DEVIS,
                LocalDateTime.of(2026, 5, 10, 10, 0),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(120),
                DocumentStatut.EN_ATTENTE,
                "",
                tiers,
                editeur
        );
    }
}

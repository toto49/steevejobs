package service.support;

import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fabrique d'objets métier minimaux pour les tests de service et de contrôleur (sans accès base).
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    /**
     * Construit un utilisateur actif avec identifiant et e-mail paramétrables.
     *
     * @param id    identifiant technique
     * @param email adresse e-mail
     * @return instance {@link User} valide pour les scénarios de test
     */
    public static User utilisateurActif(int id, String email) {
        return new User(id, "Dupont", "Jean", email, "hash123", "Adresse", "EMPLOYE", "0102030405", "Dev", true);
    }

    /**
     * Construit un produit unitaire avec quantité en stock donnée.
     *
     * @param id        identifiant produit
     * @param quantite  stock initial
     * @return instance {@link Produit}
     */
    public static Produit produitUnitaire(int id, int quantite) {
        return new Produit(id, "Pomme", BigDecimal.TEN, BigDecimal.valueOf(20), quantite, BigDecimal.ZERO, true, 5);
    }

    /**
     * Construit un tiers client avec SIRET et e-mail fixes.
     *
     * @return instance {@link Tiers} préremplie
     */
    public static Tiers tiersValide() {
        Tiers tiers = new Tiers();
        tiers.setId(1);
        tiers.setNom("Client A");
        tiers.setEmail("client@mail.fr");
        tiers.setSiret("12345678901234");
        return tiers;
    }

    /**
     * Construit un devis en attente lié à un tiers et un éditeur de test.
     *
     * @return instance {@link Document} sans identifiant persisté
     */
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

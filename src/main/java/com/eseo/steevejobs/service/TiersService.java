package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.model.Tiers;

import java.sql.SQLException;
import java.util.List;

public class TiersService {

    private TiersDAO tiersDAO;

    public TiersService() {
        this.tiersDAO = new TiersDAO();
    }

    // --------------------------------------------------------
    // MÉTHODES PUBLIQUES
    // --------------------------------------------------------

    public void ajouterTiers(Tiers tiers) throws IllegalArgumentException, SQLException {
        // 1. On vérifie les règles métier
        validerTiers(tiers);

        // 2. On envoie au DAO
        boolean success = tiersDAO.createTiers(tiers);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter le tiers.");
        }

    }

    public void modifierTiers(Tiers tiers) throws IllegalArgumentException, SQLException {
        // 1. L'ID doit exister pour une modification
        if (tiers.getId() <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }

        // 2. On vérifie les règles métier
        validerTiers(tiers);

        // 3. On envoie au DAO
        boolean success = tiersDAO.updateTiers(tiers);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de mettre à jour le tiers.");
        }
    }

    public void supprimerTiers(int idTiers) throws SQLException {
        if (idTiers <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }

        boolean success = tiersDAO.deleteTiers(idTiers);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de supprimer ce tiers. Il est peut-être lié à d'autres données.");
        }
    }

    public List<Tiers> obtenirTousLesTiers() throws SQLException {
         return tiersDAO.findAll();
    }

    // --------------------------------------------------------
    // MÉTHODES PRIVÉES
    // --------------------------------------------------------

    private void validerTiers(Tiers tiers) throws IllegalArgumentException {
        if (tiers == null) {
            throw new IllegalArgumentException("Les données du tiers sont vides.");
        }

        // Règle 1 : Le nom est obligatoire
        if (tiers.getNom() == null || tiers.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du tiers est obligatoire.");
        }

        // Règle 2 : Format de l'email (vérification basique)
        if (tiers.getEmail() != null && !tiers.getEmail().trim().isEmpty()) {
            if (!tiers.getEmail().contains("@") || !tiers.getEmail().contains(".")) {
                throw new IllegalArgumentException("Le format de l'adresse email est invalide.");
            }
        }

        // Règle 3 : Format du SIRET (14 chiffres en France)
        if (tiers.getSiret() != null && !tiers.getSiret().trim().isEmpty()) {
            if (tiers.getSiret().length() != 14 || !tiers.getSiret().matches("\\d+")) {
                throw new IllegalArgumentException("Le SIRET doit contenir exactement 14 chiffres.");
            }
        }
    }
}
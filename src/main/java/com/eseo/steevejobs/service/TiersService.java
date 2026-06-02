package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.TiersType;

import java.sql.SQLException;
import java.util.List;

public class TiersService {

    private final TiersDAO tiersDAO;

    public TiersService() {
        this.tiersDAO = new TiersDAO();
    }

    public TiersService(TiersDAO tiersDAO) {
        this.tiersDAO = tiersDAO;
    }

    // --------------------------------------------------------
    // MÉTHODES PUBLIQUES
    // --------------------------------------------------------

    public void ajouterTiers(Tiers tiers) throws IllegalArgumentException, SQLException {
        validerTiers(tiers);

        if (tiersDAO.emailExists(tiers.getEmail())) {
            throw new IllegalArgumentException("Un tiers avec cet email existe déjà.");
        }

        if (tiers.getSiret() != null && !tiers.getSiret().trim().isEmpty()
                && tiersDAO.siretExists(tiers.getSiret())) {
            throw new IllegalArgumentException("Un tiers avec ce numéro SIRET existe déjà.");
        }

        boolean success = tiersDAO.createTiers(tiers);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter le tiers.");
        }
    }

    public void modifierTiers(Tiers tiers) throws IllegalArgumentException, SQLException {
        if (tiers.getId() <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide pour une modification.");
        }

        validerTiers(tiers);

        // Vérifier l'unicité de l'email sans compter le tiers lui-même
        Tiers tiersAvecEmail = tiersDAO.getByEmail(tiers.getEmail());
        if (tiersAvecEmail != null && tiersAvecEmail.getId() != tiers.getId()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé par un autre tiers.");
        }

        boolean success = tiersDAO.updateTiers(tiers);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de mettre à jour ce tiers.");
        }
    }

    public void supprimerTiers(int idTiers) throws IllegalArgumentException, SQLException {
        if (idTiers <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }

        boolean success = tiersDAO.deleteTiers(idTiers);
        if (!success) {
            throw new RuntimeException(
                    "Erreur BDD : Impossible de supprimer ce tiers. Il est peut-être lié à des documents.");
        }
    }

    public Tiers getTiersById(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }
        return tiersDAO.getById(id);
    }

    public List<Tiers> findAll() throws SQLException {
        return tiersDAO.findAll();
    }

    public List<Tiers> obtenirTousLesTiers() throws SQLException {
        return tiersDAO.findAll();
    }

    public List<Tiers> obtenirTiersParType(TiersType type) throws IllegalArgumentException, SQLException {
        if (type == null) {
            throw new IllegalArgumentException("Le type de tiers est obligatoire.");
        }
        return tiersDAO.findByType(type);
    }

    public boolean activerTiers(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }
        return tiersDAO.activateTiers(id);
    }

    public boolean desactiverTiers(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }
        return tiersDAO.deactivateTiers(id);
    }

    // --------------------------------------------------------
    // MÉTHODES PRIVÉES (Logique métier interne)
    // --------------------------------------------------------

    private void validerTiers(Tiers tiers) throws IllegalArgumentException {
        if (tiers == null) {
            throw new IllegalArgumentException("Les données du tiers sont vides.");
        }

        if (tiers.getNom() == null || tiers.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du tiers est obligatoire.");
        }

        if (tiers.getNom().trim().length() > 255) {
            throw new IllegalArgumentException("Le nom du tiers ne peut pas dépasser 255 caractères.");
        }

        if (tiers.getType() == null) {
            throw new IllegalArgumentException("Le type du tiers (CLIENT ou FOURNISSEUR) est obligatoire.");
        }

        if (tiers.getEmail() == null || tiers.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email du tiers est obligatoire.");
        }

        if (!tiers.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            throw new IllegalArgumentException("Le format de l'adresse email est invalide.");
        }

        if (tiers.getSiret() != null && !tiers.getSiret().trim().isEmpty()) {
            if (!tiers.getSiret().matches("\\d{14}")) {
                throw new IllegalArgumentException("Le SIRET doit contenir exactement 14 chiffres.");
            }
        }

        if (tiers.getNum_tva() != null && !tiers.getNum_tva().trim().isEmpty()) {
            // Format TVA intracommunautaire : 2 lettres + 2 chiffres + 9 chiffres (France : FR + clé + SIREN)
            if (!tiers.getNum_tva().matches("^[A-Z]{2}[0-9A-Z]{2}[0-9]{9}$")) {
                throw new IllegalArgumentException(
                        "Le numéro de TVA intracommunautaire est invalide (ex : FR12345678901).");
            }
        }
    }
}

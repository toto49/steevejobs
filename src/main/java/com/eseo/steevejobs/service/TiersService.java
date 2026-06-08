package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.TiersType;

import java.sql.SQLException;
import java.util.List;

/**
 * Gestion des tiers clients et fournisseurs.
 * <p>
 * Règles métier : email unique à la création ; SIRET 14 chiffres si renseigné ;
 * numéro de TVA au format intracommunautaire ; unicité de l'email à la modification
 * hors enregistrement courant. Aucun effet de bord réseau.
 * </p>
 */
public class TiersService {

    /** Accès persistance aux tiers clients et fournisseurs. */
    private final TiersDAO tiersDAO;

    /**
     * Constructeur par défaut.
     */
    public TiersService() {
        this.tiersDAO = new TiersDAO();
    }

    /**
     * Constructeur avec injection du DAO.
     *
     * @param tiersDAO accès persistance tiers
     */
    public TiersService(TiersDAO tiersDAO) {
        this.tiersDAO = tiersDAO;
    }

    /**
     * Crée un tiers après validation et contrôle d'unicité email/SIRET.
     *
     * @param tiers entité à persister
     * @throws IllegalArgumentException si données invalides ou doublon email/SIRET
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si l'insertion échoue
     */
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

    /**
     * Met à jour un tiers existant.
     *
     * @param tiers tiers avec identifiant valide
     * @throws IllegalArgumentException si données invalides ou email déjà pris
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la mise à jour échoue
     */
    public void modifierTiers(Tiers tiers) throws IllegalArgumentException, SQLException {
        if (tiers.getId() <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide pour une modification.");
        }

        validerTiers(tiers);

        Tiers tiersAvecEmail = tiersDAO.getByEmail(tiers.getEmail());
        if (tiersAvecEmail != null && tiersAvecEmail.getId() != tiers.getId()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé par un autre tiers.");
        }

        boolean success = tiersDAO.updateTiers(tiers);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de mettre à jour ce tiers.");
        }
    }

    /**
     * Supprime un tiers.
     *
     * @param idTiers identifiant du tiers
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si suppression impossible (documents liés)
     */
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

    /**
     * Charge un tiers par identifiant.
     *
     * @param id identifiant tiers
     * @return tiers ou {@code null} selon le DAO
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public Tiers getTiersById(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }
        return tiersDAO.getById(id);
    }

    /**
     * Liste tous les tiers.
     *
     * @return liste complète
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<Tiers> findAll() throws SQLException {
        return tiersDAO.findAll();
    }

    /**
     * Alias de {@link #findAll()}.
     *
     * @return liste complète des tiers
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<Tiers> obtenirTousLesTiers() throws SQLException {
        return tiersDAO.findAll();
    }

    /**
     * Filtre les tiers par type (client ou fournisseur).
     *
     * @param type type de tiers
     * @return liste filtrée
     * @throws IllegalArgumentException si le type est null
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public List<Tiers> obtenirTiersParType(TiersType type) throws IllegalArgumentException, SQLException {
        if (type == null) {
            throw new IllegalArgumentException("Le type de tiers est obligatoire.");
        }
        return tiersDAO.findByType(type);
    }

    /**
     * Réactive un tiers désactivé.
     *
     * @param id identifiant tiers
     * @return {@code true} si l'activation a réussi
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public boolean activerTiers(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }
        return tiersDAO.activateTiers(id);
    }

    /**
     * Désactive un tiers sans le supprimer.
     *
     * @param id identifiant tiers
     * @return {@code true} si la désactivation a réussi
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public boolean desactiverTiers(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }
        return tiersDAO.deactivateTiers(id);
    }

    /**
     * Valide les champs obligatoires et les formats d'un tiers client ou fournisseur.
     *
     * @param tiers entité tiers à contrôler
     * @throws IllegalArgumentException si nom, type, e-mail, SIRET ou numéro de TVA sont invalides
     */
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
            if (!tiers.getNum_tva().matches("^[A-Z]{2}[0-9A-Z]{2}[0-9]{9}$")) {
                throw new IllegalArgumentException(
                        "Le numéro de TVA intracommunautaire est invalide (ex : FR12345678901).");
            }
        }
    }
}

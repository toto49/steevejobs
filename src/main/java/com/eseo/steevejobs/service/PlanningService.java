package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.Planning;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CRUD des événements de planning employé (réunions, congés, etc.).
 * <p>
 * Règles métier : utilisateur et type obligatoires ; fin ≥ début ;
 * durée maximale 365 jours ; début pas antérieur à plus de 2 ans.
 * Aucun effet de bord réseau.
 * </p>
 */
public class PlanningService {

    private final PlanningDAO planningDAO;

    /**
     * Constructeur avec injection du DAO.
     *
     * @param planningDAO accès persistance planning
     */
    public PlanningService(PlanningDAO planningDAO) {
        this.planningDAO = planningDAO;
    }

    /**
     * Constructeur par défaut.
     */
    public PlanningService() {
        this(new PlanningDAO());
    }

    /**
     * Crée un événement planning après validation.
     *
     * @param planning événement à insérer (identifiant renseigné après succès)
     * @throws IllegalArgumentException si les données sont invalides
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si l'insertion échoue
     */
    public void ajouterPlanning(Planning planning) throws IllegalArgumentException, SQLException {
        validerPlanning(planning);

        boolean success = planningDAO.createPlanning(planning);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter cet événement au planning.");
        }
    }

    /**
     * Met à jour un événement existant.
     *
     * @param planning événement avec identifiant valide
     * @throws IllegalArgumentException si identifiant ou données invalides
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la mise à jour échoue
     */
    public void modifierPlanning(Planning planning) throws IllegalArgumentException, SQLException {
        if (planning.getId() <= 0) {
            throw new IllegalArgumentException("L'ID du planning est invalide pour une modification.");
        }

        validerPlanning(planning);

        boolean success = planningDAO.updatePlanning(planning);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de mettre à jour cet événement.");
        }
    }

    /**
     * Supprime un événement par identifiant.
     *
     * @param idPlanning identifiant de l'événement
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la suppression échoue
     */
    public void supprimerPlanning(int idPlanning) throws IllegalArgumentException, SQLException {
        if (idPlanning <= 0) {
            throw new IllegalArgumentException("L'ID du planning est invalide.");
        }

        boolean success = planningDAO.deletePlanning(idPlanning);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de supprimer cet événement.");
        }
    }

    /**
     * Charge un événement par identifiant.
     *
     * @param id identifiant planning
     * @return événement ou {@code null} selon le DAO
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public Planning getPlanningById(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du planning est invalide.");
        }
        return planningDAO.getById(id);
    }

    /**
     * Liste le planning d'un utilisateur.
     *
     * @param userId identifiant employé
     * @return événements de l'utilisateur
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public List<Planning> obtenirPlanningsParUtilisateur(int userId) throws IllegalArgumentException, SQLException {
        if (userId <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur est invalide.");
        }
        return planningDAO.findByUserId(userId);
    }

    /**
     * Liste tous les événements planning.
     *
     * @return liste globale
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<Planning> obtenirTousLesPlannings() throws SQLException {
        return planningDAO.findAll();
    }

    private void validerPlanning(Planning planning) throws IllegalArgumentException {
        if (planning == null) {
            throw new IllegalArgumentException("Les données du planning sont vides.");
        }

        if (planning.getUser() == null || planning.getUser().getId() <= 0) {
            throw new IllegalArgumentException("L'utilisateur associé au planning est obligatoire.");
        }

        if (planning.getType() == null || planning.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Le type d'événement est obligatoire.");
        }

        if (planning.getType().trim().length() > 255) {
            throw new IllegalArgumentException("Le type d'événement ne peut pas dépasser 255 caractères.");
        }

        if (planning.getJourDebut() == null) {
            throw new IllegalArgumentException("La date de début est obligatoire.");
        }

        if (planning.getJourFin() == null) {
            throw new IllegalArgumentException("La date de fin est obligatoire.");
        }

        if (planning.getJourFin().isBefore(planning.getJourDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }

        long joursEcart = java.time.temporal.ChronoUnit.DAYS.between(
                planning.getJourDebut(), planning.getJourFin());
        if (joursEcart > 365) {
            throw new IllegalArgumentException("Un événement ne peut pas durer plus de 365 jours.");
        }

        if (planning.getJourDebut().isBefore(LocalDateTime.now().minusYears(2))) {
            throw new IllegalArgumentException(
                    "La date de début ne peut pas être antérieure à 2 ans dans le passé.");
        }
    }
}

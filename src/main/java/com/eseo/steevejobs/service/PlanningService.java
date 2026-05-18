package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.Planning;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class PlanningService {

    private final PlanningDAO planningDAO;

    public PlanningService(PlanningDAO planningDAO) {
        this.planningDAO = planningDAO;
    }

    // --------------------------------------------------------
    // MÉTHODES PUBLIQUES (Appelées par tes contrôleurs JavaFX)
    // --------------------------------------------------------

    public void ajouterPlanning(Planning planning) throws SQLException {

        validerPlanning(planning);

        boolean success = planningDAO.createPlanning(planning);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter cette case au planning.");
        }
    }

    public void updatePlanning(Planning planning) throws SQLException {

        validerPlanning(planning);

        boolean success = planningDAO.updatePlanning(planning);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de mettre a jour cette case du planning.");
        }
    }

    public void deletePlanning(int idPlanning) throws SQLException {

        boolean success = planningDAO.deletePlanning(idPlanning);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de supprimer cette case du planning.");
        }
    }

    public List<Planning> findByUserId(int userId) throws SQLException {
        return planningDAO.findByUserId(userId);
    }

    // --------------------------------------------------------
    // MÉTHODES PRIVÉES (Logique métier interne)
    // --------------------------------------------------------

    private void validerPlanning(Planning planning) {
        if (planning == null) {
            throw new IllegalArgumentException("Les données du produit sont vides.");
        }

        if (planning.getType() == null || planning.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Le type de planning est obligatoire.");
        }

        if (planning.getJourFin() == null) {
            throw new IllegalArgumentException("La date de fin est obligatoire.");
        }

        if (planning.getJourDebut() == null) {
            throw new IllegalArgumentException("La date de début est obligatoire.");
        }

        if (planning.getUser() == null) {
            throw new IllegalArgumentException("L'utilisateur est obligatoire.");
        }

        if (planning.getJourFin().isBefore(planning.getJourDebut())){
            throw  new IllegalArgumentException("La date de fin doit être plus tard que la date de début");
        }
    }
}

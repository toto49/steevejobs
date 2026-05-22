package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.HeuresTravailDAO;
import com.eseo.steevejobs.model.HeuresTravail;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public class HeuresTravailService {

    private final HeuresTravailDAO heuresTravailDAO;

    public HeuresTravailService() {
        this.heuresTravailDAO = new HeuresTravailDAO();
    }

    public HeuresTravailService(HeuresTravailDAO heuresTravailDAO) {
        this.heuresTravailDAO = heuresTravailDAO;
    }

    // --------------------------------------------------------
    // MÉTHODES PUBLIQUES
    // --------------------------------------------------------

    public boolean sauvegarderHeures(int idUser, LocalDate dateJour,
                                     LocalTime debutM, LocalTime finM,
                                     LocalTime debutA, LocalTime finA,
                                     LocalTime tTotal) throws IllegalArgumentException, SQLException {

        validerParametresSaisie(idUser, dateJour, debutM, finM, debutA, finA);

        return heuresTravailDAO.sauvegarder(idUser, dateJour, debutM, finM, debutA, finA, tTotal);
    }

    public HeuresTravail getHeuresParDate(int idUser, LocalDate dateJour)
            throws IllegalArgumentException, SQLException {

        if (idUser <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur est invalide.");
        }
        if (dateJour == null) {
            throw new IllegalArgumentException("La date est obligatoire.");
        }
        if (dateJour.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Impossible de consulter les heures d'une journée future.");
        }

        return heuresTravailDAO.getHeuresParDate(idUser, dateJour);
    }

    // --------------------------------------------------------
    // MÉTHODES PRIVÉES (Logique métier interne)
    // --------------------------------------------------------

    private void validerParametresSaisie(int idUser, LocalDate dateJour,
                                         LocalTime debutM, LocalTime finM,
                                         LocalTime debutA, LocalTime finA)
            throws IllegalArgumentException {

        if (idUser <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur est invalide.");
        }

        if (dateJour == null) {
            throw new IllegalArgumentException("La date de saisie est obligatoire.");
        }

        if (dateJour.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Impossible de saisir des heures pour une journée future.");
        }

        // Validation de la plage matin si renseignée
        if (debutM != null && finM != null) {
            if (!finM.isAfter(debutM)) {
                throw new IllegalArgumentException(
                        "L'heure de fin de matin doit être postérieure à l'heure de début.");
            }
            // Créneau matin : bornes raisonnables (06:00 - 14:00)
            if (debutM.isBefore(LocalTime.of(6, 0)) || finM.isAfter(LocalTime.of(14, 0))) {
                throw new IllegalArgumentException(
                        "Le créneau matin doit être compris entre 06:00 et 14:00.");
            }
        } else if (debutM != null || finM != null) {
            throw new IllegalArgumentException(
                    "Les heures de début et de fin de matin doivent être toutes deux renseignées ou vides.");
        }

        // Validation de la plage après-midi si renseignée
        if (debutA != null && finA != null) {
            if (!finA.isAfter(debutA)) {
                throw new IllegalArgumentException(
                        "L'heure de fin d'après-midi doit être postérieure à l'heure de début.");
            }
            // Créneau après-midi : bornes raisonnables (12:00 - 22:00)
            if (debutA.isBefore(LocalTime.of(12, 0)) || finA.isAfter(LocalTime.of(22, 0))) {
                throw new IllegalArgumentException(
                        "Le créneau après-midi doit être compris entre 12:00 et 22:00.");
            }
        } else if (debutA != null || finA != null) {
            throw new IllegalArgumentException(
                    "Les heures de début et de fin d'après-midi doivent être toutes deux renseignées ou vides.");
        }

        // Vérifier que l'après-midi ne chevauche pas le matin
        if (finM != null && debutA != null && debutA.isBefore(finM)) {
            throw new IllegalArgumentException(
                    "Le créneau après-midi ne peut pas commencer avant la fin du créneau matin.");
        }

        // Au moins un créneau doit être renseigné
        if (debutM == null && debutA == null) {
            throw new IllegalArgumentException(
                    "Au moins un créneau (matin ou après-midi) doit être renseigné.");
        }
    }
}

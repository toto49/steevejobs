package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.DatabaseConnection;
import com.eseo.steevejobs.dao.HeuresTravailDAO;
import com.eseo.steevejobs.model.HeuresTravail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Saisie et consultation des heures de travail par employé et par jour.
 * <p>
 * Règles métier : interdiction des dates futures ; créneaux matin (06:00–14:00)
 * et après-midi (12:00–22:00) cohérents ; au moins un créneau renseigné ;
 * l'après-midi ne peut pas commencer avant la fin du matin.
 * </p>
 */
public class HeuresTravailService {

    private final HeuresTravailDAO heuresTravailDAO;

    /**
     * Constructeur par défaut.
     */
    public HeuresTravailService() {
        this.heuresTravailDAO = new HeuresTravailDAO();
    }

    /**
     * Constructeur avec injection du DAO.
     *
     * @param heuresTravailDAO accès persistance des heures
     */
    public HeuresTravailService(HeuresTravailDAO heuresTravailDAO) {
        this.heuresTravailDAO = heuresTravailDAO;
    }

    /**
     * Enregistre ou met à jour les heures d'une journée après validation des créneaux.
     *
     * @param idUser   identifiant de l'employé
     * @param dateJour date de la journée (non future)
     * @param debutM   début matin (peut être null si créneau matin absent)
     * @param finM     fin matin
     * @param debutA   début après-midi
     * @param finA     fin après-midi
     * @param tTotal   durée totale calculée côté présentation
     * @return {@code true} si la persistance a réussi
     * @throws IllegalArgumentException si les paramètres ou créneaux sont invalides
     * @throws SQLException           en cas d'erreur d'accès base
     */
    public boolean sauvegarderHeures(int idUser, LocalDate dateJour,
                                     LocalTime debutM, LocalTime finM,
                                     LocalTime debutA, LocalTime finA,
                                     LocalTime tTotal) throws IllegalArgumentException, SQLException {

        validerParametresSaisie(idUser, dateJour, debutM, finM, debutA, finA);
        return heuresTravailDAO.sauvegarder(idUser, dateJour, debutM, finM, debutA, finA, tTotal);
    }

    /**
     * Retourne les heures enregistrées pour un employé à une date donnée.
     *
     * @param idUser   identifiant de l'employé
     * @param dateJour date consultée (non future)
     * @return enregistrement des heures, ou {@code null} selon le DAO
     * @throws IllegalArgumentException si identifiant ou date invalides
     * @throws SQLException           en cas d'erreur d'accès base
     */
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

    /**
     * Agrège le total d'heures travaillées sur un mois civil (requête SQL directe).
     *
     * @param userId identifiant de l'employé
     * @param annee  année (ex. 2026)
     * @param mois   mois (1–12)
     * @return total en heures décimales ; 0 si aucune saisie
     * @throws SQLException en cas d'erreur d'accès base
     */
    public double getTotalHeuresByMonth(int userId, int annee, int mois) throws SQLException {
        double total = 0;
        String sql = "SELECT SUM(TIME_TO_SEC(heures_total)) / 3600 as total_heures " +
                "FROM HEURES_TRAVAIL " +
                "WHERE id_user = ? AND YEAR(date_jour) = ? AND MONTH(date_jour) = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, annee);
            stmt.setInt(3, mois);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    total = rs.getDouble("total_heures");
                }
            }
        }
        return total;
    }

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

        if (debutM != null && finM != null) {
            if (!finM.isAfter(debutM)) {
                throw new IllegalArgumentException(
                        "L'heure de fin de matin doit être postérieure à l'heure de début.");
            }
            if (debutM.isBefore(LocalTime.of(6, 0)) || finM.isAfter(LocalTime.of(14, 0))) {
                throw new IllegalArgumentException(
                        "Le créneau matin doit être compris entre 06:00 et 14:00.");
            }
        } else if (debutM != null || finM != null) {
            throw new IllegalArgumentException(
                    "Les heures de début et de fin de matin doivent être toutes deux renseignées ou vides.");
        }

        if (debutA != null && finA != null) {
            if (!finA.isAfter(debutA)) {
                throw new IllegalArgumentException(
                        "L'heure de fin d'après-midi doit être postérieure à l'heure de début.");
            }
            if (debutA.isBefore(LocalTime.of(12, 0)) || finA.isAfter(LocalTime.of(22, 0))) {
                throw new IllegalArgumentException(
                        "Le créneau après-midi doit être compris entre 12:00 et 22:00.");
            }
        } else if (debutA != null || finA != null) {
            throw new IllegalArgumentException(
                    "Les heures de début et de fin d'après-midi doivent être toutes deux renseignées ou vides.");
        }

        if (finM != null && debutA != null && debutA.isBefore(finM)) {
            throw new IllegalArgumentException(
                    "Le créneau après-midi ne peut pas commencer avant la fin du créneau matin.");
        }

        if (debutM == null && debutA == null) {
            throw new IllegalArgumentException(
                    "Au moins un créneau (matin ou après-midi) doit être renseigné.");
        }
    }
}

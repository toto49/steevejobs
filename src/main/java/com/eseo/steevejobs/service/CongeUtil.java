package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.Planning;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Utilitaires transverses pour le calcul et l'identification des congés.
 * <p>
 * Règles métier : quota annuel de {@value #JOURS_CONGE_ANNUELS} jours ouvrés,
 * normalisation du libellé de type « congé », comptage inclusif des jours
 * sur une année civile (intersection avec le 1er janvier – 31 décembre).
 * </p>
 */
public final class CongeUtil {

    /** Nombre de jours de congé alloués par employé et par année civile. */
    public static final int JOURS_CONGE_ANNUELS = 25;
    /** Libellé technique du type d'événement planning associé à un congé validé. */
    public static final String TYPE_CONGE = "Congé";
    /** Libellé d'affichage pour les interfaces utilisateur. */
    public static final String TYPE_CONGE_AFFICHAGE = "Congés";
    /** Couleur hexadécimale des congés validés dans le planning. */
    public static final String COULEUR_CONGE = "#FFB347";
    /** Couleur hexadécimale des demandes de congé en attente de validation RH. */
    public static final String COULEUR_DEMANDE_EN_ATTENTE = "#FFE0B2";

    private CongeUtil() {
    }

    /**
     * Indique si le libellé de type correspond à un congé (comparaison insensible à la casse et aux accents).
     *
     * @param type libellé de type d'événement planning ou demande
     * @return {@code true} si le type normalisé équivaut à « conge » ; {@code false} si null ou vide
     */
    public static boolean estTypeConge(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        String normalise = type.trim().toLowerCase(Locale.ROOT).replace("é", "e");
        return normalise.equals("conge");
    }

    /**
     * Compte le nombre de jours calendaires d'une période intersectée avec une année civile.
     * <p>
     * Les bornes invalides (fin antérieure au début, dates nulles) renvoient 0.
     * Le décompte est inclusif (jour de début et jour de fin comptés).
     * </p>
     *
     * @param debut date-heure de début de la période
     * @param fin   date-heure de fin de la période
     * @param annee année civile de référence (1er janvier – 31 décembre)
     * @return nombre de jours dans l'intersection ; 0 si période invalide ou hors année
     */
    public static long compterJoursSurPeriode(LocalDateTime debut, LocalDateTime fin, int annee) {
        if (debut == null || fin == null || fin.isBefore(debut)) {
            return 0;
        }

        LocalDate periodeDebut = LocalDate.of(annee, 1, 1);
        LocalDate periodeFin = LocalDate.of(annee, 12, 31);

        LocalDate eventDebut = debut.toLocalDate();
        LocalDate eventFin = fin.toLocalDate();

        LocalDate effectiveDebut = eventDebut.isBefore(periodeDebut) ? periodeDebut : eventDebut;
        LocalDate effectiveFin = eventFin.isAfter(periodeFin) ? periodeFin : eventFin;

        if (effectiveFin.isBefore(effectiveDebut)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(effectiveDebut, effectiveFin) + 1;
    }

    /**
     * Compte les jours de congé d'un événement planning pour une année donnée.
     *
     * @param planning événement planning (congé ou autre)
     * @param annee    année civile de référence
     * @return nombre de jours selon {@link #compterJoursSurPeriode(LocalDateTime, LocalDateTime, int)}
     */
    public static long compterJoursPlanning(Planning planning, int annee) {
        return compterJoursSurPeriode(planning.getJourDebut(), planning.getJourFin(), annee);
    }

    /**
     * Compte les jours couverts par une demande de congé pour une année donnée.
     *
     * @param demande demande de congé
     * @param annee   année civile de référence
     * @return nombre de jours selon {@link #compterJoursSurPeriode(LocalDateTime, LocalDateTime, int)}
     */
    public static long compterJoursDemande(DemandeConge demande, int annee) {
        return compterJoursSurPeriode(demande.getJourDebut(), demande.getJourFin(), annee);
    }
}

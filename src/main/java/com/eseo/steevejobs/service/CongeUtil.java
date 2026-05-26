package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.Planning;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class CongeUtil {

    public static final int JOURS_CONGE_ANNUELS = 25;
    public static final String TYPE_CONGE = "Congé";
    public static final String TYPE_CONGE_AFFICHAGE = "Congés";
    public static final String COULEUR_CONGE = "#FFB347";
    public static final String COULEUR_DEMANDE_EN_ATTENTE = "#FFE0B2";

    private CongeUtil() {
    }

    public static boolean estTypeConge(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        String normalise = type.trim().toLowerCase(Locale.ROOT).replace("é", "e");
        return normalise.equals("conge");
    }

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

    public static long compterJoursPlanning(Planning planning, int annee) {
        return compterJoursSurPeriode(planning.getJourDebut(), planning.getJourFin(), annee);
    }

    public static long compterJoursDemande(DemandeConge demande, int annee) {
        return compterJoursSurPeriode(demande.getJourDebut(), demande.getJourFin(), annee);
    }
}

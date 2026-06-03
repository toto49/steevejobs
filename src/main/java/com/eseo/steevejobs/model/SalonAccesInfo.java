package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.ReunionType;
import com.eseo.steevejobs.model.Enum.VisioStatut;

import java.time.LocalDateTime;

/**
 * Projection des données lues en base pour évaluer l'accès à un salon visio.
 * Objet immuable transitoire (non persisté) ; consommé par {@code VisioService} pour appliquer les règles métier.
 *
 * @param statut           statut courant de la visio
 * @param type             type de réunion
 * @param createurId       identifiant du créateur
 * @param invite           indique si l'utilisateur courant est invité
 * @param heureProgrammee  créneau planifié le cas échéant
 */
public record SalonAccesInfo(
        VisioStatut statut,
        ReunionType type,
        int createurId,
        boolean invite,
        LocalDateTime heureProgrammee
) {
}

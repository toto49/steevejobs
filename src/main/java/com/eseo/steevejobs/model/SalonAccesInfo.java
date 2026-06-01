package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.ReunionType;
import com.eseo.steevejobs.model.Enum.VisioStatut;

import java.time.LocalDateTime;

/** Données brutes lues en BDD pour décider de l'accès (logique dans VisioService). */
public record SalonAccesInfo(
        VisioStatut statut,
        ReunionType type,
        int createurId,
        boolean invite,
        LocalDateTime heureProgrammee
) {
}

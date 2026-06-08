package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

/**
 * Projection minimale d'un salon visio en cours (type et horaire).
 * Utilisée par {@code VisioService} pour la clôture automatique ; non persistée en tant qu'entité.
 *
 * @param typeReunion      libellé ou code du type de réunion
 * @param heureProgrammee  horodatage planifié associé
 */
public record SalonEnCoursInfo(String typeReunion, LocalDateTime heureProgrammee) {
}

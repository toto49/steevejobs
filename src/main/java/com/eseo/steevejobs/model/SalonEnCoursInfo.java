package com.eseo.steevejobs.model;

import java.time.LocalDateTime;

/** Type et heure d'un salon EN_COURS (clôture gérée par VisioService). */
public record SalonEnCoursInfo(String typeReunion, LocalDateTime heureProgrammee) {
}

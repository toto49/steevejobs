package com.eseo.steevejobs.model.Enum;

/**
 * Statut de workflow d'une demande de congé.
 * Persisté en base sur {@code DemandeConge} ; pilote l'affichage et les actions RH dans l'UI calendrier.
 */
public enum StatutDemandeConge {
    /** Demande soumise, en attente de décision RH. */
    EN_ATTENTE,
    /** Demande acceptée ; impacte le solde et le planning. */
    VALIDEE,
    /** Demande refusée. */
    REFUSEE
}

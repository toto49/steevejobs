package com.eseo.steevejobs.model.Enum;

/**
 * Statut du cycle de vie d'un ticket de support.
 * Stocké en base sur {@code Ticket} ; utilisé pour filtrer et colorer les listes côté UI.
 */
public enum StatutTicket {

    /** Ticket pris en charge par le support. */
    EN_COURS,

    /** Ticket ouvert, non encore traité. */
    EN_ATTENTE,

    /** Ticket clos. */
    FERME
}

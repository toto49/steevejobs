package com.eseo.steevejobs.model.Enum;

/**
 * Catégorie métier d'un tiers (client ou fournisseur).
 * Colonne typée en persistance sur {@code Tiers} ; conditionne les formulaires et filtres UI.
 */
public enum TiersType {

    /** Tiers acheteur ou destinataire commercial. */
    CLIENT,

    /** Tiers fournisseur. */
    FOURNISSEUR
}

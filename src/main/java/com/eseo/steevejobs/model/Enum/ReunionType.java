package com.eseo.steevejobs.model.Enum;

/**
 * Mode de réunion visio (immédiate ou planifiée).
 * Colonne sur {@code Visio} ; influence les règles d'ouverture du salon côté service.
 */
public enum ReunionType {
    /** Salon créé et rejoint sans planification préalable. */
    INSTANTANEE("Instantanée"),
    /** Réunion avec horaire programmé. */
    PLANIFIEE("Planifié");

    private String valeur;

    /** Constructeur réservé à la sérialisation enum. */
    ReunionType() {
    }

    /**
     * @param valeur libellé métier du type
     */
    ReunionType(String valeur) {
        this.valeur = valeur;
    }

    /**
     * Résout un type à partir d'un libellé ou du nom de constante.
     *
     * @param valeur libellé ou nom enum (insensible à la casse)
     * @return constante correspondante, ou {@code null} si valeur vide
     * @throws IllegalArgumentException si la valeur ne correspond à aucune constante
     */
    public static ReunionType fromValeur(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }

        for (ReunionType type : values()) {
            if (type.valeur.equalsIgnoreCase(valeur) || type.name().equalsIgnoreCase(valeur)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type inconnu : " + valeur);
    }

    /** @return libellé affichable du type de réunion */
    public String getValeur() {
        return valeur;
    }
}

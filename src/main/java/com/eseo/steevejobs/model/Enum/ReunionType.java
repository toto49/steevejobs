package com.eseo.steevejobs.model.Enum;

public enum ReunionType {
    INSTANTANEE("Instantanée"),
    PLANIFIEE("Planifié");

    private String valeur;

    ReunionType() {
    }

    ReunionType(String valeur) {
        this.valeur = valeur;
    }

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

    public String getValeur() {
        return valeur;
    }
}

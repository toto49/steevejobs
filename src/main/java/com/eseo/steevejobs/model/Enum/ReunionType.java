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
        for (ReunionType type : values()) {
            if (type.valeur.equals(valeur)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type inconnu : " + valeur);
    }

    public String getValeur() {
        return valeur;
    }
}

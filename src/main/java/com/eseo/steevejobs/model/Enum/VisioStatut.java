package com.eseo.steevejobs.model.Enum;

public enum VisioStatut {
    PROGRAMMEE("Programmé"),
    EN_COURS("En cours"),
    TERMINE("Terminé");


    private final String valeur;

    VisioStatut(String valeur) {
        this.valeur = valeur;
    }

    public static VisioStatut fromValeur(String valeur) {
        for (VisioStatut type : values()) {
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

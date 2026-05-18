package com.eseo.steevejobs.model.Enum;

public enum DocumentType {
    DEVIS("devis"),
    FACTURE("facture"),
    BON_COMMANDE("bon commande");

    private final String valeur;

    DocumentType(String valeur) {
        this.valeur = valeur;
    }

    public String getValeur() {
        return valeur;
    }

    public static DocumentType fromValeur(String valeur) {
        for (DocumentType type : values()) {
            if (type.valeur.equals(valeur)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type inconnu : " + valeur);
    }
}
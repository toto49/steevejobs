package com.eseo.steevejobs.model.Enum;

public enum DocumentStatut {
    A_PAYER("à payer"),
    EN_ATTENTE("en attente"),
    PAYE("payé"),
    REFUSE("refusé");

    private final String valeur;

    DocumentStatut(String valeur) {
        this.valeur = valeur;
    }

    public String getValeur() {
        return valeur;
    }

    public static DocumentStatut fromValeur(String valeur) {
        for (DocumentStatut statut : values()) {
            if (statut.valeur.equals(valeur)) {
                return statut;
            }
        }
        throw new IllegalArgumentException("Statut inconnu : " + valeur);
    }
}
package com.eseo.steevejobs.model.Enum;

/**
 * Type de document commercial géré par l'application.
 * Détermine les workflows et gabarits PDF en UI ; stocké sur l'entité {@code Document}.
 */
public enum DocumentType {
    /** Proposition commerciale. */
    DEVIS("devis"),
    /** Facture client. */
    FACTURE("facture"),
    /** Bon de commande fournisseur ou client. */
    BON_COMMANDE("bon commande");

    private final String valeur;

    /**
     * @param valeur libellé métier du type
     */
    DocumentType(String valeur) {
        this.valeur = valeur;
    }

    /** @return libellé affichable du type */
    public String getValeur() {
        return valeur;
    }

    /**
     * Résout un type à partir de son libellé métier.
     *
     * @param valeur libellé exact (ex. « facture »)
     * @return constante correspondante
     * @throws IllegalArgumentException si aucune constante ne correspond
     */
    public static DocumentType fromValeur(String valeur) {
        for (DocumentType type : values()) {
            if (type.valeur.equals(valeur)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type inconnu : " + valeur);
    }
}

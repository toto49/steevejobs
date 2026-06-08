package com.eseo.steevejobs.model.Enum;

/**
 * Statut de cycle de vie d'un document commercial (paiement, validation).
 * Valeur libellée persistée ou affichée en UI ; convertie depuis les chaînes métier.
 */
public enum DocumentStatut {
    /** Document à régler. */
    A_PAYER("à payer"),
    /** Document en attente de traitement ou validation. */
    EN_ATTENTE("en attente"),
    /** Document réglé. */
    PAYE("payé"),
    /** Document refusé ou annulé. */
    REFUSE("refusé");

    private final String valeur;

    /**
     * @param valeur libellé métier associé à la constante
     */
    DocumentStatut(String valeur) {
        this.valeur = valeur;
    }

    /** @return libellé affichable du statut */
    public String getValeur() {
        return valeur;
    }

    /**
     * Résout un statut à partir de son libellé métier.
     *
     * @param valeur libellé exact attendu (ex. « payé »)
     * @return constante correspondante
     * @throws IllegalArgumentException si aucune constante ne correspond
     */
    public static DocumentStatut fromValeur(String valeur) {
        for (DocumentStatut statut : values()) {
            if (statut.valeur.equals(valeur)) {
                return statut;
            }
        }
        throw new IllegalArgumentException("Statut inconnu : " + valeur);
    }
}

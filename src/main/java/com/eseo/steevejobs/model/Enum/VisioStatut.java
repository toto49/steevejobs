package com.eseo.steevejobs.model.Enum;

/**
 * Statut d'une session de visioconférence.
 * Persisté sur {@code Visio} ; pilote l'accès au salon et l'affichage des listes planifiées.
 */
public enum VisioStatut {
    /** Réunion planifiée, pas encore démarrée. */
    PROGRAMMEE("Programmé"),
    /** Salon actif. */
    EN_COURS("En cours"),
    /** Session clôturée. */
    TERMINE("Terminé");

    private final String valeur;

    /**
     * @param valeur libellé affiché en interface
     */
    VisioStatut(String valeur) {
        this.valeur = valeur;
    }

    /**
     * Résout un statut à partir de son libellé.
     *
     * @param valeur libellé exact attendu
     * @return constante correspondante
     * @throws IllegalArgumentException si aucune constante ne correspond
     */
    public static VisioStatut fromValeur(String valeur) {
        for (VisioStatut type : values()) {
            if (type.valeur.equals(valeur)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type inconnu : " + valeur);
    }

    /** @return libellé affichable du statut */
    public String getValeur() {
        return valeur;
    }
}

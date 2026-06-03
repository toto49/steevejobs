package com.eseo.steevejobs.model;

/**
 * Agrégat de solde de congés pour une année civile.
 * Objet de lecture calculé côté service (non entité JPA) ; affiché dans les écrans RH congés.
 */
public class SoldeConge {

    private final int annee;
    private final int joursAcquis;
    private final long joursPris;
    private final long joursEnAttente;
    private final long joursRestants;

    /**
     * Construit un solde et calcule les jours restants disponibles.
     *
     * @param annee           année de référence
     * @param joursAcquis     droits acquis sur l'année
     * @param joursPris       jours déjà consommés (validés)
     * @param joursEnAttente  jours couverts par des demandes en attente
     */
    public SoldeConge(int annee, int joursAcquis, long joursPris, long joursEnAttente) {
        this.annee = annee;
        this.joursAcquis = joursAcquis;
        this.joursPris = joursPris;
        this.joursEnAttente = joursEnAttente;
        this.joursRestants = Math.max(0, joursAcquis - joursPris - joursEnAttente);
    }

    /** @return année civile du solde */
    public int getAnnee() {
        return annee;
    }

    /** @return nombre de jours acquis pour l'année */
    public int getJoursAcquis() {
        return joursAcquis;
    }

    /** @return nombre de jours déjà pris (demandes validées) */
    public long getJoursPris() {
        return joursPris;
    }

    /** @return nombre de jours réservés par des demandes en attente */
    public long getJoursEnAttente() {
        return joursEnAttente;
    }

    /** @return jours encore disponibles après prises et réservations */
    public long getJoursRestants() {
        return joursRestants;
    }

    /**
     * Calcule le ratio d'utilisation du quota (prises + en attente) sur les jours acquis.
     *
     * @return ratio entre 0 et 1, ou 0 si aucun jour acquis
     */
    public double getRatioUtilise() {
        if (joursAcquis <= 0) {
            return 0;
        }
        return Math.min(1.0, (double) (joursPris + joursEnAttente) / joursAcquis);
    }
}

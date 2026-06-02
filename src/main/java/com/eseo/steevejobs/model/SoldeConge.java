package com.eseo.steevejobs.model;

public class SoldeConge {

    private final int annee;
    private final int joursAcquis;
    private final long joursPris;
    private final long joursEnAttente;
    private final long joursRestants;

    public SoldeConge(int annee, int joursAcquis, long joursPris, long joursEnAttente) {
        this.annee = annee;
        this.joursAcquis = joursAcquis;
        this.joursPris = joursPris;
        this.joursEnAttente = joursEnAttente;
        this.joursRestants = Math.max(0, joursAcquis - joursPris - joursEnAttente);
    }

    public int getAnnee() {
        return annee;
    }

    public int getJoursAcquis() {
        return joursAcquis;
    }

    public long getJoursPris() {
        return joursPris;
    }

    public long getJoursEnAttente() {
        return joursEnAttente;
    }

    public long getJoursRestants() {
        return joursRestants;
    }

    public double getRatioUtilise() {
        if (joursAcquis <= 0) {
            return 0;
        }
        return Math.min(1.0, (double) (joursPris + joursEnAttente) / joursAcquis);
    }
}

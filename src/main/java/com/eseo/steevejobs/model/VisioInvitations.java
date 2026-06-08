package com.eseo.steevejobs.model;

/**
 * Association entre une visioconférence et un employé invité.
 * Table de liaison persistée ; alimente les listes d'invités et les contrôles d'accès au salon.
 */
public class VisioInvitations {
    private int id;
    private int visio_id;
    private int employe_id;

    /** Constructeur vide pour persistance ou formulaire. */
    public VisioInvitations() {
    }

    /**
     * Construit une invitation avec identifiant technique.
     *
     * @param id         identifiant de la ligne
     * @param visio_id   identifiant de la visio
     * @param employe_id identifiant de l'employé invité
     */
    public VisioInvitations(int id, int visio_id, int employe_id) {
        this.id = id;
        this.visio_id = visio_id;
        this.employe_id = employe_id;
    }

    /**
     * Construit une invitation sans identifiant (insertion).
     *
     * @param visio_id   identifiant de la visio
     * @param employe_id identifiant de l'employé invité
     */
    public VisioInvitations(int visio_id, int employe_id) {
        this.visio_id = visio_id;
        this.employe_id = employe_id;
    }

    /** @return identifiant de la ligne */
    public int getId() {
        return id;
    }

    /** @param id identifiant de la ligne */
    public void setId(int id) {
        this.id = id;
    }

    /** @return identifiant de la visio */
    public int getVisio_id() {
        return visio_id;
    }

    /** @param visio_id identifiant de la visio */
    public void setVisio_id(int visio_id) {
        this.visio_id = visio_id;
    }

    /** @return identifiant de l'employé invité */
    public int getEmploye_id() {
        return employe_id;
    }

    /** @param employe_id identifiant de l'employé invité */
    public void setEmploye_id(int employe_id) {
        this.employe_id = employe_id;
    }

}

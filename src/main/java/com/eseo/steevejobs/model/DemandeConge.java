package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.StatutDemandeConge;

import java.time.LocalDateTime;

/**
 * Demande de congé d'un employé sur une période donnée.
 * Entité persistée liée à {@code User} et optionnellement à un créneau {@code Planning} ; gérée dans les écrans RH et calendrier.
 */
public class DemandeConge {

    private int id;
    private LocalDateTime jourDebut;
    private LocalDateTime jourFin;
    private StatutDemandeConge statut;
    private String commentaireEmploye;
    private String commentaireRh;
    private LocalDateTime dateDemande;
    private User employe;
    /** Identifiant du créneau planning impacté, le cas échéant. */
    private int idPlanning;

    /** Constructeur vide pour mapping ORM ou formulaire. */
    public DemandeConge() {
    }

    /**
     * Construit une demande avec l'ensemble des attributs métier.
     *
     * @param id                 identifiant technique
     * @param jourDebut          début de la période demandée
     * @param jourFin            fin de la période demandée
     * @param statut             statut de workflow
     * @param commentaireEmploye motif ou précision de l'employé
     * @param commentaireRh      retour RH éventuel
     * @param dateDemande        horodatage de soumission
     * @param employe            demandeur
     * @param idPlanning         créneau planning associé (0 si absent)
     */
    public DemandeConge(int id, LocalDateTime jourDebut, LocalDateTime jourFin, StatutDemandeConge statut,
                        String commentaireEmploye, String commentaireRh, LocalDateTime dateDemande, User employe,
                        int idPlanning) {
        this.id = id;
        this.jourDebut = jourDebut;
        this.jourFin = jourFin;
        this.statut = statut;
        this.commentaireEmploye = commentaireEmploye;
        this.commentaireRh = commentaireRh;
        this.dateDemande = dateDemande;
        this.employe = employe;
        this.idPlanning = idPlanning;
    }

    /** @return identifiant technique */
    public int getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return début de la période de congé */
    public LocalDateTime getJourDebut() {
        return jourDebut;
    }

    /** @param jourDebut début de la période de congé */
    public void setJourDebut(LocalDateTime jourDebut) {
        this.jourDebut = jourDebut;
    }

    /** @return fin de la période de congé */
    public LocalDateTime getJourFin() {
        return jourFin;
    }

    /** @param jourFin fin de la période de congé */
    public void setJourFin(LocalDateTime jourFin) {
        this.jourFin = jourFin;
    }

    /** @return statut de la demande */
    public StatutDemandeConge getStatut() {
        return statut;
    }

    /** @param statut statut de la demande */
    public void setStatut(StatutDemandeConge statut) {
        this.statut = statut;
    }

    /** @return commentaire saisi par l'employé */
    public String getCommentaireEmploye() {
        return commentaireEmploye;
    }

    /** @param commentaireEmploye commentaire saisi par l'employé */
    public void setCommentaireEmploye(String commentaireEmploye) {
        this.commentaireEmploye = commentaireEmploye;
    }

    /** @return commentaire ou décision RH */
    public String getCommentaireRh() {
        return commentaireRh;
    }

    /** @param commentaireRh commentaire ou décision RH */
    public void setCommentaireRh(String commentaireRh) {
        this.commentaireRh = commentaireRh;
    }

    /** @return date de soumission de la demande */
    public LocalDateTime getDateDemande() {
        return dateDemande;
    }

    /** @param dateDemande date de soumission de la demande */
    public void setDateDemande(LocalDateTime dateDemande) {
        this.dateDemande = dateDemande;
    }

    /** @return employé demandeur */
    public User getEmploye() {
        return employe;
    }

    /** @param employe employé demandeur */
    public void setEmploye(User employe) {
        this.employe = employe;
    }

    /** @return identifiant du créneau planning lié */
    public int getIdPlanning() {
        return idPlanning;
    }

    /** @param idPlanning identifiant du créneau planning lié */
    public void setIdPlanning(int idPlanning) {
        this.idPlanning = idPlanning;
    }
}

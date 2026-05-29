package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.StatutDemandeConge;

import java.time.LocalDateTime;

public class DemandeConge {

    private int id;
    private LocalDateTime jourDebut;
    private LocalDateTime jourFin;
    private StatutDemandeConge statut;
    private String commentaireEmploye;
    private String commentaireRh;
    private LocalDateTime dateDemande;
    private User employe;
    private int idPlanning;

    public DemandeConge() {
    }

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getJourDebut() {
        return jourDebut;
    }

    public void setJourDebut(LocalDateTime jourDebut) {
        this.jourDebut = jourDebut;
    }

    public LocalDateTime getJourFin() {
        return jourFin;
    }

    public void setJourFin(LocalDateTime jourFin) {
        this.jourFin = jourFin;
    }

    public StatutDemandeConge getStatut() {
        return statut;
    }

    public void setStatut(StatutDemandeConge statut) {
        this.statut = statut;
    }

    public String getCommentaireEmploye() {
        return commentaireEmploye;
    }

    public void setCommentaireEmploye(String commentaireEmploye) {
        this.commentaireEmploye = commentaireEmploye;
    }

    public String getCommentaireRh() {
        return commentaireRh;
    }

    public void setCommentaireRh(String commentaireRh) {
        this.commentaireRh = commentaireRh;
    }

    public LocalDateTime getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(LocalDateTime dateDemande) {
        this.dateDemande = dateDemande;
    }

    public User getEmploye() {
        return employe;
    }

    public void setEmploye(User employe) {
        this.employe = employe;
    }

    public int getIdPlanning() {
        return idPlanning;
    }

    public void setIdPlanning(int idPlanning) {
        this.idPlanning = idPlanning;
    }
}

package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.ReunionType;
import com.eseo.steevejobs.model.Enum.VisioStatut;

import java.time.LocalDateTime;

/**
 * Session de visioconférence (salon Jitsi ou équivalent).
 * Entité persistée avec créateur, créneaux horaires et statut ; alimente les écrans planification et salle visio.
 */
public class Visio {
    private int id;
    private String room_name;
    private int createur_id;
    private VisioStatut statut;
    private LocalDateTime heure_programmee;
    private LocalDateTime heure_debut;
    private LocalDateTime heure_fin;
    private ReunionType type_reunion;

    /** Constructeur vide pour mapping ou formulaire. */
    public Visio() {
    }

    /**
     * Construit une visio avec l'ensemble des champs persistés.
     *
     * @param id               identifiant technique
     * @param room_name        nom de salle (identifiant salon)
     * @param createur_id      identifiant de l'utilisateur créateur
     * @param statut           statut de session
     * @param type_reunion     mode instantané ou planifié
     * @param heure_debut      horodatage de début effectif
     * @param heure_programmee horodatage planifié
     * @param heure_fin        horodatage de fin
     */
    public Visio(int id, String room_name, int createur_id, VisioStatut statut, ReunionType type_reunion, LocalDateTime heure_debut, LocalDateTime heure_programmee, LocalDateTime heure_fin) {
        this.id = id;
        this.room_name = room_name;
        this.createur_id = createur_id;
        this.statut = statut;
        this.type_reunion = type_reunion;
        this.heure_debut = heure_debut;
        this.heure_programmee = heure_programmee;
        this.heure_fin = heure_fin;
    }

    /**
     * Construit une visio planifiée avec statut et type par défaut.
     *
     * @param room_name        nom de salle
     * @param createur_id      identifiant du créateur
     * @param heure_programmee date/heure prévues
     */
    public Visio(String room_name, int createur_id, LocalDateTime heure_programmee) {
        this.room_name = room_name;
        this.createur_id = createur_id;
        this.heure_programmee = heure_programmee;
        this.statut = VisioStatut.PROGRAMMEE;
        this.type_reunion = ReunionType.PLANIFIEE;
    }

    /** @return nom de la salle de visio */
    public String getRoom_name() {
        return room_name;
    }

    /** @param room_name nom de la salle de visio */
    public void setRoom_name(String room_name) {
        this.room_name = room_name;
    }

    /** @return identifiant technique */
    public int getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return identifiant du créateur */
    public int getCreateur_id() {
        return createur_id;
    }

    /** @param createur_id identifiant du créateur */
    public void setCreateur_id(int createur_id) {
        this.createur_id = createur_id;
    }

    /** @return statut de la session */
    public VisioStatut getStatut() {
        return statut;
    }

    /** @param statut statut de la session */
    public void setStatut(VisioStatut statut) {
        this.statut = statut;
    }

    /** @return horodatage planifié */
    public LocalDateTime getHeure_programmee() {
        return heure_programmee;
    }

    /** @param heure_programmee horodatage planifié */
    public void setHeure_programmee(LocalDateTime heure_programmee) {
        this.heure_programmee = heure_programmee;
    }

    /** @return horodatage de fin */
    public LocalDateTime getHeure_fin() {
        return heure_fin;
    }

    /** @param heure_fin horodatage de fin */
    public void setHeure_fin(LocalDateTime heure_fin) {
        this.heure_fin = heure_fin;
    }

    /** @return horodatage de début effectif */
    public LocalDateTime getHeure_debut() {
        return heure_debut;
    }

    /** @param heure_debut horodatage de début effectif */
    public void setHeure_debut(LocalDateTime heure_debut) {
        this.heure_debut = heure_debut;
    }

    /** @return type de réunion (instantanée ou planifiée) */
    public ReunionType getType_reunion() {
        return type_reunion;
    }

    /** @param type_reunion type de réunion */
    public void setType_reunion(ReunionType type_reunion) {
        this.type_reunion = type_reunion;
    }
}

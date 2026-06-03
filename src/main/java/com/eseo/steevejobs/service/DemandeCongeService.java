package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.DemandeCongeDAO;
import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.Enum.StatutDemandeConge;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.SoldeConge;
import com.eseo.steevejobs.model.User;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Cycle de vie des demandes de congé et calcul du solde annuel.
 * <p>
 * Règles métier : quota {@link CongeUtil#JOURS_CONGE_ANNUELS} jours par an ;
 * vérification du solde à la création et à la validation RH ; une demande validée
 * crée un événement planning de type congé ; modification d'une demande validée
 * synchronise le planning associé. Aucun e-mail ni WebSocket : persistance BDD uniquement.
 * </p>
 */
public class DemandeCongeService {

    private final DemandeCongeDAO demandeCongeDAO;
    private final PlanningDAO planningDAO;
    private final PlanningService planningService;

    /**
     * Constructeur avec injection des dépendances.
     *
     * @param demandeCongeDAO   accès aux demandes
     * @param planningDAO       accès au planning
     * @param planningService   service de gestion du planning
     */
    public DemandeCongeService(DemandeCongeDAO demandeCongeDAO, PlanningDAO planningDAO, PlanningService planningService) {
        this.demandeCongeDAO = demandeCongeDAO;
        this.planningDAO = planningDAO;
        this.planningService = planningService;
    }

    /**
     * Constructeur par défaut instanciant les DAO et {@link PlanningService}.
     */
    public DemandeCongeService() {
        PlanningDAO planningDao = new PlanningDAO();
        this.demandeCongeDAO = new DemandeCongeDAO();
        this.planningDAO = planningDao;
        this.planningService = new PlanningService(planningDao);
    }

    /**
     * Crée une demande en statut {@link StatutDemandeConge#EN_ATTENTE} après contrôle du solde.
     *
     * @param employe     employé demandeur
     * @param debut       date-heure de début (non passée)
     * @param fin         date-heure de fin (postérieure au début)
     * @param commentaire commentaire employé (peut être null, sera trimé)
     * @return demande persistée avec identifiant renseigné
     * @throws IllegalArgumentException si période ou solde invalides
     * @throws SQLException             en cas d'échec de persistance
     * @throws RuntimeException         si l'insertion en base échoue
     */
    public DemandeConge creerDemande(User employe, LocalDateTime debut, LocalDateTime fin, String commentaire)
            throws SQLException {
        validerPeriode(debut, fin);

        int annee = debut.getYear();
        long joursDemandes = CongeUtil.compterJoursSurPeriode(debut, fin, annee);
        SoldeConge solde = calculerSoldeConge(employe.getId(), annee);

        if (joursDemandes > solde.getJoursRestants()) {
            throw new IllegalArgumentException(
                    "Solde insuffisant : il vous reste " + solde.getJoursRestants() + " jour(s) sur " + annee + ".");
        }

        DemandeConge demande = new DemandeConge();
        demande.setEmploye(employe);
        demande.setJourDebut(debut);
        demande.setJourFin(fin);
        demande.setStatut(StatutDemandeConge.EN_ATTENTE);
        demande.setCommentaireEmploye(commentaire != null ? commentaire.trim() : "");
        demande.setCommentaireRh(null);
        demande.setDateDemande(LocalDateTime.now());

        if (!demandeCongeDAO.create(demande)) {
            throw new RuntimeException("Impossible d'enregistrer la demande de congé.");
        }
        return demande;
    }

    /**
     * Valide une demande en attente : crée l'événement planning congé et met à jour le statut.
     *
     * @param demandeId     identifiant de la demande
     * @param commentaireRh commentaire RH optionnel
     * @throws IllegalArgumentException si demande introuvable, déjà traitée ou solde insuffisant
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la mise à jour échoue
     */
    public void validerDemande(int demandeId, String commentaireRh) throws SQLException {
        DemandeConge demande = demandeCongeDAO.findById(demandeId);
        if (demande == null) {
            throw new IllegalArgumentException("Demande introuvable.");
        }
        if (demande.getStatut() != StatutDemandeConge.EN_ATTENTE) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée.");
        }

        int annee = demande.getJourDebut().getYear();
        long joursDemandes = CongeUtil.compterJoursDemande(demande, annee);
        SoldeConge solde = calculerSoldeConge(demande.getEmploye().getId(), annee, demandeId);

        if (joursDemandes > solde.getJoursRestants()) {
            throw new IllegalArgumentException(
                    "Validation impossible : il reste " + solde.getJoursRestants()
                            + " jour(s) disponible(s) sur " + annee + ".");
        }

        String description = demande.getCommentaireEmploye();
        if (description == null || description.isBlank()) {
            description = "Congé validé par la RH";
        }

        Planning conge = new Planning(
                0,
                demande.getJourDebut(),
                demande.getJourFin(),
                CongeUtil.TYPE_CONGE,
                description,
                CongeUtil.COULEUR_CONGE,
                demande.getEmploye()
        );
        planningService.ajouterPlanning(conge);

        demande.setStatut(StatutDemandeConge.VALIDEE);
        demande.setCommentaireRh(commentaireRh != null ? commentaireRh.trim() : null);
        demande.setIdPlanning(conge.getId());
        if (!demandeCongeDAO.update(demande)) {
            throw new RuntimeException("Impossible de mettre à jour la demande.");
        }
    }

    /**
     * Modifie les dates d'une demande ; si validée, met à jour le planning lié.
     *
     * @param demandeId identifiant de la demande
     * @param dateDebut nouvelle date de début (jour civil)
     * @param dateFin   nouvelle date de fin (jour civil)
     * @throws IllegalArgumentException si demande refusée, dates invalides ou solde insuffisant
     * @throws IllegalStateException    si congé validé sans planning associé
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la mise à jour échoue
     */
    public void modifierDemandeConge(int demandeId, LocalDate dateDebut, LocalDate dateFin) throws SQLException {
        DemandeConge demande = demandeCongeDAO.findById(demandeId);
        if (demande == null) {
            throw new IllegalArgumentException("Demande introuvable.");
        }
        if (demande.getStatut() == StatutDemandeConge.REFUSEE) {
            throw new IllegalArgumentException("Impossible de modifier une demande refusée.");
        }
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException("Les dates sont obligatoires.");
        }
        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }

        LocalDateTime nouveauDebut = debutJournee(dateDebut);
        LocalDateTime nouvelleFin = finJournee(dateFin);
        int annee = nouveauDebut.getYear();
        long joursNouveaux = CongeUtil.compterJoursSurPeriode(nouveauDebut, nouvelleFin, annee);
        long joursAnciens = CongeUtil.compterJoursDemande(demande, annee);

        verifierSoldeModification(demande, annee, joursNouveaux, joursAnciens);

        demande.setJourDebut(nouveauDebut);
        demande.setJourFin(nouvelleFin);

        if (demande.getStatut() == StatutDemandeConge.VALIDEE) {
            Planning planning = trouverPlanningAssocie(demande);
            if (planning == null) {
                throw new IllegalStateException("Congé validé introuvable dans le planning.");
            }
            String description = demande.getCommentaireEmploye();
            if (description == null || description.isBlank()) {
                description = "Congés validés par la RH";
            }
            Planning modifie = new Planning(
                    planning.getId(),
                    nouveauDebut,
                    nouvelleFin,
                    CongeUtil.TYPE_CONGE,
                    description,
                    CongeUtil.COULEUR_CONGE,
                    demande.getEmploye()
            );
            planningService.modifierPlanning(modifie);
            demande.setIdPlanning(planning.getId());
        }

        if (!demandeCongeDAO.update(demande)) {
            throw new RuntimeException("Impossible de mettre à jour la demande.");
        }
    }

    /**
     * Supprime une demande validée et l'événement planning associé le cas échéant.
     *
     * @param demandeId identifiant de la demande
     * @throws IllegalArgumentException si demande introuvable ou non validée
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la suppression échoue
     */
    public void supprimerCongeValide(int demandeId) throws SQLException {
        DemandeConge demande = demandeCongeDAO.findById(demandeId);
        if (demande == null) {
            throw new IllegalArgumentException("Demande introuvable.");
        }
        if (demande.getStatut() != StatutDemandeConge.VALIDEE) {
            throw new IllegalArgumentException("Seuls les congés validés peuvent être supprimés.");
        }

        Planning planning = trouverPlanningAssocie(demande);
        if (planning != null) {
            planningService.supprimerPlanning(planning.getId());
        }

        if (!demandeCongeDAO.delete(demandeId)) {
            throw new RuntimeException("Impossible de supprimer la demande.");
        }
    }

    private void verifierSoldeModification(DemandeConge demande, int annee, long joursNouveaux, long joursAnciens)
            throws SQLException {
        SoldeConge solde = calculerSoldeConge(demande.getEmploye().getId(), annee, demande.getId());
        long restantsEffectifs = solde.getJoursRestants();
        if (demande.getStatut() == StatutDemandeConge.VALIDEE) {
            restantsEffectifs += joursAnciens;
        }
        if (joursNouveaux > restantsEffectifs) {
            throw new IllegalArgumentException(
                    "Solde insuffisant : il reste " + restantsEffectifs + " jour(s) disponible(s) sur " + annee + ".");
        }
    }

    private Planning trouverPlanningAssocie(DemandeConge demande) throws SQLException {
        if (demande.getIdPlanning() > 0) {
            Planning planning = planningDAO.getById(demande.getIdPlanning());
            if (planning != null) {
                return planning;
            }
        }
        return planningDAO.findByUserId(demande.getEmploye().getId()).stream()
                .filter(p -> CongeUtil.estTypeConge(p.getType()))
                .filter(p -> datesSeChevauchent(
                        p.getJourDebut(), p.getJourFin(),
                        demande.getJourDebut(), demande.getJourFin()))
                .findFirst()
                .orElse(null);
    }

    private boolean datesSeChevauchent(LocalDateTime debut1, LocalDateTime fin1,
                                       LocalDateTime debut2, LocalDateTime fin2) {
        return debut1.isBefore(fin2) && fin1.isAfter(debut2);
    }

    /**
     * Refuse une demande encore en attente.
     *
     * @param demandeId identifiant de la demande
     * @throws IllegalArgumentException si demande introuvable ou déjà traitée
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la mise à jour échoue
     */
    public void refuserDemande(int demandeId) throws SQLException {
        DemandeConge demande = demandeCongeDAO.findById(demandeId);
        if (demande == null) {
            throw new IllegalArgumentException("Demande introuvable.");
        }
        if (demande.getStatut() != StatutDemandeConge.EN_ATTENTE) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée.");
        }

        demande.setStatut(StatutDemandeConge.REFUSEE);
        demande.setCommentaireRh(null);
        if (!demandeCongeDAO.update(demande)) {
            throw new RuntimeException("Impossible de mettre à jour la demande.");
        }
    }

    /**
     * Liste toutes les demandes en attente de validation RH.
     *
     * @return liste des demandes {@link StatutDemandeConge#EN_ATTENTE}
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<DemandeConge> listerEnAttente() throws SQLException {
        return demandeCongeDAO.findByStatut(StatutDemandeConge.EN_ATTENTE);
    }

    /**
     * Liste l'ensemble des demandes, tous statuts confondus.
     *
     * @return liste complète
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<DemandeConge> listerToutes() throws SQLException {
        return demandeCongeDAO.findAll();
    }

    /**
     * Liste les demandes en attente pour un employé donné.
     *
     * @param userId identifiant de l'employé
     * @return sous-ensemble en attente
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<DemandeConge> listerEnAttenteParEmploye(int userId) throws SQLException {
        return demandeCongeDAO.findByUserId(userId).stream()
                .filter(d -> d.getStatut() == StatutDemandeConge.EN_ATTENTE)
                .toList();
    }

    /**
     * Calcule le solde de congés pour un employé et une année (toutes demandes prises en compte).
     *
     * @param userId identifiant de l'employé
     * @param annee  année civile
     * @return solde (jours alloués, pris, en attente, restants)
     * @throws SQLException en cas d'erreur d'accès base
     */
    public SoldeConge calculerSoldeConge(int userId, int annee) throws SQLException {
        return calculerSoldeConge(userId, annee, -1);
    }

    /**
     * Calcule le solde en excluant éventuellement une demande du décompte « en attente ».
     *
     * @param userId          identifiant de l'employé
     * @param annee           année civile
     * @param demandeExclueId identifiant de demande à exclure du calcul en attente ; {@code <= 0} pour aucune exclusion
     * @return solde de congés
     * @throws SQLException en cas d'erreur d'accès base
     */
    public SoldeConge calculerSoldeConge(int userId, int annee, int demandeExclueId) throws SQLException {
        long joursPris = planningDAO.findByUserId(userId).stream()
                .filter(p -> CongeUtil.estTypeConge(p.getType()))
                .mapToLong(p -> CongeUtil.compterJoursPlanning(p, annee))
                .sum();

        long joursEnAttente = demandeCongeDAO.findByUserId(userId).stream()
                .filter(d -> d.getStatut() == StatutDemandeConge.EN_ATTENTE)
                .filter(d -> demandeExclueId <= 0 || d.getId() != demandeExclueId)
                .mapToLong(d -> CongeUtil.compterJoursDemande(d, annee))
                .sum();

        return new SoldeConge(annee, CongeUtil.JOURS_CONGE_ANNUELS, joursPris, joursEnAttente);
    }

    private void validerPeriode(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }
        if (debut.toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La date de début ne peut pas être dans le passé.");
        }
    }

    /**
     * Convertit une date civile en début de journée ouvrée (08:00).
     *
     * @param date date civile
     * @return date-heure de début standard
     */
    public static LocalDateTime debutJournee(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.of(8, 0));
    }

    /**
     * Convertit une date civile en fin de journée ouvrée (18:00).
     *
     * @param date date civile
     * @return date-heure de fin standard
     */
    public static LocalDateTime finJournee(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.of(18, 0));
    }
}

package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.FichePayeDAO;
import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Génération et consultation des fiches de paie mensuelles.
 * <p>
 * Règles métier : une seule fiche par employé et par mois ; mois non futur ;
 * salaire brut, heures et taux strictement positifs ; taux patronal dans [0, 1[.
 * Les jours de congé du mois sont déduits du planning ({@link CongeUtil}).
 * Effet de bord : création d'un fichier PDF local via {@link PdfGeneratorService},
 * puis mise à jour de l'URL en base.
 * </p>
 */
public class FichePayeService {

    /** Accès persistance aux fiches de paie. */
    private final FichePayeDAO        fichePayeDAO;
    /** Accès persistance au planning (déduction des jours de congé). */
    private final PlanningDAO         planningDAO;
    /** Générateur PDF local pour les bulletins de paie. */
    private final PdfGeneratorService pdfService;

    /** Seuil minimal exclusif du salaire brut (doit être strictement positif). */
    private static final double SALAIRE_MINIMUM_LEGAL = 0;
    /** Plafond maximal du salaire brut autorisé en saisie. */
    private static final double SALAIRE_MAXIMUM       = 100_000.0;

    /**
     * Constructeur par défaut.
     */
    public FichePayeService() {
        this.fichePayeDAO = new FichePayeDAO();
        this.planningDAO  = new PlanningDAO();
        this.pdfService   = new PdfGeneratorService();
    }

    /**
     * Constructeur avec injection (tests).
     *
     * @param fichePayeDAO DAO fiches de paie
     * @param planningDAO  DAO planning (congés)
     * @param pdfService   générateur PDF
     */
    public FichePayeService(FichePayeDAO fichePayeDAO, PlanningDAO planningDAO,
                            PdfGeneratorService pdfService) {
        this.fichePayeDAO = fichePayeDAO;
        this.planningDAO  = planningDAO;
        this.pdfService   = pdfService;
    }

    /**
     * Crée une fiche de paie, génère le bulletin PDF et enregistre l'URL.
     *
     * @param employe                    employé concerné
     * @param mois                       mois de paie (non futur)
     * @param salaireBrut                salaire brut mensuel
     * @param tauxCotisationsPatronales  taux entre 0 et 1 (exclus)
     * @param heuresTravaillees          heures du mois
     * @param tauxHoraire                taux horaire appliqué
     * @return fiche persistée avec URL du PDF
     * @throws IllegalArgumentException si paramètres invalides
     * @throws IllegalStateException    si une fiche existe déjà pour ce mois
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public FichePaye genererFichePaye(User employe, LocalDateTime mois,
                                      double salaireBrut, double tauxCotisationsPatronales,
                                      double heuresTravaillees, double tauxHoraire)
            throws IllegalArgumentException, SQLException {

        validerParametresGeneration(employe, mois, salaireBrut, tauxCotisationsPatronales,
                heuresTravaillees, tauxHoraire);

        FichePaye existante = fichePayeDAO.findByEmployeIdAndDate(employe.getId(), mois);
        if (existante != null) {
            throw new IllegalStateException(
                    "Une fiche de paie existe déjà pour "
                            + employe.getPrenom() + " " + employe.getNom()
                            + " sur ce mois.");
        }

        long joursConge = compterJoursConge(employe.getId(), mois);

        FichePaye fiche = new FichePaye(0, mois, "", employe);
        fichePayeDAO.createFichePaye(fiche);

        String url = pdfService.genererFichePaye(fiche, salaireBrut, tauxCotisationsPatronales,
                joursConge, heuresTravaillees, tauxHoraire);

        fichePayeDAO.updateUrl(fiche.getId(), url);
        fiche.setUrl(url);

        return fiche;
    }

    /**
     * Liste toutes les fiches de paie.
     *
     * @return liste complète
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<FichePaye> findAll() throws SQLException {
        return fichePayeDAO.findAll();
    }

    /**
     * Liste les fiches d'un employé.
     *
     * @param id identifiant employé
     * @return fiches de l'employé
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<FichePaye> findByEmployeId(int id) throws SQLException {
        return fichePayeDAO.findByEmployeId(id);
    }

    /**
     * Liste les fiches d'une année civile.
     *
     * @param annee année (ex. 2026)
     * @return fiches de l'année
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<FichePaye> findByAnnee(int annee) throws SQLException {
        return fichePayeDAO.findByAnnee(annee);
    }

    /**
     * Supprime une fiche de paie.
     *
     * @param id identifiant de la fiche
     * @return {@code true} si la suppression a réussi
     * @throws SQLException en cas d'erreur d'accès base
     */
    public boolean supprimer(int id) throws SQLException {
        return fichePayeDAO.deleteFichePaye(id);
    }

    /**
     * Valide les paramètres requis à la génération d'une fiche de paie.
     *
     * @param employe                   employé concerné (identifiant valide)
     * @param mois                      mois de paie (non futur)
     * @param salaireBrut               salaire brut strictement positif et dans les bornes autorisées
     * @param tauxCotisationsPatronales taux patronal compris dans {@code [0, 1[}
     * @param heuresTravaillees         heures travaillées strictement positives
     * @param tauxHoraire               taux horaire strictement positif
     * @throws IllegalArgumentException si un paramètre est absent ou hors plage
     */
    private void validerParametresGeneration(User employe, LocalDateTime mois,
                                             double salaireBrut,
                                             double tauxCotisationsPatronales,
                                             double heuresTravaillees,
                                             double tauxHoraire)
            throws IllegalArgumentException {

        if (employe == null) {
            throw new IllegalArgumentException("L'employé est obligatoire.");
        }
        if (employe.getId() <= 0) {
            throw new IllegalArgumentException("L'ID de l'employé est invalide.");
        }
        if (mois == null) {
            throw new IllegalArgumentException("Le mois de la fiche est obligatoire.");
        }
        if (mois.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Impossible de générer une fiche de paie pour un mois futur.");
        }
        if (salaireBrut <= 0) {
            throw new IllegalArgumentException("Le salaire brut doit être supérieur à 0.");
        }
        if (salaireBrut < SALAIRE_MINIMUM_LEGAL) {
            throw new IllegalArgumentException(
                    String.format("Le salaire brut ne peut pas être inférieur a 0.",
                            SALAIRE_MINIMUM_LEGAL));
        }
        if (salaireBrut > SALAIRE_MAXIMUM) {
            throw new IllegalArgumentException(
                    String.format("Le salaire brut ne peut pas dépasser %.0f €.", SALAIRE_MAXIMUM));
        }
        if (tauxCotisationsPatronales < 0 || tauxCotisationsPatronales >= 1) {
            throw new IllegalArgumentException("Le taux de cotisations patronales doit être entre 0 et 1.");
        }
        if (heuresTravaillees <= 0) {
            throw new IllegalArgumentException("Les heures travaillées doivent être supérieures à 0.");
        }
        if (tauxHoraire <= 0) {
            throw new IllegalArgumentException("Le taux horaire doit être supérieur à 0.");
        }
    }

    /**
     * Compte les jours de congé d'un employé intersectant le mois de paie cible.
     * <p>
     * Seuls les événements planning de type congé sont pris en compte ; les périodes
     * chevauchant le mois sont tronquées aux bornes du mois civil.
     * </p>
     *
     * @param employeId identifiant de l'employé
     * @param mois      mois de référence (année et numéro de mois utilisés)
     * @return total de jours de congé sur le mois
     * @throws SQLException en cas d'erreur d'accès base
     */
    private long compterJoursConge(int employeId, LocalDateTime mois) throws SQLException {
        List<Planning> plannings = planningDAO.findByUserId(employeId);
        int annee      = mois.getYear();
        int moisValeur = mois.getMonthValue();

        return plannings.stream()
                .filter(p -> CongeUtil.estTypeConge(p.getType()))
                .filter(p -> {
                    LocalDateTime debutMois = LocalDateTime.of(annee, moisValeur, 1, 0, 0);
                    LocalDateTime finMois   = debutMois.plusMonths(1);
                    return p.getJourDebut().isBefore(finMois) && p.getJourFin().isAfter(debutMois);
                })
                .mapToLong(p -> {
                    LocalDateTime debutMois = LocalDateTime.of(annee, moisValeur, 1, 0, 0);
                    LocalDateTime finMois   = debutMois.plusMonths(1);
                    LocalDateTime debut = p.getJourDebut().isBefore(debutMois) ? debutMois : p.getJourDebut();
                    LocalDateTime fin   = p.getJourFin().isAfter(finMois)      ? finMois   : p.getJourFin();
                    return java.time.Duration.between(debut, fin).toDays();
                })
                .sum();
    }
}

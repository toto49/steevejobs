package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.FichePayeDAO;
import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class FichePayeService {

    private final FichePayeDAO        fichePayeDAO;
    private final PlanningDAO         planningDAO;
    private final PdfGeneratorService pdfService;

    private static final double SALAIRE_MINIMUM_LEGAL = 1766.92;
    private static final double SALAIRE_MAXIMUM       = 100_000.0;

    public FichePayeService() {
        this.fichePayeDAO = new FichePayeDAO();
        this.planningDAO  = new PlanningDAO();
        this.pdfService   = new PdfGeneratorService();
    }

    public FichePayeService(FichePayeDAO fichePayeDAO, PlanningDAO planningDAO,
                            PdfGeneratorService pdfService) {
        this.fichePayeDAO = fichePayeDAO;
        this.planningDAO  = planningDAO;
        this.pdfService   = pdfService;
    }

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

    public boolean supprimerFiche(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID de la fiche de paie est invalide.");
        }
        return fichePayeDAO.deleteFichePaye(id);
    }

    public List<FichePaye> obtenirToutesLesFiches() throws SQLException {
        return fichePayeDAO.findAll();
    }

    public List<FichePaye> obtenirFichesParEmploye(int employeId)
            throws IllegalArgumentException, SQLException {
        if (employeId <= 0) {
            throw new IllegalArgumentException("L'ID de l'employé est invalide.");
        }
        return fichePayeDAO.findByEmployeId(employeId);
    }

    public List<FichePaye> obtenirFichesParAnnee(int annee)
            throws IllegalArgumentException, SQLException {
        int anneeActuelle = LocalDateTime.now().getYear();
        if (annee < 2000 || annee > anneeActuelle) {
            throw new IllegalArgumentException(
                    "L'année doit être comprise entre 2000 et " + anneeActuelle + ".");
        }
        return fichePayeDAO.findByAnnee(annee);
    }

    public List<FichePaye> findAll() throws SQLException {
        return fichePayeDAO.findAll();
    }

    public List<FichePaye> findByEmployeId(int id) throws SQLException {
        return fichePayeDAO.findByEmployeId(id);
    }

    public List<FichePaye> findByAnnee(int annee) throws SQLException {
        return fichePayeDAO.findByAnnee(annee);
    }

    public boolean supprimer(int id) throws SQLException {
        return fichePayeDAO.deleteFichePaye(id);
    }

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
                    String.format("Le salaire brut ne peut pas être inférieur au SMIC (%.2f €).",
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

    private long compterJoursConge(int employeId, LocalDateTime mois) throws SQLException {
        List<Planning> plannings = planningDAO.findByUserId(employeId);
        int annee      = mois.getYear();
        int moisValeur = mois.getMonthValue();

        return plannings.stream()
                .filter(p -> "Conge".equalsIgnoreCase(p.getType()))
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

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

    private final FichePayeDAO       fichePayeDAO;
    private final PlanningDAO        planningDAO;
    private final PdfGeneratorService pdfService;

    public FichePayeService() {
        this.fichePayeDAO = new FichePayeDAO();
        this.planningDAO  = new PlanningDAO();
        this.pdfService   = new PdfGeneratorService();
    }

    /**
     * Génère une fiche de paie à partir des heures travaillées et du taux horaire.
     *
     * @param employe l'employé concerné
     * @param mois la date du mois concerné
     * @param salaireBrut le salaire brut calculé (heures × taux horaire)
     * @param tauxCotisationsPatronales taux des cotisations patronales (ex: 0.45 = 45%)
     * @param heuresTravaillees nombre d'heures travaillées dans le mois
     * @param tauxHoraire taux horaire de l'employé (€/h)
     * @return la fiche de paie créée
     * @throws SQLException erreur base de données
     */
    public FichePaye genererFichePaye(User employe, LocalDateTime mois,
                                      double salaireBrut, double tauxCotisationsPatronales,
                                      double heuresTravaillees, double tauxHoraire)
            throws SQLException {

        // Vérifier doublon
        FichePaye existante = fichePayeDAO.findByEmployeIdAndDate(employe.getId(), mois);
        if (existante != null) {
            throw new IllegalStateException(
                    "Une fiche existe déjà pour " + employe.getPrenom() +
                            " " + employe.getNom() + " sur ce mois.");
        }

        // Valider les montants
        validerMontants(salaireBrut, tauxCotisationsPatronales, heuresTravaillees, tauxHoraire);

        // Détecter les congés depuis le planning
        long joursConge = compterJoursConge(employe.getId(), mois);

        // Créer en BDD
        FichePaye fiche = new FichePaye(0, mois, "", employe);
        fichePayeDAO.createFichePaye(fiche);

        // Générer le PDF avec les nouvelles informations
        String url = pdfService.genererFichePaye(fiche, salaireBrut, tauxCotisationsPatronales,
                joursConge, heuresTravaillees, tauxHoraire);

        // Mettre à jour l'URL
        fichePayeDAO.updateUrl(fiche.getId(), url);
        fiche.setUrl(url);

        return fiche;
    }

    // -------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------

    /**
     * Compte les jours de type "Conge" dans le planning de l'employé
     */
    private long compterJoursConge(int employeId, LocalDateTime mois) throws SQLException {
        List<Planning> plannings = planningDAO.findByUserId(employeId);

        int annee      = mois.getYear();
        int moisValeur = mois.getMonthValue();

        return plannings.stream()
                .filter(p -> "Conge".equalsIgnoreCase(p.getType()))
                .filter(p -> {
                    LocalDateTime debutMois = LocalDateTime.of(annee, moisValeur, 1, 0, 0);
                    LocalDateTime finMois   = debutMois.plusMonths(1);
                    return p.getJourDebut().isBefore(finMois) &&
                            p.getJourFin().isAfter(debutMois);
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

    private void validerMontants(double salaireBrut, double tauxCotisationsPatronales,
                                 double heuresTravaillees, double tauxHoraire) {
        if (salaireBrut <= 0) {
            throw new IllegalArgumentException("Le salaire brut doit être supérieur à 0.");
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

    // ==========================================
    // Méthodes publiques
    // ==========================================

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
}

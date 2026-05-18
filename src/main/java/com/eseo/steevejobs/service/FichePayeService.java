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
     * Génère une fiche de paie en détectant automatiquement
     * les congés depuis le planning du mois concerné.
     */
    public FichePaye genererFichePaye(User employe, LocalDateTime date,
                                      double salaireBase, double tauxCotisations)
            throws SQLException {

        // Vérifier doublon
        FichePaye existante = fichePayeDAO.findByEmployeIdAndDate(employe.getId(), date);
        if (existante != null) {
            throw new IllegalStateException(
                    "Une fiche existe déjà pour " + employe.getPrenom() +
                            " " + employe.getNom() + " sur ce mois.");
        }

        // Valider montants
        validerMontants(salaireBase, tauxCotisations);

        // Détecter les congés depuis le planning A REVOIR EN FONCTION DU SYSTEME DE PLANNING
        long joursConge = compterJoursConge(employe.getId(), date);

        // Créer en BDD
        FichePaye fiche = new FichePaye(0, date, "", employe);
        fichePayeDAO.createFichePaye(fiche);

        //Générer le PDF (avec les congés)
        String url = pdfService.genererFichePaye(fiche, salaireBase, tauxCotisations, joursConge);

        // Mettre à jour l'URL
        fichePayeDAO.updateUrl(fiche.getId(), url);
        fiche.setUrl(url);

        return fiche;
    }

    public List<FichePaye> findAll()                        throws SQLException { return fichePayeDAO.findAll(); }
    public List<FichePaye> findByEmployeId(int id)          throws SQLException { return fichePayeDAO.findByEmployeId(id); }
    public List<FichePaye> findByAnnee(int annee)           throws SQLException { return fichePayeDAO.findByAnnee(annee); }
    public boolean         supprimer(int id)                throws SQLException { return fichePayeDAO.deleteFichePaye(id); }

    // -------------------------------------------------------

    /**
     * Compte les jours de type "Conge" dans le planning de l'employé
     * pour le mois donné. Chaque entrée PLANNING de type "Conge"
     * est comptée en jours entiers (jour_fin - jour_debut).
     */
    private long compterJoursConge(int employeId, LocalDateTime date) throws SQLException {
        List<Planning> plannings = planningDAO.findByUserId(employeId);

        int annee = date.getYear();
        int moisValeur = date.getMonthValue();

        return plannings.stream()
                .filter(p -> "Conge".equalsIgnoreCase(p.getType()))
                .filter(p -> {
                    // Garder uniquement les entrées qui chevauchent le mois concerné
                    LocalDateTime debutMois = LocalDateTime.of(annee, moisValeur, 1, 0, 0);
                    LocalDateTime finMois   = debutMois.plusMonths(1);
                    return p.getJourDebut().isBefore(finMois) &&
                            p.getJourFin().isAfter(debutMois);
                })
                .mapToLong(p -> {
                    // Borner au mois concerné pour ne pas déborder sur un autre mois
                    LocalDateTime debutMois = LocalDateTime.of(annee, moisValeur, 1, 0, 0);
                    LocalDateTime finMois   = debutMois.plusMonths(1);
                    LocalDateTime debut = p.getJourDebut().isBefore(debutMois) ? debutMois : p.getJourDebut();
                    LocalDateTime fin   = p.getJourFin().isAfter(finMois)      ? finMois   : p.getJourFin();
                    return java.time.Duration.between(debut, fin).toDays();
                })
                .sum();
    }

    private void validerMontants(double salaireBase, double tauxCotisations) {
        if (salaireBase <= 0)
            throw new IllegalArgumentException("Le salaire brut doit être supérieur à 0.");
        if (tauxCotisations < 0 || tauxCotisations >= 1)
            throw new IllegalArgumentException("Le taux de cotisations doit être entre 0 et 1.");
    }
}

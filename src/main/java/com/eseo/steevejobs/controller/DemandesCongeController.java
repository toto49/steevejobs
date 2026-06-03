package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.Enum.StatutDemandeConge;
import com.eseo.steevejobs.model.SoldeConge;
import com.eseo.steevejobs.service.CongeUtil;
import com.eseo.steevejobs.service.DemandeCongeService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur FXML de la popup de gestion des demandes de congés (vue RH).
 * Liaisons FXML : {@code tableDemandes}, filtres, panneau de détail et boutons d'action.
 */
public class DemandesCongeController {

    /** Format d'affichage des dates de congé dans le tableau et le détail. */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Format d'affichage des dates de soumission de demande. */
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Service de gestion des demandes de congés. */
    private final DemandeCongeService demandeCongeService = new DemandeCongeService();
    /** Ensemble des demandes chargées depuis la base. */
    private List<DemandeConge> toutesLesDemandes;
    /** Demande actuellement sélectionnée dans le tableau. */
    private DemandeConge demandeSelectionnee;
    /** Callback exécuté après traitement d'une demande (rafraîchissement parent). */
    private Runnable onDemandeTraitee;

    /** Filtre par statut des demandes affichées. */
    @FXML
    private ComboBox<String> comboFiltreStatut;
    /** Tableau listant les demandes de congés. */
    @FXML
    private TableView<DemandeConge> tableDemandes;
    /** Colonne affichant le nom de l'employé demandeur. */
    @FXML
    private TableColumn<DemandeConge, String> colEmploye;
    /** Colonne affichant la date de début de l'absence. */
    @FXML
    private TableColumn<DemandeConge, String> colDebut;
    /** Colonne affichant la date de fin de l'absence. */
    @FXML
    private TableColumn<DemandeConge, String> colFin;
    /** Colonne affichant le nombre de jours demandés. */
    @FXML
    private TableColumn<DemandeConge, String> colJours;
    /** Colonne affichant la date de soumission de la demande. */
    @FXML
    private TableColumn<DemandeConge, String> colDateDemande;
    /** Colonne affichant le statut de la demande. */
    @FXML
    private TableColumn<DemandeConge, String> colStatut;
    /** Libellé du nom de l'employé sélectionné dans le détail. */
    @FXML
    private Label lblEmployeSelectionne;
    /** Sélecteur de date de début en édition. */
    @FXML
    private DatePicker dateDebutEdit;
    /** Sélecteur de date de fin en édition. */
    @FXML
    private DatePicker dateFinEdit;
    /** Zone de texte du commentaire employé. */
    @FXML
    private TextArea txtCommentaireEmploye;
    /** Libellé de la période de solde affichée. */
    @FXML
    private Label lblSoldePeriode;
    /** Barre de progression du solde de congés utilisé. */
    @FXML
    private ProgressBar progressSolde;
    /** Détail textuel du solde de congés. */
    @FXML
    private Label lblSoldeDetail;
    /** Bouton de modification de la demande sélectionnée. */
    @FXML
    private Button btnModifier;
    /** Bouton de suppression de la demande sélectionnée. */
    @FXML
    private Button btnSupprimer;
    /** Bouton de validation de la demande par la RH. */
    @FXML
    private Button btnValider;
    /** Bouton de refus de la demande par la RH. */
    @FXML
    private Button btnRefuser;

    /**
     * Initialise filtres, colonnes du tableau et charge les demandes.
     *
     * @throws SQLException non propagée ; affichée via alerte en cas d'échec de chargement
     */
    @FXML
    public void initialize() {
        comboFiltreStatut.setItems(FXCollections.observableArrayList(
                "En attente", "Toutes", "Validées", "Refusées"));
        comboFiltreStatut.getSelectionModel().select("En attente");

        configurerColonnes();
        tableDemandes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableDemandes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            demandeSelectionnee = selected;
            afficherDetail(selected);
        });

        dateDebutEdit.valueProperty().addListener((obs, oldVal, newVal) -> actualiserSoldeApresChangementDates());
        dateFinEdit.valueProperty().addListener((obs, oldVal, newVal) -> actualiserSoldeApresChangementDates());

        chargerDemandes();
    }

    /**
     * Enregistre un callback appelé après validation, refus, modification ou suppression.
     *
     * @param onDemandeTraitee action à exécuter (ex. rafraîchir le calendrier parent)
     */
    public void setOnDemandeTraitee(Runnable onDemandeTraitee) {
        this.onDemandeTraitee = onDemandeTraitee;
    }

    /**
     * Exécute le callback enregistré après traitement d'une demande.
     */
    private void notifierDemandeTraitee() {
        if (onDemandeTraitee != null) {
            onDemandeTraitee.run();
        }
    }

    /**
     * Configure les fabriques de valeurs des colonnes du tableau des demandes.
     */
    private void configurerColonnes() {
        colEmploye.setCellValueFactory(data -> new SimpleStringProperty(formaterNom(data.getValue())));
        colDebut.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getJourDebut().format(DATE_FMT)));
        colFin.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getJourFin().format(DATE_FMT)));
        colJours.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(CongeUtil.compterJoursDemande(data.getValue(), data.getValue().getJourDebut().getYear()))));
        colDateDemande.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDateDemande().format(DATE_TIME_FMT)));
        colStatut.setCellValueFactory(data -> new SimpleStringProperty(formatStatut(data.getValue().getStatut())));
    }

    /**
     * Applique le filtre de statut sélectionné sur le tableau.
     * Liaison FXML : {@code comboFiltreStatut}.
     */
    @FXML
    public void filtrerDemandes() {
        if (toutesLesDemandes == null) {
            return;
        }
        String filtre = comboFiltreStatut.getValue();
        List<DemandeConge> filtrees = toutesLesDemandes.stream()
                .filter(d -> correspondFiltre(d, filtre))
                .toList();
        tableDemandes.setItems(FXCollections.observableArrayList(filtrees));
        if (!filtrees.isEmpty()) {
            tableDemandes.getSelectionModel().selectFirst();
        } else {
            afficherDetail(null);
        }
    }

    /**
     * Valide la demande sélectionnée (statut en attente) et met à jour le planning.
     * Liaison FXML : {@code btnValider}.
     */
    @FXML
    public void validerDemande() {
        if (!verifierSelectionEnAttente()) {
            return;
        }
        try {
            LocalDate debut = dateDebutEdit.getValue();
            LocalDate fin = dateFinEdit.getValue();
            if (debut != null && fin != null) {
                demandeCongeService.modifierDemandeConge(demandeSelectionnee.getId(), debut, fin);
            }
            demandeCongeService.validerDemande(demandeSelectionnee.getId(), null);
            afficherInfo("Demande validée", "Les congés ont été ajoutés au planning de l'employé.");
            notifierDemandeTraitee();
            recharger();
        } catch (IllegalArgumentException ex) {
            afficherErreur(ex.getMessage());
        } catch (SQLException ex) {
            afficherErreur("Impossible de valider la demande : " + ex.getMessage());
        }
    }

    /**
     * Refuse la demande sélectionnée (statut en attente).
     * Liaison FXML : {@code btnRefuser}.
     */
    @FXML
    public void refuserDemande() {
        if (!verifierSelectionEnAttente()) {
            return;
        }
        try {
            demandeCongeService.refuserDemande(demandeSelectionnee.getId());
            afficherInfo("Demande refusée", "La demande a été marquée comme refusée.");
            notifierDemandeTraitee();
            recharger();
        } catch (IllegalArgumentException ex) {
            afficherErreur(ex.getMessage());
        } catch (SQLException ex) {
            afficherErreur("Impossible de refuser la demande : " + ex.getMessage());
        }
    }

    /**
     * Enregistre les dates modifiées d'une demande déjà validée.
     * Liaison FXML : {@code btnModifier}.
     */
    @FXML
    public void enregistrerModifications() {
        if (demandeSelectionnee == null) {
            afficherErreur("Sélectionnez une demande.");
            return;
        }
        if (demandeSelectionnee.getStatut() != StatutDemandeConge.VALIDEE) {
            afficherErreur("Seuls les congés validés peuvent être modifiés ici.");
            return;
        }
        LocalDate debut = dateDebutEdit.getValue();
        LocalDate fin = dateFinEdit.getValue();
        if (debut == null || fin == null) {
            afficherErreur("Les dates sont obligatoires.");
            return;
        }
        try {
            demandeCongeService.modifierDemandeConge(demandeSelectionnee.getId(), debut, fin);
            afficherInfo("Congés modifiés", "Les congés et le planning ont été mis à jour.");
            notifierDemandeTraitee();
            recharger();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            afficherErreur(ex.getMessage());
        } catch (SQLException ex) {
            afficherErreur("Impossible de modifier les congés : " + ex.getMessage());
        }
    }

    /**
     * Supprime des congés validés et les retire du planning après confirmation.
     * Liaison FXML : {@code btnSupprimer}.
     */
    @FXML
    public void supprimerConge() {
        if (demandeSelectionnee == null) {
            afficherErreur("Sélectionnez une demande.");
            return;
        }
        if (demandeSelectionnee.getStatut() != StatutDemandeConge.VALIDEE) {
            afficherErreur("Seuls les congés validés peuvent être supprimés.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer les congés");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Supprimer ces congés validés et les retirer du planning ?");
        styliserAlert(confirmation);
        Button btnAnnuler = (Button) confirmation.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (btnAnnuler != null) {
            btnAnnuler.setText("Annuler");
        }

        confirmation.showAndWait().ifPresent(reponse -> {
            if (reponse != ButtonType.OK) {
                return;
            }
            try {
                demandeCongeService.supprimerCongeValide(demandeSelectionnee.getId());
                afficherInfo("Congés supprimés", "Les congés ont été retirés du planning.");
                notifierDemandeTraitee();
                recharger();
            } catch (IllegalArgumentException ex) {
                afficherErreur(ex.getMessage());
            } catch (SQLException ex) {
                afficherErreur("Impossible de supprimer les congés : " + ex.getMessage());
            }
        });
    }

    /**
     * Charge toutes les demandes de congés de façon asynchrone.
     */
    private void chargerDemandes() {
        Thread chargement = new Thread(() -> {
            try {
                List<DemandeConge> demandes = demandeCongeService.listerToutes();
                Platform.runLater(() -> {
                    toutesLesDemandes = demandes;
                    filtrerDemandes();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> afficherErreur("Impossible de charger les demandes : " + e.getMessage()));
            }
        }, "chargement-demandes-conge");
        chargement.setDaemon(true);
        chargement.start();
    }

    /**
     * Relance le chargement des demandes après une action utilisateur.
     */
    private void recharger() {
        chargerDemandes();
    }

    /**
     * Affiche le détail et le solde de la demande sélectionnée.
     *
     * @param demande demande à afficher, ou {@code null} pour vider le panneau
     */
    private void afficherDetail(DemandeConge demande) {
        if (demande == null) {
            lblEmployeSelectionne.setText("Sélectionnez une demande");
            dateDebutEdit.setValue(null);
            dateFinEdit.setValue(null);
            dateDebutEdit.setDisable(true);
            dateFinEdit.setDisable(true);
            txtCommentaireEmploye.clear();
            txtCommentaireEmploye.setPromptText("—");
            lblSoldePeriode.setText("-");
            progressSolde.setProgress(0);
            lblSoldeDetail.setText("-");
            mettreAJourBoutons(null);
            return;
        }

        int annee = demande.getJourDebut().getYear();

        lblEmployeSelectionne.setText(formaterNom(demande));
        dateDebutEdit.setValue(demande.getJourDebut().toLocalDate());
        dateFinEdit.setValue(demande.getJourFin().toLocalDate());
        txtCommentaireEmploye.setText(demande.getCommentaireEmploye() != null && !demande.getCommentaireEmploye().isBlank()
                ? demande.getCommentaireEmploye()
                : "Aucun commentaire");

        boolean enAttente = demande.getStatut() == StatutDemandeConge.EN_ATTENTE;
        boolean validee = demande.getStatut() == StatutDemandeConge.VALIDEE;
        dateDebutEdit.setDisable(!enAttente && !validee);
        dateFinEdit.setDisable(!enAttente && !validee);
        mettreAJourBoutons(demande);

        actualiserSoldeDetail(demande, annee, enAttente);
    }

    /**
     * Affiche ou masque les boutons d'action selon le statut de la demande.
     *
     * @param demande demande courante, ou {@code null}
     */
    private void mettreAJourBoutons(DemandeConge demande) {
        boolean enAttente = demande != null && demande.getStatut() == StatutDemandeConge.EN_ATTENTE;
        boolean validee = demande != null && demande.getStatut() == StatutDemandeConge.VALIDEE;

        configurerBouton(btnModifier, validee, validee);
        configurerBouton(btnSupprimer, validee, validee);
        configurerBouton(btnValider, enAttente, enAttente);
        configurerBouton(btnRefuser, enAttente, enAttente);
    }

    /**
     * Configure la visibilité et l'état actif d'un bouton d'action.
     *
     * @param bouton bouton cible
     * @param visible {@code true} pour afficher le bouton
     * @param actif {@code true} pour activer le bouton
     */
    private void configurerBouton(Button bouton, boolean visible, boolean actif) {
        bouton.setVisible(visible);
        bouton.setManaged(visible);
        bouton.setDisable(!actif);
    }

    /**
     * Recalcule l'affichage du solde lorsque les dates d'édition changent.
     */
    private void actualiserSoldeApresChangementDates() {
        if (demandeSelectionnee == null || dateDebutEdit.getValue() == null) {
            return;
        }
        boolean enAttente = demandeSelectionnee.getStatut() == StatutDemandeConge.EN_ATTENTE;
        actualiserSoldeDetail(demandeSelectionnee, dateDebutEdit.getValue().getYear(), enAttente);
    }

    /**
     * Calcule et affiche le solde de congés pour la demande et la période données.
     *
     * @param demande demande concernée
     * @param annee année de référence pour le solde
     * @param enAttente {@code true} si la demande est en attente
     */
    private void actualiserSoldeDetail(DemandeConge demande, int annee, boolean enAttente) {
        LocalDate debut = dateDebutEdit.getValue() != null ? dateDebutEdit.getValue() : demande.getJourDebut().toLocalDate();
        LocalDate fin = dateFinEdit.getValue() != null ? dateFinEdit.getValue() : demande.getJourFin().toLocalDate();
        long joursDemande = CongeUtil.compterJoursSurPeriode(
                debut.atStartOfDay().withHour(8),
                fin.atStartOfDay().withHour(18),
                annee);

        Thread calculSolde = new Thread(() -> {
            try {
                SoldeConge solde = demandeCongeService.calculerSoldeConge(
                        demande.getEmploye().getId(), annee, demande.getId());
                Platform.runLater(() -> afficherSolde(solde, joursDemande, enAttente, demande.getStatut()));
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    lblSoldePeriode.setText("Solde indisponible");
                    lblSoldeDetail.setText(e.getMessage());
                });
            }
        }, "calcul-solde-conge");
        calculSolde.setDaemon(true);
        calculSolde.start();
    }

    /**
     * Met à jour les indicateurs visuels du solde de congés.
     *
     * @param solde solde calculé
     * @param joursDemande nombre de jours de la demande courante
     * @param enAttente {@code true} si la demande est en attente
     * @param statut statut de la demande
     */
    private void afficherSolde(SoldeConge solde, long joursDemande, boolean enAttente, StatutDemandeConge statut) {
        lblSoldePeriode.setText("Période " + solde.getAnnee());
        progressSolde.setProgress(solde.getRatioUtilise());
        lblSoldeDetail.setText(
                solde.getJoursAcquis() + " jours acquis · "
                        + solde.getJoursPris() + " pris · "
                        + solde.getJoursEnAttente() + " en attente · "
                        + solde.getJoursRestants() + " restants");

        if (enAttente && joursDemande > solde.getJoursRestants()) {
            progressSolde.setStyle("-fx-accent: #e57373;");
            lblSoldeDetail.setText(lblSoldeDetail.getText()
                    + "\nAttention : cette demande dépasse le solde restant.");
        } else {
            progressSolde.setStyle("-fx-accent: #7A9FE0;");
        }
    }

    /**
     * Vérifie qu'une demande en attente est sélectionnée.
     *
     * @return {@code true} si la sélection est valide pour validation ou refus
     */
    private boolean verifierSelectionEnAttente() {
        if (demandeSelectionnee == null) {
            afficherErreur("Sélectionnez une demande.");
            return false;
        }
        if (demandeSelectionnee.getStatut() != StatutDemandeConge.EN_ATTENTE) {
            afficherErreur("Seules les demandes en attente peuvent être traitées.");
            return false;
        }
        return true;
    }

    /**
     * Indique si une demande correspond au filtre de statut choisi.
     *
     * @param demande demande à tester
     * @param filtre libellé du filtre combo
     * @return {@code true} si la demande doit apparaître dans le tableau filtré
     */
    private boolean correspondFiltre(DemandeConge demande, String filtre) {
        if (filtre == null || filtre.equals("Toutes")) {
            return true;
        }
        return switch (filtre) {
            case "En attente" -> demande.getStatut() == StatutDemandeConge.EN_ATTENTE;
            case "Validées" -> demande.getStatut() == StatutDemandeConge.VALIDEE;
            case "Refusées" -> demande.getStatut() == StatutDemandeConge.REFUSEE;
            default -> true;
        };
    }

    /**
     * Formate le nom de l'employé associé à une demande.
     *
     * @param demande demande source
     * @return nom complet ou chaîne vide
     */
    private String formaterNom(DemandeConge demande) {
        if (demande == null || demande.getEmploye() == null) {
            return "";
        }
        String prenom = demande.getEmploye().getPrenom() != null ? demande.getEmploye().getPrenom().trim() : "";
        String nom = demande.getEmploye().getNom() != null ? demande.getEmploye().getNom().trim() : "";
        return (prenom + " " + nom).trim();
    }

    /**
     * Retourne le libellé français d'un statut de demande.
     *
     * @param statut statut enum
     * @return libellé affichable
     */
    private String formatStatut(StatutDemandeConge statut) {
        return switch (statut) {
            case EN_ATTENTE -> "En attente";
            case VALIDEE -> "Validée";
            case REFUSEE -> "Refusée";
        };
    }

    /**
     * Affiche une alerte d'erreur stylisée.
     *
     * @param message texte du message
     */
    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        styliserAlert(alert);
        alert.showAndWait();
    }

    /**
     * Affiche une alerte d'information stylisée.
     *
     * @param titre titre de la fenêtre
     * @param message texte du message
     */
    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styliserAlert(alert);
        alert.showAndWait();
    }

    /**
     * Applique la feuille de style popup à une alerte.
     *
     * @param alert alerte à styliser
     */
    private void styliserAlert(Alert alert) {
        DialogPane dp = alert.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style/popup.css").toExternalForm());
        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);
        if (btnOk != null) {
            btnOk.getStyleClass().add("button-ok");
        }
    }
}

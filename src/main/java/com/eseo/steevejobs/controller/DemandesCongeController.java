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

public class DemandesCongeController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DemandeCongeService demandeCongeService = new DemandeCongeService();
    private List<DemandeConge> toutesLesDemandes;
    private DemandeConge demandeSelectionnee;
    private Runnable onDemandeTraitee;

    @FXML
    private ComboBox<String> comboFiltreStatut;
    @FXML
    private TableView<DemandeConge> tableDemandes;
    @FXML
    private TableColumn<DemandeConge, String> colEmploye;
    @FXML
    private TableColumn<DemandeConge, String> colDebut;
    @FXML
    private TableColumn<DemandeConge, String> colFin;
    @FXML
    private TableColumn<DemandeConge, String> colJours;
    @FXML
    private TableColumn<DemandeConge, String> colDateDemande;
    @FXML
    private TableColumn<DemandeConge, String> colStatut;
    @FXML
    private Label lblEmployeSelectionne;
    @FXML
    private DatePicker dateDebutEdit;
    @FXML
    private DatePicker dateFinEdit;
    @FXML
    private TextArea txtCommentaireEmploye;
    @FXML
    private Label lblSoldePeriode;
    @FXML
    private ProgressBar progressSolde;
    @FXML
    private Label lblSoldeDetail;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnValider;
    @FXML
    private Button btnRefuser;

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

    public void setOnDemandeTraitee(Runnable onDemandeTraitee) {
        this.onDemandeTraitee = onDemandeTraitee;
    }

    private void notifierDemandeTraitee() {
        if (onDemandeTraitee != null) {
            onDemandeTraitee.run();
        }
    }

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

    private void recharger() {
        chargerDemandes();
    }

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

    private void mettreAJourBoutons(DemandeConge demande) {
        boolean enAttente = demande != null && demande.getStatut() == StatutDemandeConge.EN_ATTENTE;
        boolean validee = demande != null && demande.getStatut() == StatutDemandeConge.VALIDEE;

        configurerBouton(btnModifier, validee, validee);
        configurerBouton(btnSupprimer, validee, validee);
        configurerBouton(btnValider, enAttente, enAttente);
        configurerBouton(btnRefuser, enAttente, enAttente);
    }

    private void configurerBouton(Button bouton, boolean visible, boolean actif) {
        bouton.setVisible(visible);
        bouton.setManaged(visible);
        bouton.setDisable(!actif);
    }

    private void actualiserSoldeApresChangementDates() {
        if (demandeSelectionnee == null || dateDebutEdit.getValue() == null) {
            return;
        }
        boolean enAttente = demandeSelectionnee.getStatut() == StatutDemandeConge.EN_ATTENTE;
        actualiserSoldeDetail(demandeSelectionnee, dateDebutEdit.getValue().getYear(), enAttente);
    }

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

    private String formaterNom(DemandeConge demande) {
        if (demande == null || demande.getEmploye() == null) {
            return "";
        }
        String prenom = demande.getEmploye().getPrenom() != null ? demande.getEmploye().getPrenom().trim() : "";
        String nom = demande.getEmploye().getNom() != null ? demande.getEmploye().getNom().trim() : "";
        return (prenom + " " + nom).trim();
    }

    private String formatStatut(StatutDemandeConge statut) {
        return switch (statut) {
            case EN_ATTENTE -> "En attente";
            case VALIDEE -> "Validée";
            case REFUSEE -> "Refusée";
        };
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        styliserAlert(alert);
        alert.showAndWait();
    }

    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styliserAlert(alert);
        alert.showAndWait();
    }

    private void styliserAlert(Alert alert) {
        DialogPane dp = alert.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style/popup.css").toExternalForm());
        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);
        if (btnOk != null) {
            btnOk.getStyleClass().add("button-ok");
        }
    }
}

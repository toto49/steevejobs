package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.FichePayeService;
import com.eseo.steevejobs.service.WebDavService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class FichePayeController implements Initializable {

    @FXML private ComboBox<User> comboEmployeFiltre;
    @FXML private ComboBox<Integer> comboAnneeFiltre;
    @FXML private TableView<FichePaye> tableFiches;
    @FXML private TableColumn<FichePaye, String> colEmploye;
    @FXML private TableColumn<FichePaye, String> colMois;
    @FXML private TableColumn<FichePaye, String> colPoste;
    @FXML private TableColumn<FichePaye, Void> colActions;
    @FXML private Label lblNbFiches;

    private final FichePayeService fichePayeService = new FichePayeService();
    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<FichePaye> toutesLesFiches = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT_MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        chargerFiltreEmployes();
        chargerFiltreAnnees();
        chargerToutesFiches();
    }

    // ==========================================
    // CONFIGURATION
    // ==========================================

    private void configurerColonnes() {
        colEmploye.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmploye().getPrenom() + " " + data.getValue().getEmploye().getNom()));

        colMois.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDate().format(FMT_MOIS)));

        colPoste.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmploye().getPoste()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnOuvrir = new Button("📄");
            private final Button btnSuppr = new Button("🗑");
            private final HBox box = new HBox(8, btnOuvrir, btnSuppr);

            {
                btnOuvrir.setStyle("-fx-background-color: #4B78CC; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12; -fx-padding: 5 10; -fx-background-radius: 5;");
                btnSuppr.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12; -fx-padding: 5 10; -fx-background-radius: 5;");
                btnOuvrir.setOnAction(e -> ouvrirPdf(getTableView().getItems().get(getIndex())));
                btnSuppr.setOnAction(e -> confirmerSuppression(getTableView().getItems().get(getIndex())));
                box.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void chargerFiltreEmployes() {
        try {
            ObservableList<User> employes = FXCollections.observableArrayList();
            employes.add(null);
            employes.addAll(userDAO.findAll());
            comboEmployeFiltre.setItems(employes);
            comboEmployeFiltre.setConverter(new StringConverter<>() {
                @Override public String toString(User u) {
                    return u == null ? "Tous les employés" : u.getPrenom() + " " + u.getNom();
                }
                @Override public User fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les employés : " + e.getMessage());
        }
    }

    private void chargerFiltreAnnees() {
        int anneeActuelle = LocalDateTime.now().getYear();
        ObservableList<Integer> annees = FXCollections.observableArrayList();
        annees.add(null);
        for (int a = anneeActuelle; a >= anneeActuelle - 5; a--) {
            annees.add(a);
        }
        comboAnneeFiltre.setItems(annees);
        comboAnneeFiltre.setConverter(new StringConverter<>() {
            @Override public String toString(Integer a) {
                return a == null ? "Toutes les années" : String.valueOf(a);
            }
            @Override public Integer fromString(String s) { return null; }
        });
    }

    // ==========================================
    // ACTIONS
    // ==========================================

    @FXML
    private void ouvrirFormulaireGeneration() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Générer une fiche de paie");
        dialog.setHeaderText("Générer une fiche de paie");
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");

        ButtonType btnGenerer = new ButtonType("Générer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGenerer, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");

        ComboBox<User> comboEmploye = new ComboBox<>();
        ComboBox<String> comboMois = new ComboBox<>();
        ComboBox<Integer> comboAnnee = new ComboBox<>();
        TextField txtSalaireBase = new TextField();
        TextField txtTauxCotis = new TextField();

        txtSalaireBase.setPromptText("Ex : 2500.00");
        txtTauxCotis.setPromptText("Ex : 0.22");

        String fieldStyle = "-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-padding: 6;";
        txtSalaireBase.setStyle(fieldStyle);
        txtTauxCotis.setStyle(fieldStyle);
        comboEmploye.setStyle(fieldStyle);
        comboMois.setStyle(fieldStyle);
        comboAnnee.setStyle(fieldStyle);

        int anneeActuelle = LocalDate.now().getYear();
        for (int a = anneeActuelle - 2; a <= anneeActuelle + 1; a++) {
            comboAnnee.getItems().add(a);
        }
        comboAnnee.setValue(anneeActuelle);

        String[] mois = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        comboMois.getItems().addAll(mois);
        comboMois.setValue(mois[LocalDate.now().getMonthValue() - 1]);

        try {
            comboEmploye.setItems(FXCollections.observableArrayList(userDAO.findActiveUsers()));
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les employés : " + e.getMessage());
            return;
        }

        comboEmploye.setConverter(new StringConverter<User>() {
            @Override public String toString(User u) {
                return u == null ? "" : u.getPrenom() + " " + u.getNom() + " — " + u.getPoste();
            }
            @Override public User fromString(String s) { return null; }
        });

        String labelStyle = "-fx-text-fill: #333333; -fx-font-weight: bold;";

        Label lblEmploye = new Label("Employé :"); lblEmploye.setStyle(labelStyle);
        Label lblMois = new Label("Mois :"); lblMois.setStyle(labelStyle);
        Label lblAnnee = new Label("Année :"); lblAnnee.setStyle(labelStyle);
        Label lblSalaire = new Label("Salaire brut (€) :"); lblSalaire.setStyle(labelStyle);
        Label lblTaux = new Label("Taux cotisations :"); lblTaux.setStyle(labelStyle);
        Label lblExemple = new Label("(ex : 0.22 = 22%)"); lblExemple.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        grid.add(lblEmploye, 0, 0);
        grid.add(comboEmploye, 1, 0);
        grid.add(lblMois, 0, 1);
        grid.add(comboMois, 1, 1);
        grid.add(lblAnnee, 0, 2);
        grid.add(comboAnnee, 1, 2);
        grid.add(lblSalaire, 0, 3);
        grid.add(txtSalaireBase, 1, 3);
        grid.add(lblTaux, 0, 4);
        grid.add(txtTauxCotis, 1, 4);
        grid.add(lblExemple, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(460);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(btnGenerer);
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);

        if (btnOk != null) {
            btnOk.setStyle("-fx-background-color: #4B78CC; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16;");
        }
        if (btnCancel != null) {
            btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333333; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16;");
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != btnGenerer) return;

        if (comboEmploye.getValue() == null || comboMois.getValue() == null || comboAnnee.getValue() == null
                || txtSalaireBase.getText().isBlank() || txtTauxCotis.getText().isBlank()) {
            afficherErreur("Tous les champs sont obligatoires.");
            return;
        }

        User employe = comboEmploye.getValue();
        double salaireBase, tauxCotisations;
        try {
            salaireBase = Double.parseDouble(txtSalaireBase.getText().replace(",", "."));
            tauxCotisations = Double.parseDouble(txtTauxCotis.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            afficherErreur("Le salaire et le taux doivent être des nombres valides.");
            return;
        }

        int moisValue = comboMois.getSelectionModel().getSelectedIndex() + 1;
        int anneeValue = comboAnnee.getValue();
        LocalDateTime date = LocalDateTime.of(anneeValue, moisValue, 1, 0, 0);

        try {
            FichePaye fiche = fichePayeService.genererFichePaye(employe, date, salaireBase, tauxCotisations);

            String nomFichier = String.format("fiche_%d_%d_%02d.pdf", employe.getId(), date.getYear(), date.getMonthValue());
            String dossierEmploye = "employe_" + employe.getId();
            if (employe.getPrenom() != null && employe.getNom() != null) {
                dossierEmploye = (employe.getPrenom() + "_" + employe.getNom()).toLowerCase().replaceAll("[^a-z0-9_]", "");
            }
            final String finalDossier = dossierEmploye;

            CompletableFuture.runAsync(() -> {
                try {
                    WebDavService.envoyerFichierLocalSurNAS(finalDossier, nomFichier, fiche.getUrl());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAcceptAsync(v -> {
                Platform.runLater(() -> {
                    afficherSucces("Fiche générée et sauvegardée sur le NAS avec succès !");
                    chargerToutesFiches();
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    afficherErreur("Fiche générée localement, mais échec de l'envoi sur le NAS : " + ex.getCause().getMessage());
                    chargerToutesFiches();
                });
                return null;
            });

        } catch (IllegalStateException | IllegalArgumentException e) {
            afficherErreur(e.getMessage());
        } catch (Exception e) {
            afficherErreur("Erreur lors de la génération : " + e.getMessage());
        }
    }

    @FXML
    private void filtrerParEmploye() {
        User employe = comboEmployeFiltre.getValue();
        Integer annee = comboAnneeFiltre.getValue();
        try {
            List<FichePaye> fiches;
            if (employe != null && annee != null) {
                fiches = fichePayeService.findByEmployeId(employe.getId()).stream()
                        .filter(f -> f.getDate().getYear() == annee)
                        .toList();
            } else if (employe != null) {
                fiches = fichePayeService.findByEmployeId(employe.getId());
            } else if (annee != null) {
                fiches = fichePayeService.findByAnnee(annee);
            } else {
                fiches = fichePayeService.findAll();
            }
            tableFiches.setItems(FXCollections.observableArrayList(fiches));
            lblNbFiches.setText(fiches.size() + " fiche(s)");
        } catch (SQLException e) {
            afficherErreur("Erreur filtre : " + e.getMessage());
        }
    }

    // ==========================================
    // GESTION PDF
    // ==========================================

    private void ouvrirPdf(FichePaye fiche) {
        try {
            File f = new File(fiche.getUrl());
            if (f.exists()) {
                Desktop.getDesktop().open(f);
            } else {
                afficherErreur("Fichier introuvable :\n" + fiche.getUrl());
            }
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir le PDF : " + e.getMessage());
        }
    }

    private void confirmerSuppression(FichePaye fiche) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la fiche");
        confirm.setHeaderText("Supprimer la fiche de " +
                fiche.getEmploye().getPrenom() + " " + fiche.getEmploye().getNom() +
                " — " + fiche.getDate().format(FMT_MOIS) + " ?");
        confirm.setContentText("Le fichier PDF sera également supprimé. Action irréversible.");
        confirm.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    fichePayeService.supprimer(fiche.getId());
                    new File(fiche.getUrl()).delete();
                    String nomFichier = String.format("fiche_%d_%d_%02d.pdf", fiche.getEmploye().getId(), fiche.getDate().getYear(), fiche.getDate().getMonthValue());
                    String dossierEmploye = "employe_" + fiche.getEmploye().getId();
                    if (fiche.getEmploye().getPrenom() != null && fiche.getEmploye().getNom() != null) {
                        dossierEmploye = (fiche.getEmploye().getPrenom() + "_" + fiche.getEmploye().getNom()).toLowerCase().replaceAll("[^a-z0-9_]", "");
                    }
                    final String finalDossier = dossierEmploye;
                    CompletableFuture.runAsync(() -> {
                        WebDavService.supprimerFichierDuNAS(finalDossier, nomFichier);
                    });

                    chargerToutesFiches();
                } catch (SQLException e) {
                    afficherErreur("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    // ==========================================
    // CHARGEMENT
    // ==========================================

    private void chargerToutesFiches() {
        try {
            List<FichePaye> fiches = fichePayeService.findAll();
            toutesLesFiches.setAll(fiches);
            tableFiches.setItems(toutesLesFiches);
            lblNbFiches.setText(fiches.size() + " fiche(s)");
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les fiches : " + e.getMessage());
        }
    }

    // ==========================================
    // MESSAGES
    // ==========================================

    private void afficherErreur(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");
        alert.showAndWait();
    }

    private void afficherSucces(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");
        alert.showAndWait();
    }
}
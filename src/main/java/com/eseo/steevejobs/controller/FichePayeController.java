package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.FichePayeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class FichePayeController implements Initializable {

    @FXML private ComboBox<User>                 comboEmployeFiltre;
    @FXML private ComboBox<Integer>              comboAnneeFiltre;
    @FXML private TableView<FichePaye>           tableFiches;
    @FXML private TableColumn<FichePaye, String> colEmploye;
    @FXML private TableColumn<FichePaye, String> colMois;
    @FXML private TableColumn<FichePaye, String> colPoste;
    @FXML private TableColumn<FichePaye, Void>   colActions;
    @FXML private Label                          lblNbFiches;

    private final FichePayeService fichePayeService = new FichePayeService();
    private final UserDAO          userDAO          = new UserDAO();

    private static final DateTimeFormatter FMT_MOIS =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        chargerFiltreEmployes();
        chargerFiltreAnnees();
        chargerToutesFiches();
    }

    // -------------------------------------------------------
    // Actions FXML

    @FXML
    private void ouvrirFormulaireGeneration() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Générer une fiche de paie");
        dialog.setHeaderText("Renseigner les informations de paie");

        ButtonType btnGenerer = new ButtonType("Générer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGenerer, ButtonType.CANCEL);

        ComboBox<User> comboEmploye   = new ComboBox<>();
        DatePicker     datePicker     = new DatePicker();
        TextField      txtSalaireBase = new TextField();
        TextField      txtTauxCotis  = new TextField();

        txtSalaireBase.setPromptText("Ex : 2500.00");
        txtTauxCotis.setPromptText("Ex : 0.22  (= 22%)");

        try {
            comboEmploye.setItems(FXCollections.observableArrayList(userDAO.findActiveUsers()));
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les employés : " + e.getMessage());
            return;
        }

        comboEmploye.setConverter(new StringConverter<>() {
            @Override public String toString(User u) {
                return u == null ? "" : u.getPrenom() + " " + u.getNom() + " — " + u.getPoste();
            }
            @Override public User fromString(String s) { return null; }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Employé :"),           0, 0); grid.add(comboEmploye,   1, 0);
        grid.add(new Label("Mois concerné :"),     0, 1); grid.add(datePicker,     1, 1);
        grid.add(new Label("Salaire brut (€) :"),  0, 2); grid.add(txtSalaireBase, 1, 2);
        grid.add(new Label("Taux cotisations :"),  0, 3); grid.add(txtTauxCotis,  1, 3);
        grid.add(new Label("(ex : 0.22 = 22%)"),   1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(460);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != btnGenerer) return;

        User employe = comboEmploye.getValue();
        if (employe == null || datePicker.getValue() == null
                || txtSalaireBase.getText().isBlank() || txtTauxCotis.getText().isBlank()) {
            afficherErreur("Tous les champs sont obligatoires.");
            return;
        }

        double salaireBase, tauxCotisations;
        try {
            salaireBase     = Double.parseDouble(txtSalaireBase.getText().replace(",", "."));
            tauxCotisations = Double.parseDouble(txtTauxCotis.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            afficherErreur("Le salaire et le taux doivent être des nombres valides.");
            return;
        }

        LocalDateTime mois = datePicker.getValue().withDayOfMonth(1).atStartOfDay();

        try {
            FichePaye fiche = fichePayeService.genererFichePaye(employe, mois, salaireBase, tauxCotisations);
            afficherSucces("Fiche générée !\nFichier : " + fiche.getUrl());
            chargerToutesFiches();
        } catch (IllegalStateException | IllegalArgumentException e) {
            afficherErreur(e.getMessage());
        } catch (Exception e) {
            afficherErreur("Erreur lors de la génération : " + e.getMessage());
        }
    }

    @FXML
    private void filtrerParEmploye() {
        User    employe = comboEmployeFiltre.getValue();
        Integer annee   = comboAnneeFiltre.getValue();
        try {
            List<FichePaye> fiches;
            if (employe != null)      fiches = fichePayeService.findByEmployeId(employe.getId());
            else if (annee != null)   fiches = fichePayeService.findByAnnee(annee);
            else                      fiches = fichePayeService.findAll();
            tableFiches.setItems(FXCollections.observableArrayList(fiches));
            lblNbFiches.setText(fiches.size() + " fiche(s)");
        } catch (SQLException e) {
            afficherErreur("Erreur filtre : " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Configuration interne

    private void configurerColonnes() {
        colEmploye.setCellValueFactory(data -> {
            User u = data.getValue().getEmploye();
            return new SimpleStringProperty(u.getPrenom() + " " + u.getNom());
        });
        colMois.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMois().format(FMT_MOIS)));
        colPoste.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmploye().getPoste()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnOuvrir    = new Button("📄");
            private final Button btnSupprimer = new Button("🗑");
            private final HBox   box          = new HBox(8, btnOuvrir, btnSupprimer);
            {
                String s = "-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14;";
                btnOuvrir.setStyle(s);
                btnSupprimer.setStyle(s);
                btnOuvrir.setOnAction(e -> ouvrirPdf(getTableView().getItems().get(getIndex()).getUrl()));
                btnSupprimer.setOnAction(e -> confirmerSuppression(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void chargerToutesFiches() {
        try {
            List<FichePaye> fiches = fichePayeService.findAll();
            tableFiches.setItems(FXCollections.observableArrayList(fiches));
            lblNbFiches.setText(fiches.size() + " fiche(s)");
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les fiches : " + e.getMessage());
        }
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
        for (int a = anneeActuelle; a >= anneeActuelle - 5; a--) annees.add(a);
        comboAnneeFiltre.setItems(annees);
        comboAnneeFiltre.setConverter(new StringConverter<>() {
            @Override public String toString(Integer a) {
                return a == null ? "Toutes les années" : String.valueOf(a);
            }
            @Override public Integer fromString(String s) { return null; }
        });
    }

    private void ouvrirPdf(String chemin) {
        try {
            File f = new File(chemin);
            if (f.exists()) Desktop.getDesktop().open(f);
            else afficherErreur("Fichier introuvable :\n" + chemin);
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir le PDF : " + e.getMessage());
        }
    }

    private void confirmerSuppression(FichePaye fiche) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la fiche");
        confirm.setHeaderText("Supprimer la fiche de " +
                fiche.getEmploye().getPrenom() + " " + fiche.getEmploye().getNom() +
                " — " + fiche.getMois().format(FMT_MOIS) + " ?");
        confirm.setContentText("Le fichier PDF sera également supprimé. Action irréversible.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    fichePayeService.supprimer(fiche.getId());
                    new File(fiche.getUrl()).delete();
                    chargerToutesFiches();
                } catch (SQLException e) {
                    afficherErreur("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    private void afficherErreur(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void afficherSucces(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}

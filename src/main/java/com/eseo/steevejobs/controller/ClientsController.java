package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.TiersType;
import com.eseo.steevejobs.service.TiersService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ClientsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> comboTypeFiltre;
    @FXML private TableView<Tiers> tableClients;
    @FXML private TableColumn<Tiers, String> colType, colNom, colPrenom, colEmail, colTel, colSiret;
    @FXML private Label lblNbClients;
    @FXML private Label detailType, detailNom, detailPrenom, detailEmail, detailTel, detailAdresse, detailSiret, detailNumTva;
    @FXML private Button btnModifier, btnSupprimer;

    private final TiersService tiersService = new TiersService();
    private ObservableList<Tiers> tousLesClients = FXCollections.observableArrayList();
    private Tiers clientSelectionne = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        chargerTousClients();
        configurerSelectionTableau();
        configurerFiltreType();

        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);
    }

    private void configurerColonnes() {
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().name()));
        colNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom()));
        colPrenom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrenom() != null ? data.getValue().getPrenom() : ""));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colTel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTel() != null ? data.getValue().getTel() : ""));
        colSiret.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSiret() != null ? data.getValue().getSiret() : ""));
    }

    private void configurerFiltreType() {
        comboTypeFiltre.setItems(FXCollections.observableArrayList("Tous", "Client", "Fournisseur"));
        comboTypeFiltre.setValue("Tous");
    }

    private void configurerSelectionTableau() {
        tableClients.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                clientSelectionne = nouveau;
                afficherDetail(nouveau);
                btnModifier.setDisable(false);
                btnSupprimer.setDisable(false);
            }
        });
    }

    @FXML
    private void ouvrirFormulaireCreation() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouveau");
        dialog.setHeaderText("Créer un nouveau client");
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");

        ButtonType btnCreer = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCreer, ButtonType.CANCEL);

        // Création du GridPane
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");

        // Configuration des colonnes
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setPrefWidth(120);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setFillWidth(true);

        grid.getColumnConstraints().addAll(col1, col2);

        // CHAMPS du formulaire
        ComboBox<TiersType> comboType = new ComboBox<>();
        TextField txtNom = new TextField();
        TextField txtPrenom = new TextField();
        TextField txtEmail = new TextField();
        TextField txtTel = new TextField();
        TextField txtAdresse = new TextField();
        TextField txtSiret = new TextField();
        TextField txtNumTva = new TextField();

        // Configuration du ComboBox Type
        comboType.setItems(FXCollections.observableArrayList(TiersType.values()));
        comboType.setPromptText("Choisir un type");
        comboType.setValue(null);

        // Configuration des champs texte
        txtNom.setPromptText("Nom");
        txtPrenom.setPromptText("Prénom");
        txtEmail.setPromptText("Email");
        txtTel.setPromptText("Téléphone");
        txtAdresse.setPromptText("Adresse");
        txtSiret.setPromptText("SIRET (14 chiffres)");
        txtNumTva.setPromptText("N° TVA");

        // Faire prendre toute la largeur aux champs
        comboType.setMaxWidth(Double.MAX_VALUE);
        txtNom.setMaxWidth(Double.MAX_VALUE);
        txtPrenom.setMaxWidth(Double.MAX_VALUE);
        txtEmail.setMaxWidth(Double.MAX_VALUE);
        txtTel.setMaxWidth(Double.MAX_VALUE);
        txtAdresse.setMaxWidth(Double.MAX_VALUE);
        txtSiret.setMaxWidth(Double.MAX_VALUE);
        txtNumTva.setMaxWidth(Double.MAX_VALUE);

        // Forcer la même hauteur pour tous les champs
        double prefHeight = 32;
        comboType.setPrefHeight(prefHeight);
        txtNom.setPrefHeight(prefHeight);
        txtPrenom.setPrefHeight(prefHeight);
        txtEmail.setPrefHeight(prefHeight);
        txtTel.setPrefHeight(prefHeight);
        txtAdresse.setPrefHeight(prefHeight);
        txtSiret.setPrefHeight(prefHeight);
        txtNumTva.setPrefHeight(prefHeight);

        // Style des champs
        String fieldStyle = "-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-padding: 6; -fx-text-fill: black;";
        comboType.setStyle(fieldStyle);
        txtNom.setStyle(fieldStyle);
        txtPrenom.setStyle(fieldStyle);
        txtEmail.setStyle(fieldStyle);
        txtTel.setStyle(fieldStyle);
        txtAdresse.setStyle(fieldStyle);
        txtSiret.setStyle(fieldStyle);
        txtNumTva.setStyle(fieldStyle);

        // Labels avec style
        String labelStyle = "-fx-text-fill: #333333; -fx-font-weight: bold;";

        Label lblType = new Label("Type :");
        Label lblNom = new Label("Nom :");
        Label lblPrenom = new Label("Prénom :");
        Label lblEmail = new Label("Email :");
        Label lblTel = new Label("Téléphone :");
        Label lblAdresse = new Label("Adresse :");
        Label lblSiret = new Label("SIRET :");
        Label lblNumTva = new Label("N° TVA :");

        lblType.setStyle(labelStyle);
        lblNom.setStyle(labelStyle);
        lblPrenom.setStyle(labelStyle);
        lblEmail.setStyle(labelStyle);
        lblTel.setStyle(labelStyle);
        lblAdresse.setStyle(labelStyle);
        lblSiret.setStyle(labelStyle);
        lblNumTva.setStyle(labelStyle);

        // Ajout au grid (SANS le label d'exemple)
        grid.add(lblType, 0, 0);
        grid.add(comboType, 1, 0);

        grid.add(lblNom, 0, 1);
        grid.add(txtNom, 1, 1);

        grid.add(lblPrenom, 0, 2);
        grid.add(txtPrenom, 1, 2);

        grid.add(lblEmail, 0, 3);
        grid.add(txtEmail, 1, 3);

        grid.add(lblTel, 0, 4);
        grid.add(txtTel, 1, 4);

        grid.add(lblAdresse, 0, 5);
        grid.add(txtAdresse, 1, 5);

        grid.add(lblSiret, 0, 6);
        grid.add(txtSiret, 1, 6);

        grid.add(lblNumTva, 0, 7);
        grid.add(txtNumTva, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(550);

        // Style des boutons
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(btnCreer);
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);

        if (btnOk != null) {
            btnOk.setStyle("-fx-background-color: #4B78CC; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16;");
        }
        if (btnCancel != null) {
            btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333333; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16;");
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != btnCreer) return;

        // Validation
        if (comboType.getValue() == null) {
            afficherErreur("Veuillez sélectionner un type (Client ou Fournisseur).");
            return;
        }
        if (txtNom.getText().trim().isEmpty()) {
            afficherErreur("Le nom est obligatoire.");
            return;
        }
        if (txtEmail.getText().trim().isEmpty()) {
            afficherErreur("L'email est obligatoire.");
            return;
        }

        String siret = txtSiret.getText().trim();
        if (!siret.isEmpty() && (siret.length() != 14 || !siret.matches("\\d+"))) {
            afficherErreur("Le SIRET doit contenir exactement 14 chiffres.");
            return;
        }

        Tiers client = new Tiers(
                0,
                txtNom.getText().trim(),
                txtPrenom.getText().trim(),
                comboType.getValue(),
                txtEmail.getText().trim(),
                txtAdresse.getText().trim(),
                txtTel.getText().trim(),
                siret,
                txtNumTva.getText().trim()
        );

        try {
            tiersService.ajouterTiers(client);
            afficherSucces("Client/Fournisseur créé avec succès !");
            chargerTousClients();
        } catch (Exception e) {
            afficherErreur("Erreur création : " + e.getMessage());
        }
    }

    @FXML
    private void modifierClient() {
        if (clientSelectionne == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier");
        dialog.setHeaderText("Modifier les informations du client/fournisseur");
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");

        ButtonType btnModifier = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnModifier, ButtonType.CANCEL);

        // Création du GridPane
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");

        // Configuration des colonnes
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setPrefWidth(120);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setFillWidth(true);

        grid.getColumnConstraints().addAll(col1, col2);

        // CHAMPS du formulaire
        ComboBox<TiersType> comboType = new ComboBox<>();
        TextField txtNom = new TextField(clientSelectionne.getNom());
        TextField txtPrenom = new TextField(clientSelectionne.getPrenom());
        TextField txtEmail = new TextField(clientSelectionne.getEmail());
        TextField txtTel = new TextField(clientSelectionne.getTel());
        TextField txtAdresse = new TextField(clientSelectionne.getAdresse());
        TextField txtSiret = new TextField(clientSelectionne.getSiret());
        TextField txtNumTva = new TextField(clientSelectionne.getNum_tva());

        // Configuration du ComboBox Type
        comboType.setItems(FXCollections.observableArrayList(TiersType.values()));
        comboType.setValue(clientSelectionne.getType());

        // Faire prendre toute la largeur aux champs
        comboType.setMaxWidth(Double.MAX_VALUE);
        txtNom.setMaxWidth(Double.MAX_VALUE);
        txtPrenom.setMaxWidth(Double.MAX_VALUE);
        txtEmail.setMaxWidth(Double.MAX_VALUE);
        txtTel.setMaxWidth(Double.MAX_VALUE);
        txtAdresse.setMaxWidth(Double.MAX_VALUE);
        txtSiret.setMaxWidth(Double.MAX_VALUE);
        txtNumTva.setMaxWidth(Double.MAX_VALUE);

        // Forcer la même hauteur pour tous les champs
        double prefHeight = 32;
        comboType.setPrefHeight(prefHeight);
        txtNom.setPrefHeight(prefHeight);
        txtPrenom.setPrefHeight(prefHeight);
        txtEmail.setPrefHeight(prefHeight);
        txtTel.setPrefHeight(prefHeight);
        txtAdresse.setPrefHeight(prefHeight);
        txtSiret.setPrefHeight(prefHeight);
        txtNumTva.setPrefHeight(prefHeight);

        // Style des champs
        String fieldStyle = "-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-padding: 6; -fx-text-fill: black;";
        comboType.setStyle(fieldStyle);
        txtNom.setStyle(fieldStyle);
        txtPrenom.setStyle(fieldStyle);
        txtEmail.setStyle(fieldStyle);
        txtTel.setStyle(fieldStyle);
        txtAdresse.setStyle(fieldStyle);
        txtSiret.setStyle(fieldStyle);
        txtNumTva.setStyle(fieldStyle);

        // Labels avec style
        String labelStyle = "-fx-text-fill: #333333; -fx-font-weight: bold;";

        Label lblType = new Label("Type :");
        Label lblNom = new Label("Nom :");
        Label lblPrenom = new Label("Prénom :");
        Label lblEmail = new Label("Email :");
        Label lblTel = new Label("Téléphone :");
        Label lblAdresse = new Label("Adresse :");
        Label lblSiret = new Label("SIRET :");
        Label lblNumTva = new Label("N° TVA :");

        lblType.setStyle(labelStyle);
        lblNom.setStyle(labelStyle);
        lblPrenom.setStyle(labelStyle);
        lblEmail.setStyle(labelStyle);
        lblTel.setStyle(labelStyle);
        lblAdresse.setStyle(labelStyle);
        lblSiret.setStyle(labelStyle);
        lblNumTva.setStyle(labelStyle);

        // Ajout au grid
        grid.add(lblType, 0, 0);
        grid.add(comboType, 1, 0);

        grid.add(lblNom, 0, 1);
        grid.add(txtNom, 1, 1);

        grid.add(lblPrenom, 0, 2);
        grid.add(txtPrenom, 1, 2);

        grid.add(lblEmail, 0, 3);
        grid.add(txtEmail, 1, 3);

        grid.add(lblTel, 0, 4);
        grid.add(txtTel, 1, 4);

        grid.add(lblAdresse, 0, 5);
        grid.add(txtAdresse, 1, 5);

        grid.add(lblSiret, 0, 6);
        grid.add(txtSiret, 1, 6);

        grid.add(lblNumTva, 0, 7);
        grid.add(txtNumTva, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(550);

        // Style des boutons
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(btnModifier);
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);

        if (btnOk != null) {
            btnOk.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16;");
        }
        if (btnCancel != null) {
            btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333333; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16;");
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != btnModifier) return;

        // Validation (COMME DANS CREATION)
        if (comboType.getValue() == null || txtNom.getText().trim().isEmpty() || txtEmail.getText().trim().isEmpty()) {
            afficherErreur("Le type, le nom et l'email sont obligatoires.");
            return;
        }

        String siret = txtSiret.getText().trim();
        if (!siret.isEmpty() && (siret.length() != 14 || !siret.matches("\\d+"))) {
            afficherErreur("Le SIRET doit contenir exactement 14 chiffres.");
            return;
        }

        clientSelectionne.setType(comboType.getValue());
        clientSelectionne.setNom(txtNom.getText().trim());
        clientSelectionne.setPrenom(txtPrenom.getText().trim());
        clientSelectionne.setEmail(txtEmail.getText().trim());
        clientSelectionne.setTel(txtTel.getText().trim());
        clientSelectionne.setAdresse(txtAdresse.getText().trim());
        clientSelectionne.setSiret(siret);
        clientSelectionne.setNum_tva(txtNumTva.getText().trim());

        try {
            tiersService.modifierTiers(clientSelectionne);
            afficherSucces("Client/Fournisseur modifié avec succès !");
            chargerTousClients();
            afficherDetail(clientSelectionne);
        } catch (Exception e) {
            afficherErreur("Erreur modification : " + e.getMessage());
        }
    }

    @FXML
    private void supprimerClient() {
        if (clientSelectionne == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer le client " + clientSelectionne.getNom() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    tiersService.supprimerTiers(clientSelectionne.getId());
                    afficherSucces("Client supprimé avec succès !");
                    viderDetail();
                    chargerTousClients();
                } catch (SQLException e) {
                    afficherErreur("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void filtrer() {
        String recherche = searchField.getText().toLowerCase().trim();
        String typeFiltre = comboTypeFiltre.getValue();

        List<Tiers> filtres = tousLesClients.stream()
                .filter(c -> recherche.isEmpty() ||
                        c.getNom().toLowerCase().contains(recherche) ||
                        c.getPrenom().toLowerCase().contains(recherche) ||
                        c.getEmail().toLowerCase().contains(recherche))
                .filter(c -> {
                    if (typeFiltre == null || typeFiltre.equals("Tous")) return true;
                    if (typeFiltre.equals("Client")) return c.getType() == TiersType.CLIENT;
                    if (typeFiltre.equals("Fournisseur")) return c.getType() == TiersType.FOURNISSEUR;
                    return true;
                })
                .toList();

        tableClients.setItems(FXCollections.observableArrayList(filtres));
        lblNbClients.setText(filtres.size() + " client(s)");
    }

    private void chargerTousClients() {
        try {
            tousLesClients.setAll(tiersService.obtenirTousLesTiers());
            tableClients.setItems(tousLesClients);
            lblNbClients.setText(tousLesClients.size() + " client(s)");
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les clients : " + e.getMessage());
        }
    }

    private void afficherDetail(Tiers client) {
        detailType.setText(client.getType().name());
        detailNom.setText(client.getNom());
        detailPrenom.setText(client.getPrenom() != null ? client.getPrenom() : "Non renseigné");
        detailEmail.setText(client.getEmail());
        detailTel.setText(client.getTel() != null ? client.getTel() : "Non renseigné");
        detailAdresse.setText(client.getAdresse() != null ? client.getAdresse() : "Non renseigné");
        detailSiret.setText(client.getSiret() != null ? client.getSiret() : "Non renseigné");
        detailNumTva.setText(client.getNum_tva() != null ? client.getNum_tva() : "Non renseigné");
    }

    private void viderDetail() {
        clientSelectionne = null;
        detailType.setText("");
        detailNom.setText("");
        detailPrenom.setText("");
        detailEmail.setText("");
        detailTel.setText("");
        detailAdresse.setText("");
        detailSiret.setText("");
        detailNumTva.setText("");
        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);
    }

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
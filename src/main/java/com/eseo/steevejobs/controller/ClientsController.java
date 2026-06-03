package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.TiersType;
import com.eseo.steevejobs.service.TiersService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.scene.layout.Region;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur FXML de gestion des clients et fournisseurs ({@code tiers}).
 * Liaisons FXML : {@code tableClients}, {@code searchField}, {@code comboTypeFiltre}, panneau détail.
 */
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

    // ─────────────────────────────────────────────────────────────
    // CSS
    // ─────────────────────────────────────────────────────────────

    private void applyCSS(Scene scene) {
        java.net.URL styleUrl = getClass().getResource("/style/style.css");
        java.net.URL popupUrl = getClass().getResource("/style/popup.css");
        if (styleUrl != null) scene.getStylesheets().add(styleUrl.toExternalForm());
        if (popupUrl != null) scene.getStylesheets().add(popupUrl.toExternalForm());
    }

    private void appliquerStyleDialog(DialogPane dp) {
        java.net.URL popupUrl = getClass().getResource("/style/popup.css");
        if (popupUrl != null) dp.getStylesheets().add(popupUrl.toExternalForm());

        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);
        if (btnOk != null) btnOk.getStyleClass().add("button-ok");

        Button btnCancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (btnCancel != null) btnCancel.getStyleClass().add("button-cancel");
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers UI
    // ─────────────────────────────────────────────────────────────

    private TextField champForm(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("champform");
        return tf;
    }

    private TextField champFormValeur(String valeur) {
        TextField tf = new TextField(valeur != null ? valeur : "");
        tf.getStyleClass().add("champform");
        return tf;
    }

    private ComboBox<TiersType> comboFormType() {
        ComboBox<TiersType> cb = new ComboBox<>();
        cb.setItems(FXCollections.observableArrayList(TiersType.values()));
        cb.setPromptText("Choisir un type");
        cb.getStyleClass().add("champform");
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    private HBox buildHeader(String titre) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("popup-header");
        Label titreLabel = new Label(titre);
        titreLabel.getStyleClass().add("popup-header-title");
        // un peu de padding pour coller le texte à gauche
        titreLabel.setPadding(new Insets(0, 0, 0, 4));
        header.getChildren().add(titreLabel);
        return header;
    }

    private Label labelChamp(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("label-style");
        return l;
    }

    private Label erreurLabel() {
        Label l = new Label("");
        l.getStyleClass().addAll("label-style", "label-erreur");
        return l;
    }

    // ─────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────

    /**
     * Configure les colonnes, charge les tiers et initialise filtres et sélection.
     *
     * @param url URL du FXML (non utilisée)
     * @param rb ressources de localisation (non utilisées)
     */
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
        colPrenom.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPrenom() != null ? data.getValue().getPrenom() : ""));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colTel.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTel() != null ? data.getValue().getTel() : ""));
        colSiret.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getSiret() != null ? data.getValue().getSiret() : ""));
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

    // ─────────────────────────────────────────────────────────────
    // Popup Nouveau Client
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void ouvrirFormulaireCreation() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Nouveau client / fournisseur");

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        // ── Champs ────────────────────────────────────────────────
        ComboBox<TiersType> comboType = comboFormType();
        TextField txtNom     = champForm("Nom");
        TextField txtPrenom  = champForm("Prénom");
        TextField txtEmail   = champForm("Email");
        TextField txtTel     = champForm("Téléphone");
        TextField txtAdresse = champForm("Adresse");
        TextField txtSiret   = champForm("SIRET (14 chiffres)");
        TextField txtNumTva  = champForm("N° TVA");

        Label errType    = erreurLabel();
        Label errNom     = erreurLabel();
        Label errEmail   = erreurLabel();
        Label errSiret   = erreurLabel();

        // ── Header ────────────────────────────────────────────────
        HBox header = buildHeader("Nouveau client / fournisseur");

        // ── GridPane 2 colonnes ────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(6, 6, 6, 6));

        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setHalignment(HPos.LEFT);   // texte du label aligné à gauche
        colLabel.setMinWidth(120);
        colLabel.setPrefWidth(140);
        colLabel.setMaxWidth(180);

        ColumnConstraints colField = new ColumnConstraints();
        colField.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(colLabel, colField);

        int row = 0;
        grid.add(new Label("Type *"), 0, row);
        grid.add(comboType, 1, row++);
        grid.add(errType, 1, row++);

        grid.add(new Label("Nom *"), 0, row);
        grid.add(txtNom, 1, row++);
        grid.add(errNom, 1, row++);

        grid.add(new Label("Prénom"), 0, row);
        grid.add(txtPrenom, 1, row++);

        grid.add(new Label("Email *"), 0, row);
        grid.add(txtEmail, 1, row++);
        grid.add(errEmail, 1, row++);

        grid.add(new Label("Téléphone"), 0, row);
        grid.add(txtTel, 1, row++);

        grid.add(new Label("Adresse"), 0, row);
        grid.add(txtAdresse, 1, row++);

        grid.add(new Label("SIRET"), 0, row);
        grid.add(txtSiret, 1, row++);
        grid.add(errSiret, 1, row++);

        grid.add(new Label("N° TVA"), 0, row);
        grid.add(txtNumTva, 1, row++);

        // Assure que chaque champ grandit horizontalement
        GridPane.setHgrow(comboType, Priority.ALWAYS);
        GridPane.setHgrow(txtNom, Priority.ALWAYS);
        GridPane.setHgrow(txtPrenom, Priority.ALWAYS);
        GridPane.setHgrow(txtEmail, Priority.ALWAYS);
        GridPane.setHgrow(txtTel, Priority.ALWAYS);
        GridPane.setHgrow(txtAdresse, Priority.ALWAYS);
        GridPane.setHgrow(txtSiret, Priority.ALWAYS);
        GridPane.setHgrow(txtNumTva, Priority.ALWAYS);

        // ── Contenu  ───────────────────────────────
        VBox contentBox = new VBox(12, header, grid);
        contentBox.getStyleClass().addAll("popup-contenu");
        contentBox.setPadding(new Insets(12));
        contentBox.setFillWidth(true);

        // On met le content directement dans le DialogPane pour que la taille s'ajuste
        dp.setContent(contentBox);

        // Applique le CSS du popup
        appliquerStyleDialog(dp);

        // Récupère les boutons
        Button btnCancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);

        if (btnOk != null) {
            btnOk.setText("Créer le client");
            btnOk.getStyleClass().add("button-primary");
            // Empêche fermeture si validation échoue
            btnOk.addEventFilter(ActionEvent.ACTION, ev -> {
                errType.setText(""); errNom.setText(""); errEmail.setText(""); errSiret.setText("");
                boolean valide = true;

                if (comboType.getValue() == null) {
                    errType.setText("Veuillez sélectionner un type."); valide = false;
                }
                if (txtNom.getText().trim().isEmpty()) {
                    errNom.setText("Le nom est obligatoire."); valide = false;
                }
                if (txtEmail.getText().trim().isEmpty()) {
                    errEmail.setText("L'email est obligatoire."); valide = false;
                }
                String siret = txtSiret.getText().trim();
                if (!siret.isEmpty() && (siret.length() != 14 || !siret.matches("\\d+"))) {
                    errSiret.setText("Le SIRET doit contenir exactement 14 chiffres."); valide = false;
                }

                if (!valide) {
                    ev.consume();
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
                    dialog.close();
                    chargerTousClients();
                    showInfo("Succès", "Client/Fournisseur créé avec succès !");
                } catch (Exception ex) {
                    errNom.setText("Erreur : " + ex.getMessage());
                    ev.consume();
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setText("Annuler");
            btnCancel.getStyleClass().add("button-annuler");
        }

        // Ajuste la taille du DialogPane : plus large par défaut, hauteur calculée sur le contenu
        dp.setPrefWidth(720);
        dp.setMinWidth(560);
        dp.setMinHeight(Region.USE_PREF_SIZE);
        dp.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dp.setMaxHeight(600);

        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    // Popup Modifier Client
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void modifierClient() {
        if (clientSelectionne == null) return;
        Tiers c = clientSelectionne;

        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Modifier le client");

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        ComboBox<TiersType> comboType = comboFormType();
        comboType.setValue(c.getType());

        TextField txtNom     = champFormValeur(c.getNom());
        TextField txtPrenom  = champFormValeur(c.getPrenom());
        TextField txtEmail   = champFormValeur(c.getEmail());
        TextField txtTel     = champFormValeur(c.getTel());
        TextField txtAdresse = champFormValeur(c.getAdresse());
        TextField txtSiret   = champFormValeur(c.getSiret());
        TextField txtNumTva  = champFormValeur(c.getNum_tva());

        Label errType  = erreurLabel();
        Label errNom   = erreurLabel();
        Label errEmail = erreurLabel();
        Label errSiret = erreurLabel();

        HBox header = buildHeader("Modifier : " + c.getNom());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(6, 6, 6, 6));

        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setHalignment(HPos.LEFT);
        colLabel.setMinWidth(120);
        colLabel.setPrefWidth(140);
        colLabel.setMaxWidth(180);

        ColumnConstraints colField = new ColumnConstraints();
        colField.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(colLabel, colField);

        int row = 0;
        grid.add(new Label("Type *"), 0, row);
        grid.add(comboType, 1, row++);
        grid.add(errType, 1, row++);

        grid.add(new Label("Nom *"), 0, row);
        grid.add(txtNom, 1, row++);
        grid.add(errNom, 1, row++);

        grid.add(new Label("Prénom"), 0, row);
        grid.add(txtPrenom, 1, row++);

        grid.add(new Label("Email *"), 0, row);
        grid.add(txtEmail, 1, row++);
        grid.add(errEmail, 1, row++);

        grid.add(new Label("Téléphone"), 0, row);
        grid.add(txtTel, 1, row++);

        grid.add(new Label("Adresse"), 0, row);
        grid.add(txtAdresse, 1, row++);

        grid.add(new Label("SIRET"), 0, row);
        grid.add(txtSiret, 1, row++);
        grid.add(errSiret, 1, row++);

        grid.add(new Label("N° TVA"), 0, row);
        grid.add(txtNumTva, 1, row++);

        GridPane.setHgrow(comboType, Priority.ALWAYS);
        GridPane.setHgrow(txtNom, Priority.ALWAYS);
        GridPane.setHgrow(txtPrenom, Priority.ALWAYS);
        GridPane.setHgrow(txtEmail, Priority.ALWAYS);
        GridPane.setHgrow(txtTel, Priority.ALWAYS);
        GridPane.setHgrow(txtAdresse, Priority.ALWAYS);
        GridPane.setHgrow(txtSiret, Priority.ALWAYS);
        GridPane.setHgrow(txtNumTva, Priority.ALWAYS);

        VBox contentBox = new VBox(12, header, grid);
        contentBox.getStyleClass().addAll("popup-contenu");
        contentBox.setPadding(new Insets(12));
        contentBox.setFillWidth(true);

        dp.setContent(contentBox);

        appliquerStyleDialog(dp);

        Button btnCancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);

        if (btnOk != null) {
            btnOk.setText("Enregistrer les modifications");
            btnOk.getStyleClass().add("button-primary");
            btnOk.addEventFilter(ActionEvent.ACTION, ev -> {
                errType.setText(""); errNom.setText(""); errEmail.setText(""); errSiret.setText("");
                boolean valide = true;

                if (comboType.getValue() == null) {
                    errType.setText("Veuillez sélectionner un type."); valide = false;
                }
                if (txtNom.getText().trim().isEmpty()) {
                    errNom.setText("Le nom est obligatoire."); valide = false;
                }
                if (txtEmail.getText().trim().isEmpty()) {
                    errEmail.setText("L'email est obligatoire."); valide = false;
                }
                String siret = txtSiret.getText().trim();
                if (!siret.isEmpty() && (siret.length() != 14 || !siret.matches("\\d+"))) {
                    errSiret.setText("Le SIRET doit contenir exactement 14 chiffres."); valide = false;
                }

                if (!valide) {
                    ev.consume();
                    return;
                }

                c.setType(comboType.getValue());
                c.setNom(txtNom.getText().trim());
                c.setPrenom(txtPrenom.getText().trim());
                c.setEmail(txtEmail.getText().trim());
                c.setTel(txtTel.getText().trim());
                c.setAdresse(txtAdresse.getText().trim());
                c.setSiret(siret);
                c.setNum_tva(txtNumTva.getText().trim());

                try {
                    tiersService.modifierTiers(c);
                    dialog.close();
                    chargerTousClients();
                    afficherDetail(c);
                    showInfo("Succès", "Client/Fournisseur modifié avec succès !");
                } catch (Exception ex) {
                    errNom.setText("Erreur : " + ex.getMessage());
                    ev.consume();
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setText("Annuler");
            btnCancel.getStyleClass().add("button-annuler");
        }

        dp.setPrefWidth(720);
        dp.setMinWidth(560);
        dp.setMinHeight(Region.USE_PREF_SIZE);
        dp.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dp.setMaxHeight(600);

        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    // Suppression
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void supprimerClient() {
        if (clientSelectionne == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer " + clientSelectionne.getNom() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        appliquerStyleDialog(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    tiersService.supprimerTiers(clientSelectionne.getId());
                    viderDetail();
                    chargerTousClients();
                    showInfo("Succès", "Client supprimé avec succès !");
                } catch (SQLException e) {
                    showError("Erreur suppression", e.getMessage());
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Filtre / Recherche
    // ─────────────────────────────────────────────────────────────

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
                    if (typeFiltre.equals("Client"))      return c.getType() == TiersType.CLIENT;
                    if (typeFiltre.equals("Fournisseur")) return c.getType() == TiersType.FOURNISSEUR;
                    return true;
                })
                .toList();

        tableClients.setItems(FXCollections.observableArrayList(filtres));
        lblNbClients.setText(filtres.size() + " client(s)");
    }

    // ─────────────────────────────────────────────────────────────
    // Chargement & détail
    // ─────────────────────────────────────────────────────────────

    private void chargerTousClients() {
        try {
            tousLesClients.setAll(tiersService.obtenirTousLesTiers());
            tableClients.setItems(tousLesClients);
            lblNbClients.setText(tousLesClients.size() + " client(s)");
        } catch (SQLException e) {
            showError("Erreur SQL", "Impossible de charger les clients : " + e.getMessage());
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
        detailType.setText(""); detailNom.setText(""); detailPrenom.setText("");
        detailEmail.setText(""); detailTel.setText(""); detailAdresse.setText("");
        detailSiret.setText(""); detailNumTva.setText("");
        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);
    }

    // ─────────────────────────────────────────────────────────────
    // Dialogs utilitaires
    // ─────────────────────────────────────────────────────────────

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        appliquerStyleDialog(a.getDialogPane());
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        appliquerStyleDialog(a.getDialogPane());
        a.showAndWait();
    }
}

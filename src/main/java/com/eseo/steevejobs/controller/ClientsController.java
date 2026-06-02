package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.TiersType;
import com.eseo.steevejobs.service.TiersService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
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
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Nouveau client / fournisseur");
        popup.setResizable(true);
        popup.setMinWidth(400);
        popup.setMinHeight(450);

        // ── Champs ────────────────────────────────────────────────
        ComboBox<TiersType> comboType = comboFormType();
        TextField txtNom     = champForm("Nom");
        TextField txtPrenom  = champForm("Prénom");
        TextField txtEmail   = champForm("Email");
        TextField txtTel     = champForm("Téléphone");
        TextField txtAdresse = champForm("Adresse");
        TextField txtSiret   = champForm("SIRET (14 chiffres)");
        TextField txtNumTva  = champForm("N° TVA");

        // Labels d'erreur
        Label errType    = erreurLabel();
        Label errNom     = erreurLabel();
        Label errEmail   = erreurLabel();
        Label errSiret   = erreurLabel();

        // ── Header ────────────────────────────────────────────────
        HBox header = buildHeader("Nouveau client / fournisseur");

        // ── Carte formulaire ──────────────────────────────────────
        VBox carte = new VBox(14);
        carte.getStyleClass().add("popup-carte");
        carte.getChildren().addAll(
                labelChamp("Type *"),     comboType,  errType,
                labelChamp("Nom *"),      txtNom,     errNom,
                labelChamp("Prénom"),     txtPrenom,
                labelChamp("Email *"),    txtEmail,   errEmail,
                labelChamp("Téléphone"),  txtTel,
                labelChamp("Adresse"),    txtAdresse,
                labelChamp("SIRET"),      txtSiret,   errSiret,
                labelChamp("N° TVA"),     txtNumTva
        );

        // ── Boutons ───────────────────────────────────────────────
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.getStyleClass().add("button-annuler");

        Button btnCreer = new Button("Créer le client");
        btnCreer.getStyleClass().add("button-primary");

        HBox boutons = new HBox(12, btnAnnuler, btnCreer);
        boutons.setAlignment(Pos.CENTER_RIGHT);

        // ── Contenu scrollable ────────────────────────────────────
        VBox contenu = new VBox(16, carte, boutons);
        contenu.getStyleClass().add("popup-contenu");

        ScrollPane scroll = new ScrollPane(contenu);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("rounded-scroll-pane");

        VBox root = new VBox(header, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getStyleClass().add("popup-root");

        // ── Actions ───────────────────────────────────────────────
        btnAnnuler.setOnAction(e -> popup.close());

        btnCreer.setOnAction(e -> {
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

            if (!valide) return;

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
                popup.close();
                chargerTousClients();
                showInfo("Succès", "Client/Fournisseur créé avec succès !");
            } catch (Exception ex) {
                errNom.setText("Erreur : " + ex.getMessage());
            }
        });

        // ── Affichage ─────────────────────────────────────────────
        Scene scene = new Scene(root, 480, 620);
        applyCSS(scene);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    // Popup Modifier Client
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void modifierClient() {
        if (clientSelectionne == null) return;
        Tiers c = clientSelectionne;

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Modifier le client");
        popup.setResizable(true);
        popup.setMinWidth(400);
        popup.setMinHeight(450);

        // ── Champs pré-remplis ────────────────────────────────────
        ComboBox<TiersType> comboType = comboFormType();
        comboType.setValue(c.getType());

        TextField txtNom     = champFormValeur(c.getNom());
        TextField txtPrenom  = champFormValeur(c.getPrenom());
        TextField txtEmail   = champFormValeur(c.getEmail());
        TextField txtTel     = champFormValeur(c.getTel());
        TextField txtAdresse = champFormValeur(c.getAdresse());
        TextField txtSiret   = champFormValeur(c.getSiret());
        TextField txtNumTva  = champFormValeur(c.getNum_tva());

        // Labels d'erreur
        Label errType  = erreurLabel();
        Label errNom   = erreurLabel();
        Label errEmail = erreurLabel();
        Label errSiret = erreurLabel();

        // ── Header ────────────────────────────────────────────────
        HBox header = buildHeader("Modifier : " + c.getNom());

        // ── Carte formulaire ──────────────────────────────────────
        VBox carte = new VBox(14);
        carte.getStyleClass().add("popup-carte");
        carte.getChildren().addAll(
                labelChamp("Type *"),     comboType,  errType,
                labelChamp("Nom *"),      txtNom,     errNom,
                labelChamp("Prénom"),     txtPrenom,
                labelChamp("Email *"),    txtEmail,   errEmail,
                labelChamp("Téléphone"),  txtTel,
                labelChamp("Adresse"),    txtAdresse,
                labelChamp("SIRET"),      txtSiret,   errSiret,
                labelChamp("N° TVA"),     txtNumTva
        );

        // ── Boutons ───────────────────────────────────────────────
        Button btnAnnuler      = new Button("Annuler");
        btnAnnuler.getStyleClass().add("button-annuler");

        Button btnEnregistrer  = new Button("Enregistrer les modifications");
        btnEnregistrer.getStyleClass().add("button-primary");

        HBox boutons = new HBox(12, btnAnnuler, btnEnregistrer);
        boutons.setAlignment(Pos.CENTER_RIGHT);

        // ── Contenu scrollable ────────────────────────────────────
        VBox contenu = new VBox(16, carte, boutons);
        contenu.getStyleClass().add("popup-contenu");

        ScrollPane scroll = new ScrollPane(contenu);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("rounded-scroll-pane");

        VBox root = new VBox(header, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getStyleClass().add("popup-root");

        // ── Actions ───────────────────────────────────────────────
        btnAnnuler.setOnAction(e -> popup.close());

        btnEnregistrer.setOnAction(ev -> {
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

            if (!valide) return;

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
                popup.close();
                chargerTousClients();
                afficherDetail(c);
                showInfo("Succès", "Client/Fournisseur modifié avec succès !");
            } catch (Exception ex) {
                errNom.setText("Erreur : " + ex.getMessage());
            }
        });

        // ── Affichage ─────────────────────────────────────────────
        Scene scene = new Scene(root, 480, 620);
        applyCSS(scene);
        popup.setScene(scene);
        popup.showAndWait();
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
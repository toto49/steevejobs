package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;
import com.eseo.steevejobs.service.DocumentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DocumentController implements Initializable {

    // ---- TableView ----
    @FXML private TextField                        searchField;
    @FXML private ComboBox<DocumentType>           comboTypeFiltre;
    @FXML private ComboBox<DocumentStatut>         comboStatutFiltre;
    @FXML private TableView<Document>              tableDocuments;
    @FXML private TableColumn<Document, String>    colType;
    @FXML private TableColumn<Document, String>    colClient;
    @FXML private TableColumn<Document, String>    colDate;
    @FXML private TableColumn<Document, String>    colHT;
    @FXML private TableColumn<Document, String>    colTTC;
    @FXML private TableColumn<Document, String>    colStatut;
    @FXML private TableColumn<Document, Void>      colActions;
    @FXML private Label                            lblNbDocs;

    // ---- Panneau détail ----
    @FXML private Label  detailType;
    @FXML private Label  detailClient;
    @FXML private Label  detailDate;
    @FXML private Label  detailHT;
    @FXML private Label  detailTTC;
    @FXML private Label  detailStatut;
    @FXML private VBox   lignesContainer;
    @FXML private Button btnOuvrirPdf;
    @FXML private Button btnChanger;
    @FXML private Button btnSupprimer;

    private final DocumentService documentService =
            new DocumentService(new DocumentDAO());
    private final TiersDAO tiersDAO = new TiersDAO();

    private ObservableList<Document> tousLesDocuments = FXCollections.observableArrayList();
    private Document documentSelectionne = null;

    private static final DateTimeFormatter FMT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        configurerFiltres();
        chargerTousDocuments();
        configurerSelectionTableau();

        // Désactiver les boutons détail tant que rien n'est sélectionné
        btnOuvrirPdf.setDisable(true);
        btnChanger.setDisable(true);
        btnSupprimer.setDisable(true);
    }

    // -------------------------------------------------------
    // Actions FXML

    @FXML
    private void ouvrirFormulaireCreation() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouveau document");
        dialog.setHeaderText("Créer un devis, une facture ou un bon de commande");

        ButtonType btnCreer = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCreer, ButtonType.CANCEL);

        ComboBox<DocumentType>   comboType   = new ComboBox<>();
        ComboBox<DocumentStatut> comboStatut = new ComboBox<>();
        ComboBox<Tiers>          comboTiers  = new ComboBox<>();
        DatePicker               datePicker  = new DatePicker();

        comboType.setItems(FXCollections.observableArrayList(DocumentType.values()));
        comboStatut.setItems(FXCollections.observableArrayList(DocumentStatut.values()));

        try {
            comboTiers.setItems(FXCollections.observableArrayList(tiersDAO.findAll()));
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les clients : " + e.getMessage());
            return;
        }

        comboTiers.setConverter(new StringConverter<>() {
            @Override public String toString(Tiers t) {
                return t == null ? "" : t.getNom() + (t.getPrenom() != null ? " " + t.getPrenom() : "");
            }
            @Override public Tiers fromString(String s) { return null; }
        });

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Type :"),   0, 0); grid.add(comboType,   1, 0);
        grid.add(new Label("Client :"), 0, 1); grid.add(comboTiers,  1, 1);
        grid.add(new Label("Date :"),   0, 2); grid.add(datePicker,  1, 2);
        grid.add(new Label("Statut :"), 0, 3); grid.add(comboStatut, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(460);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != btnCreer) return;

        if (comboType.getValue() == null || comboTiers.getValue() == null
                || datePicker.getValue() == null || comboStatut.getValue() == null) {
            afficherErreur("Tous les champs sont obligatoires.");
            return;
        }

        Document doc = new Document(
                0,
                comboType.getValue(),
                datePicker.getValue().atStartOfDay(),
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                comboStatut.getValue(),
                "",
                comboTiers.getValue(),
                null
        );

        try {
            // Pas de lignes à la création — on passe une liste vide
            documentService.ajouterDocument(doc, new ArrayList<>());
            afficherSucces("Document créé !\nFichier : " + doc.getUrl());
            chargerTousDocuments();
        } catch (Exception e) {
            afficherErreur("Erreur création : " + e.getMessage());
        }
    }

    @FXML
    private void filtrer() {
        String recherche = searchField.getText().toLowerCase().trim();
        DocumentType   type   = comboTypeFiltre.getValue();
        DocumentStatut statut = comboStatutFiltre.getValue();

        List<Document> filtres = tousLesDocuments.stream()
                .filter(d -> recherche.isEmpty() ||
                        d.getTiers().getNom().toLowerCase().contains(recherche))
                .filter(d -> type   == null || d.getType()   == type)
                .filter(d -> statut == null || d.getStatut() == statut)
                .toList();

        tableDocuments.setItems(FXCollections.observableArrayList(filtres));
        lblNbDocs.setText(filtres.size() + " document(s)");
    }

    @FXML
    private void ouvrirPdf() {
        if (documentSelectionne == null || documentSelectionne.getUrl() == null) return;
        try {
            File f = new File(documentSelectionne.getUrl());
            if (f.exists()) Desktop.getDesktop().open(f);
            else afficherErreur("Fichier introuvable :\n" + documentSelectionne.getUrl());
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir le PDF : " + e.getMessage());
        }
    }

    @FXML
    private void changerStatut() {
        if (documentSelectionne == null) return;

        ChoiceDialog<DocumentStatut> choix = new ChoiceDialog<>(
                documentSelectionne.getStatut(),
                DocumentStatut.values());
        choix.setTitle("Changer le statut");
        choix.setHeaderText("Nouveau statut pour ce document");

        choix.showAndWait().ifPresent(nouveauStatut -> {
            try {
                documentSelectionne.setStatut(nouveauStatut);
                documentService.modifierDocument(documentSelectionne);
                chargerTousDocuments();
                afficherDetail(documentSelectionne);
            } catch (SQLException e) {
                afficherErreur("Erreur mise à jour statut : " + e.getMessage());
            }
        });
    }

    @FXML
    private void supprimerDocument() {
        if (documentSelectionne == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le document");
        confirm.setHeaderText("Supprimer ce " + documentSelectionne.getType().name() +
                " du " + documentSelectionne.getDate().format(FMT_DATE) + " ?");
        confirm.setContentText("Le fichier PDF sera également supprimé. Action irréversible.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    documentService.supprimerDocument(documentSelectionne.getId());
                    if (documentSelectionne.getUrl() != null)
                        new File(documentSelectionne.getUrl()).delete();
                    viderDetail();
                    chargerTousDocuments();
                } catch (SQLException e) {
                    afficherErreur("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    // -------------------------------------------------------
    // Configuration interne

    private void configurerColonnes() {
        colType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getType().name()));
        colClient.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTiers().getNom()));
        colDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDate().format(FMT_DATE)));
        colHT.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f €", data.getValue().getPrixHt())));
        colTTC.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f €", data.getValue().getPrixTtc())));
        colStatut.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatut().name()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPdf = new Button("📄");
            private final HBox   box    = new HBox(btnPdf);
            {
                btnPdf.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14;");
                btnPdf.setOnAction(e -> {
                    Document d = getTableView().getItems().get(getIndex());
                    try { Desktop.getDesktop().open(new File(d.getUrl())); }
                    catch (Exception ex) { afficherErreur("Impossible d'ouvrir le PDF."); }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void configurerFiltres() {
        ObservableList<DocumentType> types = FXCollections.observableArrayList();
        types.add(null);
        types.addAll(DocumentType.values());
        comboTypeFiltre.setItems(types);
        comboTypeFiltre.setConverter(new StringConverter<>() {
            @Override public String toString(DocumentType t) { return t == null ? "Tous les types" : t.name(); }
            @Override public DocumentType fromString(String s) { return null; }
        });

        ObservableList<DocumentStatut> statuts = FXCollections.observableArrayList();
        statuts.add(null);
        statuts.addAll(DocumentStatut.values());
        comboStatutFiltre.setItems(statuts);
        comboStatutFiltre.setConverter(new StringConverter<>() {
            @Override public String toString(DocumentStatut s) { return s == null ? "Tous les statuts" : s.name(); }
            @Override public DocumentStatut fromString(String s) { return null; }
        });
    }

    private void configurerSelectionTableau() {
        tableDocuments.getSelectionModel().selectedItemProperty().addListener(
                (obs, ancien, nouveau) -> {
                    if (nouveau != null) {
                        documentSelectionne = nouveau;
                        afficherDetail(nouveau);
                        btnOuvrirPdf.setDisable(nouveau.getUrl() == null || nouveau.getUrl().isBlank());
                        btnChanger.setDisable(false);
                        btnSupprimer.setDisable(false);
                    }
                });
    }

    private void chargerTousDocuments() {
        try {
            // findAll() à ajouter dans DocumentDAO si absent
            List<Document> docs = documentService.getByTiersId(0); // remplace par findAll()
            tousLesDocuments = FXCollections.observableArrayList(docs);
            tableDocuments.setItems(tousLesDocuments);
            lblNbDocs.setText(docs.size() + " document(s)");
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les documents : " + e.getMessage());
        }
    }

    private void afficherDetail(Document doc) {
        detailType.setText(doc.getType().name());
        detailClient.setText(doc.getTiers().getNom());
        detailDate.setText(doc.getDate().format(FMT_DATE));
        detailHT.setText(String.format("%.2f €", doc.getPrixHt()));
        detailTTC.setText(String.format("%.2f €", doc.getPrixTtc()));
        detailStatut.setText(doc.getStatut().name());

        lignesContainer.getChildren().clear();
        try {
            List<Composer> lignes = documentService.getLignes(doc.getId());
            for (Composer ligne : lignes) {
                Label lblLigne = new Label(
                        ligne.getProduit().getNom() +
                                "  ×" + ligne.getQuantite().stripTrailingZeros().toPlainString() +
                                "  → " + String.format("%.2f €",
                                ligne.getPrixVente().multiply(ligne.getQuantite())));
                lblLigne.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px;");
                lignesContainer.getChildren().add(lblLigne);
            }
            if (lignes.isEmpty()) {
                lignesContainer.getChildren().add(
                        new Label("Aucune ligne produit.") {{
                            setStyle("-fx-text-fill: #9ca3af;");
                        }});
            }
        } catch (SQLException e) {
            lignesContainer.getChildren().add(new Label("Erreur chargement lignes."));
        }
    }

    private void viderDetail() {
        documentSelectionne = null;
        detailType.setText(""); detailClient.setText(""); detailDate.setText("");
        detailHT.setText(""); detailTTC.setText(""); detailStatut.setText("");
        lignesContainer.getChildren().clear();
        btnOuvrirPdf.setDisable(true);
        btnChanger.setDisable(true);
        btnSupprimer.setDisable(true);
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

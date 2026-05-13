package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.DocumentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DocumentController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<DocumentType> comboTypeFiltre;
    @FXML private ComboBox<DocumentStatut> comboStatutFiltre;
    @FXML private TableView<Document> tableDocuments;
    @FXML private TableColumn<Document, String> colType, colClient, colDate, colHT, colTTC, colStatut;
    @FXML private TableColumn<Document, Void> colActions;
    @FXML private Label lblNbDocs;

    // Labels du panneau détail
    @FXML private Label detailType, detailClient, detailDate, detailHT, detailTTC, detailStatut;
    @FXML private Label detailEmail, detailTel, detailAdresse;
    @FXML private VBox lignesContainer;
    @FXML private Button btnExporterPdf, btnOuvrirPdf, btnModifier, btnChanger, btnSupprimer;

    private final DocumentService documentService = new DocumentService(new DocumentDAO());
    private final TiersDAO tiersDAO = new TiersDAO();
    private ObservableList<Document> tousLesDocuments = FXCollections.observableArrayList();
    private Document documentSelectionne = null;

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private User utilisateurConnecte;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        //supprime les colonnes fantomes
        tableDocuments.getColumns().removeIf(col -> col.getText() == null || col.getText().isEmpty());
        tableDocuments.getColumns().removeIf(col -> {
            String text = col.getText();
            return (text == null || text.isEmpty()) && col != colActions;
        });
        configurerFiltres();
        chargerTousDocuments();
        configurerSelectionTableau();
        btnExporterPdf.setDisable(true);
        btnOuvrirPdf.setDisable(true);
        btnModifier.setDisable(true);
        btnChanger.setDisable(true);
        btnSupprimer.setDisable(true);
    }

    public void setUtilisateurConnecte(User user) {
        this.utilisateurConnecte = user;
    }

    @FXML
    private void ouvrirFormulaireCreation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/nouveau-document-view.fxml"));
            Parent root = loader.load();
            NouveauDocumentController controller = loader.getController();
            controller.setUtilisateurConnecte(utilisateurConnecte);
            Stage stage = new Stage();
            stage.setTitle("Nouveau document");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            chargerTousDocuments();
        } catch (IOException e) {
            afficherErreur("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    @FXML
    private void modifierDocument() {
        if (documentSelectionne == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/modifier-document-view.fxml"));
            Parent root = loader.load();
            ModifierDocumentController controller = loader.getController();
            controller.setDocument(documentSelectionne);
            Stage stage = new Stage();
            stage.setTitle("Modifier document");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            chargerTousDocuments();
        } catch (IOException e) {
            afficherErreur("Erreur ouverture modification : " + e.getMessage());
        }
    }

    @FXML
    private void exporterPdf() {
        if (documentSelectionne == null) return;
        try {
            String url = documentService.exporterPdf(documentSelectionne.getId());
            documentSelectionne.setUrl(url);
            btnOuvrirPdf.setDisable(false);
            afficherSucces("PDF généré : " + url);
            chargerTousDocuments();
        } catch (Exception e) {
            afficherErreur("Erreur génération PDF : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirPdf() {
        if (documentSelectionne == null || documentSelectionne.getUrl() == null) return;
        try {
            File f = new File(documentSelectionne.getUrl());
            if (f.exists()) Desktop.getDesktop().open(f);
            else afficherErreur("Fichier introuvable");
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir le PDF");
        }
    }

    @FXML
    private void changerStatut() {
        if (documentSelectionne == null) return;

        ChoiceDialog<DocumentStatut> choix = new ChoiceDialog<>(documentSelectionne.getStatut(), DocumentStatut.values());
        choix.setTitle("Changer le statut");
        choix.setHeaderText("Nouveau statut pour ce document");
        // Charge le CSS
        choix.getDialogPane().getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
        choix.showAndWait().ifPresent(nouveauStatut -> {
            try {
                documentSelectionne.setStatut(nouveauStatut);
                documentService.modifierDocument(documentSelectionne);
                chargerTousDocuments();
                afficherDetail(documentSelectionne);
            } catch (SQLException e) {
                afficherErreur("Erreur mise à jour statut");
            }
        });
    }

    @FXML
    private void supprimerDocument() {
        if (documentSelectionne == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer ce document ?");

        // Charge le CSS
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    documentService.supprimerDocument(documentSelectionne.getId());
                    viderDetail();
                    chargerTousDocuments();
                } catch (SQLException e) {
                    afficherErreur("Erreur suppression");
                }
            }
        });
    }

    @FXML
    private void filtrer() {
        String recherche = searchField.getText().toLowerCase();
        DocumentType type = comboTypeFiltre.getValue();
        DocumentStatut statut = comboStatutFiltre.getValue();
        List<Document> filtres = tousLesDocuments.stream()
                .filter(d -> recherche.isEmpty() || d.getTiers().getNom().toLowerCase().contains(recherche))
                .filter(d -> type == null || d.getType() == type)
                .filter(d -> statut == null || d.getStatut() == statut)
                .toList();
        tableDocuments.setItems(FXCollections.observableArrayList(filtres));
        lblNbDocs.setText(filtres.size() + " document(s)");
    }

    private void configurerColonnes() {
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().name()));
        colClient.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTiers().getNom()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate().format(FMT_DATE)));
        colHT.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f €", data.getValue().getPrixHt())));
        colTTC.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f €", data.getValue().getPrixTtc())));
        colStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatut().name()));
    //evite les colonne fantome
        tableDocuments.getColumns().setAll(colType, colClient, colDate, colHT, colTTC, colStatut, colActions);

    }

    private void configurerFiltres() {
        comboTypeFiltre.setItems(FXCollections.observableArrayList(DocumentType.values()));
        comboTypeFiltre.getItems().add(0, null);
        comboTypeFiltre.setConverter(new StringConverter<>() {
            @Override public String toString(DocumentType t) { return t == null ? "Tous les types" : t.name(); }
            @Override public DocumentType fromString(String s) { return null; }
        });
        comboStatutFiltre.setItems(FXCollections.observableArrayList(DocumentStatut.values()));
        comboStatutFiltre.getItems().add(0, null);
        comboStatutFiltre.setConverter(new StringConverter<>() {
            @Override public String toString(DocumentStatut s) { return s == null ? "Tous les statuts" : s.name(); }
            @Override public DocumentStatut fromString(String s) { return null; }
        });
    }

    private void configurerSelectionTableau() {
        tableDocuments.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                documentSelectionne = nouveau;
                afficherDetail(nouveau);
                boolean pdfExiste = nouveau.getUrl() != null && !nouveau.getUrl().isBlank();
                boolean fichierExiste = pdfExiste && new File(nouveau.getUrl()).exists();
                btnExporterPdf.setDisable(false);
                btnOuvrirPdf.setDisable(!fichierExiste);
                btnModifier.setDisable(false);
                btnChanger.setDisable(false);
                btnSupprimer.setDisable(false);
            }
        });
    }

    private void chargerTousDocuments() {
        try {
            tousLesDocuments.setAll(documentService.findAll());
            tableDocuments.setItems(tousLesDocuments);
            lblNbDocs.setText(tousLesDocuments.size() + " document(s)");
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les documents");
        }
    }

    private void afficherDetail(Document doc) {
        // Type de document
        detailType.setText(doc.getType().name());

        // Informations client
        if (doc.getTiers() != null) {
            Tiers client = doc.getTiers();
            detailClient.setText((client.getNom() != null ? client.getNom() : "") + " " + (client.getPrenom() != null ? client.getPrenom() : ""));
            detailEmail.setText(client.getEmail() != null && !client.getEmail().isEmpty() ? client.getEmail() : "Non renseigné");
            detailTel.setText(client.getTel() != null && !client.getTel().isEmpty() ? client.getTel() : "Non renseigné");
            detailAdresse.setText(client.getAdresse() != null && !client.getAdresse().isEmpty() ? client.getAdresse() : "Non renseigné");
        } else {
            detailClient.setText("Client non renseigné");
            detailEmail.setText("Non renseigné");
            detailTel.setText("Non renseigné");
            detailAdresse.setText("Non renseigné");
        }

        // Informations document
        detailDate.setText(doc.getDate().format(FMT_DATE));
        detailHT.setText(String.format("%.2f €", doc.getPrixHt()));
        detailTTC.setText(String.format("%.2f €", doc.getPrixTtc()));
        detailStatut.setText(doc.getStatut().name());
        detailStatut.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");

        // Lignes produits
        lignesContainer.getChildren().clear();

        try {
            for (Composer ligne : documentService.getLignes(doc.getId())) {
                Produit produit = ligne.getProduit();
                BigDecimal quantite = ligne.getQuantite();

                // Détermine l'unité (kg ou unité(s))
                String unite = (produit.getPoid() != null && produit.getPoid().compareTo(BigDecimal.ZERO) > 0) ? "kg" : "unité(s)";

                // Affiche avec l'unité
                Label lbl = new Label(
                        "• " + produit.getNom() + " : " + quantite.stripTrailingZeros().toPlainString() + " " + unite +
                                " → " + String.format("%.2f €", ligne.getPrixVente().multiply(quantite))
                );
                lbl.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px;");
                lignesContainer.getChildren().add(lbl);
            }
            if (lignesContainer.getChildren().isEmpty()) {
                Label lbl = new Label("Aucune ligne produit");
                lbl.setStyle("-fx-text-fill: #9ca3af;");
                lignesContainer.getChildren().add(lbl);
            }
        } catch (SQLException e) {
            Label lbl = new Label("Erreur chargement lignes");
            lbl.setStyle("-fx-text-fill: #E81123;");
            lignesContainer.getChildren().add(lbl);
        }
    }

    private void viderDetail() {
        documentSelectionne = null;
        detailType.setText("");
        detailClient.setText("");
        detailEmail.setText("");
        detailTel.setText("");
        detailAdresse.setText("");
        detailDate.setText("");
        detailHT.setText("");
        detailTTC.setText("");
        detailStatut.setText("");
        lignesContainer.getChildren().clear();
        btnExporterPdf.setDisable(true);
        btnOuvrirPdf.setDisable(true);
        btnModifier.setDisable(true);
        btnChanger.setDisable(true);
        btnSupprimer.setDisable(true);
    }

    private void afficherErreur(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void afficherSucces(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
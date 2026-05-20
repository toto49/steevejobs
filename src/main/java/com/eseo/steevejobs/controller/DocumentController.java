package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.*;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;
import com.eseo.steevejobs.service.DocumentService;
import com.eseo.steevejobs.service.WebDavService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

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
    private final ObservableList<Document> tousLesDocuments = FXCollections.observableArrayList();
    private Document documentSelectionne = null;

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private User utilisateurConnecte;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        // Supprime les colonnes fantômes
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

        btnExporterPdf.setDisable(true);
        String texteOriginal = btnExporterPdf.getText();
        btnExporterPdf.setText("Génération...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return documentService.exporterPdf(documentSelectionne.getId());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).thenAcceptAsync(url -> {
            Platform.runLater(() -> {
                documentSelectionne.setUrl(url);
                btnExporterPdf.setText(texteOriginal);
                btnExporterPdf.setDisable(false);
                btnOuvrirPdf.setDisable(false);
                afficherSucces("PDF exporté dans le dossier 'Téléchargements' et synchronisé sur le NAS !");
                chargerTousDocuments();
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                btnExporterPdf.setText(texteOriginal);
                btnExporterPdf.setDisable(false);
                afficherErreur("Erreur génération PDF/NAS : " + ex.getCause().getMessage());
            });
            return null;
        });
    }

    @FXML
    private void ouvrirPdf() {
        if (documentSelectionne == null) return;

        btnOuvrirPdf.setDisable(true);
        String texteOriginal = btnOuvrirPdf.getText();
        btnOuvrirPdf.setText("Récupération...");
        String nomFichier = String.format("%s_%d.pdf",
                documentSelectionne.getType().getValeur().replace(" ", "_"),
                documentSelectionne.getId());
        System.out.println("DEBUG - Le Java cherche ce fichier sur le NAS : " + nomFichier);
        String cheminLocal = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + nomFichier;
        File f = new File(cheminLocal);

        CompletableFuture.runAsync(() -> {
            try {
                if (!f.exists()) {
                    WebDavService.telechargerFichierDuNAS("documents_commerciaux", nomFichier, cheminLocal);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAcceptAsync(v -> {
            Platform.runLater(() -> {
                btnOuvrirPdf.setText(texteOriginal);
                btnOuvrirPdf.setDisable(false);
                try {
                    if (f.exists()) {
                        Desktop.getDesktop().open(f);
                    } else {
                        afficherErreur("Le fichier est introuvable après le téléchargement.");
                    }
                } catch (IOException ex) {
                    afficherErreur("Impossible d'ouvrir le fichier. Vérifiez qu'un lecteur PDF est installé.");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                btnOuvrirPdf.setText(texteOriginal);
                btnOuvrirPdf.setDisable(false);
                afficherErreur("Erreur lors de la récupération depuis le NAS : " + ex.getCause().getMessage());
            });
            return null;
        });
    }

    @FXML
    private void changerStatut() {
        if (documentSelectionne == null) return;

        ChoiceDialog<DocumentStatut> choix = new ChoiceDialog<>(documentSelectionne.getStatut(), DocumentStatut.values());
        choix.setTitle("Changer le statut");
        choix.setHeaderText("Nouveau statut pour ce document");
        choix.getDialogPane().getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
        choix.showAndWait().ifPresent(nouveauStatut -> {
            try {
                documentSelectionne.setStatut(nouveauStatut);
                documentService.modifierDocument(documentSelectionne);
                CompletableFuture.runAsync(() -> {
                    try {
                        documentService.exporterPdf(documentSelectionne.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                chargerTousDocuments();
                afficherDetail(documentSelectionne);
                afficherSucces("Statut modifié et PDF mis à jour !");
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
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    documentService.supprimerDocument(documentSelectionne.getId());
                    viderDetail();
                    chargerTousDocuments();
                } catch (SQLException e) {
                    afficherErreur("Erreur suppression : " + e.getMessage());
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
        // Supprime les underscores dans l'affichage du type et du statut
        colType.setCellValueFactory(data -> {
            String type = data.getValue().getType().name();
            if ("BON_COMMANDE".equals(type)) {
                return new SimpleStringProperty("BON DE COMMANDE");
            }
            return new SimpleStringProperty(type);
        });
        colClient.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTiers().getNom()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate().format(FMT_DATE)));
        colHT.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f €", data.getValue().getPrixHt())));
        colTTC.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f €", data.getValue().getPrixTtc())));
        colStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatut().name().replace("_", " ")));
        tableDocuments.getColumns().setAll(colType, colClient, colDate, colHT, colTTC, colStatut, colActions);
    }

    private void configurerFiltres() {
        comboTypeFiltre.setItems(FXCollections.observableArrayList(DocumentType.values()));
        comboTypeFiltre.getItems().add(0, null);
        comboTypeFiltre.setConverter(new StringConverter<>() {
            @Override
            public String toString(DocumentType t) {
                if (t == null) return "Tous les types";
                if (t == DocumentType.BON_COMMANDE) return "BON DE COMMANDE";
                return t.name();
            }
            @Override public DocumentType fromString(String s) { return null; }
        });
        comboStatutFiltre.setItems(FXCollections.observableArrayList(DocumentStatut.values()));
        comboStatutFiltre.getItems().add(0, null);
        comboStatutFiltre.setConverter(new StringConverter<>() {
            @Override public String toString(DocumentStatut s) { return s == null ? "Tous les statuts" : s.name().replace("_", " "); }
            @Override public DocumentStatut fromString(String s) { return null; }
        });
    }

    private void configurerSelectionTableau() {
        tableDocuments.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                documentSelectionne = nouveau;
                afficherDetail(nouveau);

                boolean pdfExiste = nouveau.getUrl() != null && !nouveau.getUrl().isBlank();

                btnExporterPdf.setDisable(false);
                btnOuvrirPdf.setDisable(!pdfExiste);
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
        // Supprime les underscores dans l'affichage du type
        String type = doc.getType().name();
        if ("BON_COMMANDE".equals(type)) {
            detailType.setText("BON DE COMMANDE");
        } else {
            detailType.setText(type);
        }

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

        detailDate.setText(doc.getDate().format(FMT_DATE));
        detailHT.setText(String.format("%.2f €", doc.getPrixHt()));
        detailTTC.setText(String.format("%.2f €", doc.getPrixTtc()));
        // Supprime les underscores dans l'affichage du statut
        detailStatut.setText(doc.getStatut().name().replace("_", " "));
        detailStatut.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");

        lignesContainer.getChildren().clear();

        try {
            for (Composer ligne : documentService.getLignes(doc.getId())) {
                Produit produit = ligne.getProduit();
                BigDecimal quantite = ligne.getQuantite();
                String unite = (produit.getPoid() != null && produit.getPoid().compareTo(BigDecimal.ZERO) > 0) ? "kg" : "unité(s)";

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
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");
        alert.showAndWait();
    }

    private void afficherSucces(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");
        alert.showAndWait();
    }
}
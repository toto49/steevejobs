package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.FichePayeService;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.WebDavService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class DocumentUserController implements Initializable {

    private final FichePayeService fichePayeService = new FichePayeService();
    private final ObservableList<FichePaye> mesFiches = FXCollections.observableArrayList();
    @FXML
    private TableView<FichePaye> tableFiches;
    @FXML
    private TableColumn<FichePaye, LocalDateTime> colPeriode;
    @FXML
    private Button btnOuvrirPdf;
    @FXML
    private Label lblMessage;
    private User utilisateurConnecte;
    private FichePaye ficheSelectionnee = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        utilisateurConnecte = SessionService.getUtilisateurConnecte();

        configurerColonnes();
        configurerSelectionTableau();
        chargerMesFiches();

        btnOuvrirPdf.setDisable(true);
    }

    private void configurerColonnes() {
        colPeriode.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDate()));

        colPeriode.setCellFactory(col -> new TableCell<FichePaye, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String periode = item.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
                    periode = periode.substring(0, 1).toUpperCase() + periode.substring(1);
                    setText(periode);
                }
            }
        });

        colPeriode.setSortType(TableColumn.SortType.DESCENDING);
        tableFiches.getSortOrder().add(colPeriode);
    }

    private void configurerSelectionTableau() {
        tableFiches.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                ficheSelectionnee = nouveau;
                btnOuvrirPdf.setDisable(false);
            } else {
                ficheSelectionnee = null;
                btnOuvrirPdf.setDisable(true);
            }
        });
    }

    private void chargerMesFiches() {
        if (utilisateurConnecte == null) return;

        try {
            mesFiches.setAll(fichePayeService.findByEmployeId(utilisateurConnecte.getId()));
            tableFiches.setItems(mesFiches);

            tableFiches.sort();

            if (mesFiches.isEmpty()) {
                lblMessage.setText("Aucune fiche de paie disponible.");
            } else {
                lblMessage.setText(mesFiches.size() + " fiche(s) trouvée(s).");
            }
        } catch (SQLException e) {
            afficherErreur("Impossible de charger vos fiches de paie : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirPdf() {
        if (ficheSelectionnee == null) return;

        btnOuvrirPdf.setDisable(true);
        String texteOriginal = btnOuvrirPdf.getText();
        btnOuvrirPdf.setText("Téléchargement...");
        String nomFichier = String.format("fiche_%d_%d_%02d.pdf",
                utilisateurConnecte.getId(),
                ficheSelectionnee.getDate().getYear(),
                ficheSelectionnee.getDate().getMonthValue());

        String dossierEmploye = "employe_" + utilisateurConnecte.getId();
        if (utilisateurConnecte.getPrenom() != null && utilisateurConnecte.getNom() != null) {
            dossierEmploye = (utilisateurConnecte.getPrenom() + "_" + utilisateurConnecte.getNom())
                    .toLowerCase()
                    .replaceAll("[^a-z0-9_]", "");
        }

        String cheminLocal = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + nomFichier;
        File f = new File(cheminLocal);

        final String finalDossier = dossierEmploye;

        CompletableFuture.runAsync(() -> {
            try {
                if (!f.exists()) {
                    WebDavService.telechargerFichierDuNAS(finalDossier, nomFichier, cheminLocal);
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
                        lblMessage.setText("Fiche téléchargée et ouverte.");
                    } else {
                        afficherErreur("Fichier introuvable après téléchargement.");
                    }
                } catch (IOException ex) {
                    afficherErreur("Impossible d'ouvrir le fichier. Lecteur PDF manquant ?");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                btnOuvrirPdf.setText(texteOriginal);
                btnOuvrirPdf.setDisable(false);
                ex.printStackTrace();
                afficherErreur("La fiche n'est pas encore disponible sur le NAS.");
            });
            return null;
        });
    }

    private void afficherErreur(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.service.DocumentService;
import com.eseo.steevejobs.service.ProduitService;
import com.eseo.steevejobs.service.TiersService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur FXML du formulaire de modification de document commercial.
 * Liaisons FXML : combos type/tiers/statut, tableau des lignes, totaux HT/TVA/TTC.
 */
public class ModifierDocumentController implements Initializable {

    @FXML private ComboBox<DocumentType> comboType;
    @FXML private ComboBox<Tiers> comboTiers;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<DocumentStatut> comboStatut;
    @FXML private ComboBox<Produit> comboProduit;
    @FXML private TextField txtQuantite;
    @FXML private TextField txtPrixVente;
    @FXML private Button btnAjouterLigne;
    @FXML private TableView<Composer> tableLignes;
    @FXML private TableColumn<Composer, String> colProduit, colQuantite, colPrixUnitaire, colTotalLigne;
    @FXML private TableColumn<Composer, Void> colActions;
    @FXML private Label lblTotalHT, lblTVA, lblTotalTTC;
    @FXML private Button btnAnnuler, btnModifier;

    private final TiersService tiersService = new TiersService();
    private final ProduitService produitService = new ProduitService();
    private final DocumentService documentService = new DocumentService();

    private final ObservableList<Composer> lignes = FXCollections.observableArrayList();
    private Document documentModification;

    /**
     * Configure combos, tableau des lignes et charge tiers/produits.
     *
     * @param url URL du FXML (non utilisée)
     * @param rb ressources de localisation (non utilisées)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerComboBox();
        configurerTableLignes();
        chargerDonnees();
        tableLignes.setItems(lignes);
    }

    /**
     * Préremplit le formulaire avec le document à modifier.
     *
     * @param document document source ; ne doit pas être {@code null}
     */
    public void setDocument(Document document) {
        this.documentModification = document;

        comboType.setValue(document.getType());
        comboTiers.setValue(document.getTiers());
        datePicker.setValue(document.getDate().toLocalDate());
        comboStatut.setValue(document.getStatut());

        try {
            lignes.setAll(documentService.findLignesByDocumentId(document.getId()));
            updateTotaux();
        } catch (SQLException e) {
            afficherErreur("Erreur chargement des lignes : " + e.getMessage());
        }
    }

    private void configurerComboBox() {
        comboType.setItems(FXCollections.observableArrayList(DocumentType.values()));
        comboStatut.setItems(FXCollections.observableArrayList(DocumentStatut.values()));

        comboTiers.setConverter(new StringConverter<>() {
            @Override public String toString(Tiers t) { return t == null ? "" : t.getNom() + (t.getPrenom() != null ? " " + t.getPrenom() : ""); }
            @Override public Tiers fromString(String s) { return null; }
        });

        comboProduit.setConverter(new StringConverter<>() {
            @Override public String toString(Produit p) { return p == null ? "" : p.getNom(); }
            @Override public Produit fromString(String s) { return null; }
        });

        comboProduit.setOnAction(e -> {
            Produit p = comboProduit.getValue();
            if (p != null && p.getPrix() != null) {
                txtPrixVente.setText(p.getPrix().toString());
            }
        });
    }

    private void configurerTableLignes() {
        colProduit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduit().getNom()));

        colQuantite.setCellValueFactory(data -> {
            Produit p = data.getValue().getProduit();
            BigDecimal quantite = data.getValue().getQuantite();
            String unite = (p.getPoid() != null && p.getPoid().compareTo(BigDecimal.ZERO) > 0) ? "kg" : "unité(s)";
            return new SimpleStringProperty(quantite.stripTrailingZeros().toPlainString() + " " + unite);
        });

        colPrixUnitaire.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f €", data.getValue().getPrixVente())));
        colTotalLigne.setCellValueFactory(data -> {
            BigDecimal total = data.getValue().getPrixVente().multiply(data.getValue().getQuantite());
            return new SimpleStringProperty(String.format("%.2f €", total));
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnSuppr = new Button("🗑");
            {
                btnSuppr.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #E81123;");
                btnSuppr.setOnAction(e -> {
                    lignes.remove(getIndex());
                    updateTotaux();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : btnSuppr);
            }
        });
    }

    private void chargerDonnees() {
        try {
            comboTiers.setItems(FXCollections.observableArrayList(tiersService.findAll()));
            comboProduit.setItems(FXCollections.observableArrayList(produitService.findAllActive()));
        } catch (SQLException e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    @FXML
    private void ajouterLigne() {
        Produit produit = comboProduit.getValue();
        String qStr = txtQuantite.getText();
        String pStr = txtPrixVente.getText();

        if (produit == null || qStr.isEmpty() || pStr.isEmpty()) {
            afficherErreur("Remplissez tous les champs");
            return;
        }

        try {
            BigDecimal quantite = new BigDecimal(qStr);
            BigDecimal prix = new BigDecimal(pStr);

            boolean estProduitPoids = produit.getPoid() != null && produit.getPoid().compareTo(BigDecimal.ZERO) > 0;

            if (!estProduitPoids) {
                if (quantite.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                    afficherErreur("La quantité doit être un nombre entier pour ce produit (unités)");
                    return;
                }
                if (quantite.compareTo(BigDecimal.ZERO) <= 0) {
                    afficherErreur("La quantité doit être supérieure à 0");
                    return;
                }
            } else {
                if (quantite.compareTo(BigDecimal.ZERO) <= 0) {
                    afficherErreur("Le poids doit être supérieur à 0 kg");
                    return;
                }
            }

            lignes.add(new Composer(0, produit, quantite, prix));
            txtQuantite.clear();
            txtPrixVente.clear();
            comboProduit.setValue(null);
            updateTotaux();
        } catch (NumberFormatException e) {
            afficherErreur("Quantité ou prix invalide");
        }
    }

    private void updateTotaux() {
        BigDecimal totalHT = BigDecimal.ZERO;
        for (Composer ligne : lignes) {
            totalHT = totalHT.add(ligne.getPrixVente().multiply(ligne.getQuantite()));
        }
        BigDecimal tva = totalHT.multiply(new BigDecimal("0.20"));
        BigDecimal totalTTC = totalHT.add(tva);

        lblTotalHT.setText(String.format("%.2f €", totalHT));
        lblTVA.setText(String.format("%.2f €", tva));
        lblTotalTTC.setText(String.format("%.2f €", totalTTC));
    }

    @FXML
    private void modifierDocument() {
        if (comboType.getValue() == null || comboTiers.getValue() == null
                || datePicker.getValue() == null || comboStatut.getValue() == null) {
            afficherErreur("Tous les champs sont obligatoires");
            return;
        }

        if (lignes.isEmpty()) {
            afficherErreur("Le document doit contenir au moins un produit.");
            return;
        }

        BigDecimal totalHT = BigDecimal.ZERO;
        for (Composer ligne : lignes) {
            totalHT = totalHT.add(ligne.getPrixVente().multiply(ligne.getQuantite()));
        }
        BigDecimal totalTTC = totalHT.multiply(new BigDecimal("1.20"));

        documentModification.setType(comboType.getValue());
        documentModification.setTiers(comboTiers.getValue());
        documentModification.setDate(datePicker.getValue().atStartOfDay());
        documentModification.setStatut(comboStatut.getValue());
        documentModification.setPrixHt(totalHT);
        documentModification.setPrixTtc(totalTTC);
        btnModifier.setDisable(true);
        btnAnnuler.setDisable(true);

        try {
            documentService.modifierDocumentAvecLignes(documentModification, List.copyOf(lignes));
            CompletableFuture.supplyAsync(() -> {
                try {
                    return documentService.exporterPdf(documentModification.getId());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).thenAcceptAsync(urlPdf -> {
                Platform.runLater(() -> {
                    afficherSucces("Document mis à jour en base et sur le NAS !");
                    fermer();
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    ex.printStackTrace();
                    afficherErreur("Mis à jour en BDD, mais échec du PDF/NAS : " + ex.getCause().getMessage());
                    btnModifier.setDisable(false);
                    btnAnnuler.setDisable(false);
                });
                return null;
            });

        } catch (Exception e) {
            afficherErreur("Erreur modification BDD : " + e.getMessage());
            btnModifier.setDisable(false);
            btnAnnuler.setDisable(false);
        }
    }

    @FXML
    private void fermer() {
        ((Stage) btnAnnuler.getScene().getWindow()).close();
    }

    private void afficherErreur(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void afficherSucces(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}

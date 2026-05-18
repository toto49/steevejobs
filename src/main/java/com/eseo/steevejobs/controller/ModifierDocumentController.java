package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.ComposerDAO;
import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.dao.ProduitDAO;
import com.eseo.steevejobs.dao.TiersDAO;
import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;
import com.eseo.steevejobs.service.DocumentService;
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
import java.util.ResourceBundle;

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

    private final TiersDAO tiersDAO = new TiersDAO();
    private final ProduitDAO produitDAO = new ProduitDAO();
    private final ComposerDAO composerDAO = new ComposerDAO();
    private final DocumentService documentService = new DocumentService(new DocumentDAO());

    private final ObservableList<Composer> lignes = FXCollections.observableArrayList();
    private Document documentModification;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerComboBox();
        configurerTableLignes();
        chargerDonnees();
        tableLignes.setItems(lignes);
    }

    public void setDocument(Document document) {
        this.documentModification = document;

        comboType.setValue(document.getType());
        comboTiers.setValue(document.getTiers());
        datePicker.setValue(document.getDate().toLocalDate());
        comboStatut.setValue(document.getStatut());

        try {
            lignes.setAll(composerDAO.findByDocumentId(document.getId()));
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

        // Colonne Quantité avec unité (kg ou unité(s))
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
            comboTiers.setItems(FXCollections.observableArrayList(tiersDAO.findAll()));
            comboProduit.setItems(FXCollections.observableArrayList(produitDAO.findAllActive()));
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

            // Vérification selon le type de produit (poids vs unité)
            boolean estProduitPoids = produit.getPoid() != null && produit.getPoid().compareTo(BigDecimal.ZERO) > 0;

            if (!estProduitPoids) {
                // Vente à l'unité - la quantité doit être un entier
                if (quantite.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                    afficherErreur("La quantité doit être un nombre entier pour ce produit (unités)");
                    return;
                }
                if (quantite.compareTo(BigDecimal.ZERO) <= 0) {
                    afficherErreur("La quantité doit être supérieure à 0");
                    return;
                }
            } else {
                // Vente au poids - quantité positive
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

        try {
            documentService.modifierDocument(documentModification);

            // Mettre à jour les lignes
            composerDAO.deleteByDocumentId(documentModification.getId());
            for (Composer ligne : lignes) {
                ligne.setIdDocument(documentModification.getId());
                composerDAO.createLigne(ligne);
            }

            afficherSucces("Document modifié !");
            fermer();
        } catch (Exception e) {
            afficherErreur("Erreur modification : " + e.getMessage());
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
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

    /** Liste déroulante du type de document. */
    @FXML private ComboBox<DocumentType> comboType;
    /** Liste déroulante du tiers associé. */
    @FXML private ComboBox<Tiers> comboTiers;
    /** Sélecteur de date du document. */
    @FXML private DatePicker datePicker;
    /** Liste déroulante du statut du document. */
    @FXML private ComboBox<DocumentStatut> comboStatut;
    /** Liste déroulante du produit à ajouter. */
    @FXML private ComboBox<Produit> comboProduit;
    /** Champ de saisie de la quantité. */
    @FXML private TextField txtQuantite;
    /** Champ de saisie du prix de vente. */
    @FXML private TextField txtPrixVente;
    /** Bouton d'ajout d'une ligne au document. */
    @FXML private Button btnAjouterLigne;
    /** Tableau des lignes du document. */
    @FXML private TableView<Composer> tableLignes;
    /** Colonnes lignes : {@code colProduit}, {@code colQuantite}, {@code colPrixUnitaire}, {@code colTotalLigne}. */
    @FXML private TableColumn<Composer, String> colProduit, colQuantite, colPrixUnitaire, colTotalLigne;
    /** Colonne des actions sur chaque ligne. */
    @FXML private TableColumn<Composer, Void> colActions;
    /** Labels d'affichage des totaux HT, TVA et TTC. */
    @FXML private Label lblTotalHT, lblTVA, lblTotalTTC;
    /** Boutons d'annulation et de modification du document. */
    @FXML private Button btnAnnuler, btnModifier;

    /** Service d'accès aux tiers. */
    private final TiersService tiersService = new TiersService();
    /** Service d'accès aux produits. */
    private final ProduitService produitService = new ProduitService();
    /** Service de gestion des documents. */
    private final DocumentService documentService = new DocumentService();

    /** Lignes composant le document en cours de modification. */
    private final ObservableList<Composer> lignes = FXCollections.observableArrayList();
    /** Document chargé pour modification. */
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

    /**
     * Configure les listes déroulantes et le remplissage automatique du prix produit.
     */
    private void configurerComboBox() {
        comboType.setItems(FXCollections.observableArrayList(DocumentType.values()));
        comboStatut.setItems(FXCollections.observableArrayList(DocumentStatut.values()));

        comboTiers.setConverter(new StringConverter<>() {
            /** @param t tiers affiché dans la liste */
            @Override public String toString(Tiers t) { return t == null ? "" : t.getNom() + (t.getPrenom() != null ? " " + t.getPrenom() : ""); }
            /** @param s saisie utilisateur (non convertie) */
            @Override public Tiers fromString(String s) { return null; }
        });

        comboProduit.setConverter(new StringConverter<>() {
            /** @param p produit affiché dans la liste */
            @Override public String toString(Produit p) { return p == null ? "" : p.getNom(); }
            /** @param s saisie utilisateur (non convertie) */
            @Override public Produit fromString(String s) { return null; }
        });

        comboProduit.setOnAction(e -> {
            Produit p = comboProduit.getValue();
            if (p != null && p.getPrix() != null) {
                txtPrixVente.setText(p.getPrix().toString());
            }
        });
    }

    /**
     * Configure les colonnes du tableau des lignes et le bouton de suppression.
     */
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
            /**
             * Affiche le bouton de suppression de ligne ou une cellule vide.
             *
             * @param item non utilisé
             * @param empty {@code true} si la ligne est hors plage
             */
            @Override protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : btnSuppr);
            }
        });
    }

    /**
     * Charge la liste des tiers et des produits actifs depuis la base.
     */
    private void chargerDonnees() {
        try {
            comboTiers.setItems(FXCollections.observableArrayList(tiersService.findAll()));
            comboProduit.setItems(FXCollections.observableArrayList(produitService.findAllActive()));
        } catch (SQLException e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    /**
     * Ajoute une ligne produit au document en cours de modification.
     * Liaison FXML : {@code btnAjouterLigne}.
     */
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

    /**
     * Recalcule et affiche les totaux HT, TVA et TTC du document.
     */
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

    /**
     * Enregistre les modifications du document et régénère le PDF sur le NAS.
     * Liaison FXML : {@code btnModifier}.
     */
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

    /**
     * Ferme la fenêtre de modification.
     * Liaison FXML : {@code btnAnnuler}.
     */
    @FXML
    private void fermer() {
        ((Stage) btnAnnuler.getScene().getWindow()).close();
    }

    /**
     * Affiche une boîte de dialogue d'erreur.
     *
     * @param msg message à afficher
     */
    private void afficherErreur(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    /**
     * Affiche une boîte de dialogue de succès.
     *
     * @param msg message à afficher
     */
    private void afficherSucces(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}

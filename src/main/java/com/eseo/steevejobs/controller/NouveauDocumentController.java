package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.*;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;
import com.eseo.steevejobs.service.DocumentService;
import com.eseo.steevejobs.service.ProduitService;
import com.eseo.steevejobs.service.SessionService;
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
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur FXML du formulaire de création de document commercial.
 * Liaisons FXML : combos, tableau des lignes, totaux et boutons créer/annuler.
 */
public class NouveauDocumentController implements Initializable {

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
    /** Totaux : {@code lblTotalHT}, {@code lblTVA}, {@code lblTotalTTC}. */
    @FXML private Label lblTotalHT, lblTVA, lblTotalTTC;
    /** Boutons {@code btnAnnuler} et {@code btnCreer}. */
    @FXML private Button btnAnnuler, btnCreer;

    /** Service d'accès aux tiers. */
    private final TiersService tiersService = new TiersService();
    /** Service d'accès aux produits. */
    private final ProduitService produitService = new ProduitService();
    /** Service de gestion des documents. */
    private final DocumentService documentService = new DocumentService();
    /** Lignes composant le nouveau document. */
    private final ObservableList<Composer> lignes = FXCollections.observableArrayList();
    /** Utilisateur connecté lors de l'initialisation. */
    private User user;

    /**
     * Initialise combos, tableau et charge les données de référence.
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
        updateTotaux();
        user = SessionService.getUtilisateurConnecte();
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
     * Ajoute une ligne produit au nouveau document.
     * Liaison FXML : {@code btnAjouterLigne}.
     */
    @FXML
    private void ajouterLigne() {
        Produit produit = comboProduit.getValue();
        String quantiteStr = txtQuantite.getText();
        String prixStr = txtPrixVente.getText();

        if (produit == null || quantiteStr.isEmpty() || prixStr.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs produit");
            return;
        }

        try {
            BigDecimal quantite = new BigDecimal(quantiteStr);
            BigDecimal prixVente = new BigDecimal(prixStr);

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

            Composer ligne = new Composer(0, produit, quantite, prixVente);
            lignes.add(ligne);

            txtQuantite.clear();
            txtPrixVente.clear();
            comboProduit.setValue(null);

            updateTotaux();
        } catch (NumberFormatException ex) {
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
     * Crée le document en base et exporte le PDF sur le NAS.
     * Liaison FXML : {@code btnCreer}.
     */
    @FXML
    private void creerDocument() {
        if (comboType.getValue() == null || comboTiers.getValue() == null
                || datePicker.getValue() == null || comboStatut.getValue() == null) {
            afficherErreur("Tous les champs sont obligatoires");
            return;
        }

        if (lignes.isEmpty()) {
            afficherErreur("Vous devez ajouter au moins un produit à ce document.");
            return;
        }
        btnCreer.setDisable(true);
        btnAnnuler.setDisable(true);

        BigDecimal totalHT = BigDecimal.ZERO;
        for (Composer ligne : lignes) {
            totalHT = totalHT.add(ligne.getPrixVente().multiply(ligne.getQuantite()));
        }
        Document doc = new Document(0, comboType.getValue(), datePicker.getValue().atStartOfDay(),
                totalHT, totalHT.multiply(new BigDecimal("1.20")), comboStatut.getValue(),
                "", comboTiers.getValue(), user);

        try {
            documentService.ajouterDocument(doc, lignes);

            CompletableFuture.supplyAsync(() -> {
                try {
                    return documentService.exporterPdf(doc.getId());
                } catch (SQLException e) {
                    throw new RuntimeException("Erreur de l'exportation PDF/NAS : " + e.getMessage(), e);
                }
            }).thenAcceptAsync(urlPdf -> {
                Platform.runLater(() -> {
                    afficherSucces("Document généré et sauvegardé sur le NAS avec succès !");
                    fermer();
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    ex.printStackTrace();
                    afficherErreur("Document sauvegardé, mais échec du transfert NAS : " + ex.getCause().getMessage());
                    btnCreer.setDisable(false);
                    btnAnnuler.setDisable(false);
                });
                return null;
            });

        } catch (Exception e) {
            afficherErreur("Erreur critique (Création BDD) : " + e.getMessage());
            btnCreer.setDisable(false);
            btnAnnuler.setDisable(false);
        }
    }

    /**
     * Ferme la fenêtre de création.
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
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");
        alert.showAndWait();
    }

    /**
     * Affiche une boîte de dialogue de succès.
     *
     * @param msg message à afficher
     */
    private void afficherSucces(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");
        alert.showAndWait();
    }

    /** Utilisateur créateur du document, défini explicitement. */
    private User utilisateurConnecte;

    /**
     * Définit l'utilisateur créateur du document (prioritaire sur la session).
     *
     * @param user utilisateur connecté
     */
    public void setUtilisateurConnecte(User user) {
        this.utilisateurConnecte = user;
    }
}
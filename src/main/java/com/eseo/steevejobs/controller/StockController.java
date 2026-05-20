package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.service.ProduitService;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

public class StockController {

    // ── Barre du haut ──────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label     labelNbProduits;

    // ── Tableau ────────────────────────────────────────────────────
    @FXML private TableView<Produit>           tableProduits;
    @FXML private TableColumn<Produit, Number> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colCategorie;
    @FXML private TableColumn<Produit, String> colReference;
    @FXML private TableColumn<Produit, Number> colQuantite;
    @FXML private TableColumn<Produit, String> colStatut;

    // ── Fiche produit ──────────────────────────────────────────────
    @FXML private Label  ficheNom;
    @FXML private Label  fichePrix;
    @FXML private Label  ficheTva;
    @FXML private Label  ficheQte;
    @FXML private Label  fichePoids;
    @FXML private Label  ficheSeuilAlerte;
    @FXML private Label  ficheStatut;
    @FXML private Button btnFicheEntree;
    @FXML private Button btnFicheSortie;
    @FXML private Button btnFicheAjuster;

    // ── Données ────────────────────────────────────────────────────
    private final ProduitService          produitService     = new ProduitService();
    private final ObservableList<Produit> data               = FXCollections.observableArrayList();
    private       Produit                 produitSelectionne = null;

    // ──────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {

        tableProduits.getStyleClass().add("stock-table");

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colCategorie.setCellValueFactory(c -> new SimpleStringProperty("—"));
        colReference.setCellValueFactory(c -> new SimpleStringProperty("—"));

        colQuantite.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantite()));
        colQuantite.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number val, boolean empty) {
                super.updateItem(val, empty);
                if (empty) { setText(null); return; }
                Produit p = getTableView().getItems().get(getIndex());
                setText(p.getPoid() != null
                        ? p.getPoid() + " kg"
                        : (val == null ? "0" : val.toString()));
            }
        });

        colStatut.setCellValueFactory(c -> new SimpleStringProperty(calculerStatut(c.getValue())));
        colStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { setGraphic(null); return; }
                badge.setText(statut);
                badge.getStyleClass().setAll(cssBadge(statut));
                setGraphic(badge);
                setText(null);
            }
        });

        tableProduits.setItems(data);

        tableProduits.getSelectionModel().selectedItemProperty().addListener(
                (obs, ancien, nouveau) -> afficherFiche(nouveau));

        searchField.setOnAction(e -> onSearch());

        rafraichirFiche(null);
        refreshTable();
    }

    // ─────────────────────────────────────────────────────────────
    // Recherche temps réel
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void onSearchRealTime(KeyEvent event) {
        onSearch();
    }

    // ─────────────────────────────────────────────────────────────
    // Statut
    // ─────────────────────────────────────────────────────────────

    private String calculerStatut(Produit p) {
        if (!p.isActif()) return "Inactif";
        int seuil = p.getSeuilAlerte();
        if (p.getPoid() != null) {
            BigDecimal poids = p.getPoid();
            if (poids.compareTo(BigDecimal.ZERO) <= 0)          return "Rupture";
            if (poids.compareTo(new BigDecimal(seuil)) <= 0)    return "A recommander";
            return "En stock";
        } else {
            int qte = p.getQuantite();
            if (qte <= 0)     return "Rupture";
            if (qte <= seuil) return "A recommander";
            return "En stock";
        }
    }

    private String cssBadge(String statut) {
        return switch (statut) {
            case "En stock"      -> "badge-en-stock";
            case "A recommander" -> "badge-a-recommander";
            default              -> "badge-rupture";
        };
    }

    private String couleurStatut(String statut) {
        return switch (statut) {
            case "En stock"      -> "#27ae60";
            case "A recommander" -> "#f39c12";
            default              -> "#e74c3c";
        };
    }

    // ─────────────────────────────────────────────────────────────
    // Fiche produit
    // ─────────────────────────────────────────────────────────────

    private void afficherFiche(Produit p) {
        produitSelectionne = p;
        rafraichirFiche(p);
    }

    private void rafraichirFiche(Produit p) {
        boolean actif = (p != null);
        btnFicheEntree.setDisable(!actif);
        btnFicheSortie.setDisable(!actif);
        btnFicheAjuster.setDisable(!actif);

        if (!actif) {
            ficheNom.setText("Aucun produit sélectionné");
            fichePrix.setText("—");
            ficheTva.setText("—");
            ficheQte.setText("—");
            fichePoids.setText("—");
            ficheSeuilAlerte.setText("—");
            ficheStatut.setText("—");
            ficheStatut.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2d3450;");
            return;
        }

        ficheNom.setText(p.getNom());
        fichePrix.setText(p.getPrix() != null ? p.getPrix() + " €" : "—");
        ficheTva.setText(p.getTauxTva() != null ? p.getTauxTva() + " %" : "—");
        ficheQte.setText(p.getPoid() != null ? "—" : String.valueOf(p.getQuantite()));
        fichePoids.setText(p.getPoid() != null ? p.getPoid() + " kg" : "—");
        ficheSeuilAlerte.setText(String.valueOf(p.getSeuilAlerte()));

        String statut = calculerStatut(p);
        ficheStatut.setText(statut);
        ficheStatut.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
                + couleurStatut(statut) + ";");
    }

    @FXML private void onFicheEntree()  { if (produitSelectionne != null) entreeDepuisLigne(produitSelectionne); }
    @FXML private void onFicheSortie()  { if (produitSelectionne != null) sortieDepuisLigne(produitSelectionne); }
    @FXML private void onFicheAjuster() { if (produitSelectionne != null) ajusterDepuisLigne(produitSelectionne); }

    // ─────────────────────────────────────────────────────────────
    // Popup Nouveau Produit
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void onNouveauProduit() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Nouveau Produit");
        popup.setResizable(true);
        popup.setMinWidth(380);
        popup.setMinHeight(400);

        // ── Champs ────────────────────────────────────────────────
        String styleChamp = "-fx-background-radius: 25; -fx-border-radius: 25; " +
                "-fx-border-color: #000000; -fx-background-color: transparent; " +
                "-fx-pref-height: 38; -fx-font-family: Arial;";

        TextField champNom   = new TextField();
        champNom.setPromptText("Ex : Briques, Câble électrique...");
        champNom.setStyle(styleChamp);

        TextField champPrix  = new TextField();
        champPrix.setPromptText("Ex : 29.90");
        champPrix.setStyle(styleChamp);

        TextField champTva   = new TextField();
        champTva.setPromptText("Ex : 20");
        champTva.setStyle(styleChamp);

        TextField champPoids = new TextField();
        champPoids.setPromptText("Laisser vide si produit unitaire");
        champPoids.setStyle(styleChamp);

        TextField champSeuil = new TextField();
        champSeuil.setPromptText("Ex : 5");
        champSeuil.setStyle(styleChamp);

        // Labels d'erreur
        Label errNom   = erreurLabel();
        Label errPrix  = erreurLabel();
        Label errTva   = erreurLabel();
        Label errPoids = erreurLabel();
        Label errSeuil = erreurLabel();

        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color: #82A9F1;");
        Label titrePopup = new Label("Nouveau Produit");
        titrePopup.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; " +
                "-fx-font-family: 'Comic Sans MS'; -fx-text-fill: white;");
        header.getChildren().add(titrePopup);

        // ── Carte formulaire ──────────────────────────────────────
        VBox carte = new VBox(14);
        carte.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #CCCCCC; -fx-border-radius: 10; -fx-padding: 20;");

        carte.getChildren().addAll(
                labelChamp("Nom du produit *"),  champNom,   errNom,
                labelChamp("Prix HT (€)"),        champPrix,  errPrix,
                labelChamp("Taux TVA (%)"),        champTva,   errTva,
                labelChamp("Poids initial (kg) — laisser vide si produit unitaire"),
                champPoids, errPoids,
                labelChamp("Seuil d'alerte stock bas"), champSeuil, errSeuil
        );

        // ── Boutons ───────────────────────────────────────────────
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: white; -fx-text-fill: #5584D5; " +
                "-fx-border-color: #5584D5; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-padding: 9 20; -fx-cursor: hand;");

        Button btnCreer = new Button("Créer le produit");
        btnCreer.setStyle("-fx-background-color: #5584D5; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-padding: 9 20; -fx-cursor: hand;");

        HBox boutons = new HBox(12, btnAnnuler, btnCreer);
        boutons.setAlignment(Pos.CENTER_RIGHT);

        // ── Contenu scrollable ────────────────────────────────────
        VBox contenu = new VBox(16, carte, boutons);
        contenu.setStyle("-fx-background-color: #DDE8FF;");
        contenu.setPadding(new Insets(20));

        ScrollPane scroll = new ScrollPane(contenu);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #DDE8FF; -fx-background: #DDE8FF; " +
                "-fx-border-color: transparent;");
        scroll.getStyleClass().add("rounded-scroll-pane");

        VBox root = new VBox(header, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setStyle("-fx-background-color: #DDE8FF;");

        // ── Actions ───────────────────────────────────────────────
        btnAnnuler.setOnAction(e -> popup.close());

        btnCreer.setOnAction(e -> {
            // Reset erreurs
            errNom.setText(""); errPrix.setText(""); errTva.setText("");
            errPoids.setText(""); errSeuil.setText("");

            boolean valide = true;

            // Validation nom
            String nom = champNom.getText().trim();
            if (nom.isEmpty()) {
                errNom.setText("Le nom est obligatoire.");
                valide = false;
            }

            // Validation prix
            BigDecimal prix = BigDecimal.ZERO;
            try {
                String txtPrix = champPrix.getText().trim().replace(',', '.');
                if (!txtPrix.isEmpty()) {
                    prix = new BigDecimal(txtPrix);
                    if (prix.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                errPrix.setText("Prix invalide (ex : 29.90).");
                valide = false;
            }

            // Validation TVA
            BigDecimal tva = BigDecimal.ZERO;
            try {
                String txtTva = champTva.getText().trim().replace(',', '.');
                if (!txtTva.isEmpty()) {
                    tva = new BigDecimal(txtTva);
                    if (tva.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                errTva.setText("TVA invalide (ex : 20).");
                valide = false;
            }

            // Validation poids (optionnel)
            BigDecimal poids = null;
            String txtPoids = champPoids.getText().trim().replace(',', '.');
            if (!txtPoids.isEmpty()) {
                try {
                    poids = new BigDecimal(txtPoids);
                    if (poids.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errPoids.setText("Poids invalide (ex : 1000.00).");
                    valide = false;
                }
            }

            // Validation seuil
            int seuil = 0;
            try {
                String txtSeuil = champSeuil.getText().trim();
                if (!txtSeuil.isEmpty()) {
                    seuil = Integer.parseInt(txtSeuil);
                    if (seuil < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                errSeuil.setText("Seuil invalide (entier positif).");
                valide = false;
            }

            if (!valide) return;

            // Création du produit
            Produit nouveau = new Produit(0, nom, prix, tva, 0, poids, true, seuil);

            try {
                produitService.ajouterProduit(nouveau);
                popup.close();
                refreshTable();
            } catch (Exception ex) {
                errNom.setText("Erreur : " + ex.getMessage());
            }
        });

        // ── Affichage ─────────────────────────────────────────────
        Scene scene = new Scene(root, 460, 580);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ── Helpers UI ────────────────────────────────────────────────

    private Label labelChamp(String texte) {
        Label l = new Label(texte);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e2545;");
        return l;
    }

    private Label erreurLabel() {
        Label l = new Label("");
        l.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        return l;
    }

    // ─────────────────────────────────────────────────────────────
    // Actions stock
    // ─────────────────────────────────────────────────────────────

    private void entreeDepuisLigne(Produit p) {
        if (p == null) return;
        try {
            if (p.getPoid() != null) {
                BigDecimal val = askPositiveBigDecimal("Entrée (vrac)", "Poids à ajouter (ex: 2.5) :");
                if (val == null) return;
                produitService.mettreAJourStockAuto(p.getId(), null, val);
            } else {
                Integer qte = askPositiveInt("Entrée", "Quantité à ajouter :");
                if (qte == null) return;
                produitService.mettreAJourStockAuto(p.getId(), qte, null);
            }
            refreshTable();
            rechargerFicheApresAction(p.getId());
        } catch (Exception e) { showError("Entrée impossible", e.getMessage()); }
    }

    private void sortieDepuisLigne(Produit p) {
        if (p == null) return;
        try {
            if (p.getPoid() != null) {
                BigDecimal val = askPositiveBigDecimal("Sortie (vrac)", "Poids à retirer (ex: 2.5) :");
                if (val == null) return;
                produitService.mettreAJourStockAuto(p.getId(), null, val.negate());
            } else {
                Integer qte = askPositiveInt("Sortie", "Quantité à retirer :");
                if (qte == null) return;
                produitService.mettreAJourStockAuto(p.getId(), -qte, null);
            }
            refreshTable();
            rechargerFicheApresAction(p.getId());
        } catch (Exception e) { showError("Sortie impossible", e.getMessage()); }
    }

    private void ajusterDepuisLigne(Produit p) {
        if (p == null) return;
        try {
            if (p.getPoid() != null) {
                BigDecimal nv = askPositiveBigDecimal("Ajuster (vrac)", "Nouveau poids (ex: 2.5) :");
                if (nv == null) return;
                produitService.mettreAJourStockAuto(p.getId(), null, nv.subtract(p.getPoid()));
            } else {
                Integer nv = askPositiveInt("Ajuster", "Nouvelle quantité :");
                if (nv == null) return;
                produitService.mettreAJourStockAuto(p.getId(), nv - p.getQuantite(), null);
            }
            refreshTable();
            rechargerFicheApresAction(p.getId());
        } catch (Exception e) { showError("Ajustement impossible", e.getMessage()); }
    }

    private void rechargerFicheApresAction(int id) {
        if (produitSelectionne != null && produitSelectionne.getId() == id) {
            try { rafraichirFiche(produitService.obtenirProduitParId(id)); }
            catch (SQLException ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Barre du haut
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void onRefresh() {
        searchField.clear();
        refreshTable();
    }

    private void onSearch() {
        String term = searchField.getText() == null ? "" : searchField.getText().trim();
        if (term.isEmpty()) { refreshTable(); return; }
        try { data.setAll(produitService.rechercherProduitsParNom(term)); }
        catch (SQLException e) { showError("Erreur SQL", e.getMessage()); }
        updateCompteur();
    }

    // ─────────────────────────────────────────────────────────────
    // Chargement
    // ─────────────────────────────────────────────────────────────

    private void refreshTable() {
        try { data.setAll(produitService.obtenirTousLesProduits()); }
        catch (SQLException e) { showError("Erreur SQL", e.getMessage()); }
        updateCompteur();
    }

    private void updateCompteur() {
        if (labelNbProduits != null)
            labelNbProduits.setText(data.size() + " produit(s)");
    }

    // ─────────────────────────────────────────────────────────────
    // Dialogs
    // ─────────────────────────────────────────────────────────────

    private Integer askPositiveInt(String title, String msg) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle(title); d.setHeaderText(null); d.setContentText(msg);
        Optional<String> r = d.showAndWait();
        if (r.isEmpty()) return null;
        try {
            int v = Integer.parseInt(r.get().trim());
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException ex) {
            showError("Valeur invalide", "Entre un entier positif."); return null;
        }
    }

    private BigDecimal askPositiveBigDecimal(String title, String msg) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle(title); d.setHeaderText(null); d.setContentText(msg);
        Optional<String> r = d.showAndWait();
        if (r.isEmpty()) return null;
        try {
            BigDecimal v = new BigDecimal(r.get().trim().replace(',', '.'));
            if (v.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            return v;
        } catch (Exception ex) {
            showError("Valeur invalide", "Entre un nombre positif (ex: 2.5)."); return null;
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
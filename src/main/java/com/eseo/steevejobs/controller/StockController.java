package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.service.ProduitService;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

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

        // Colonnes
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colCategorie.setCellValueFactory(c -> new SimpleStringProperty("—"));
        colReference.setCellValueFactory(c -> new SimpleStringProperty("—"));

        // Quantité : poids si vrac, sinon quantité unitaire
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

        // Badge statut — basé sur seuilAlerte du produit
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

        // Sélection → fiche produit
        tableProduits.getSelectionModel().selectedItemProperty().addListener(
                (obs, ancien, nouveau) -> afficherFiche(nouveau));

        // Recherche sur touche Entrée
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
    // Calcul statut — basé sur seuilAlerte propre au produit
    // ─────────────────────────────────────────────────────────────

    private String calculerStatut(Produit p) {
        if (!p.isActif()) return "Inactif";

        int seuil = p.getSeuilAlerte(); // seuil défini sur le produit lui-même

        if (p.getPoid() != null) {
            BigDecimal poids = p.getPoid();
            if (poids.compareTo(BigDecimal.ZERO) <= 0)
                return "Rupture";
            if (poids.compareTo(new BigDecimal(seuil)) <= 0)
                return "A recommander";
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
            ficheStatut.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2d3450;");
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
        ficheStatut.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
                        + couleurStatut(statut) + ";");
    }

    @FXML private void onFicheEntree()  { if (produitSelectionne != null) entreeDepuisLigne(produitSelectionne); }
    @FXML private void onFicheSortie()  { if (produitSelectionne != null) sortieDepuisLigne(produitSelectionne); }
    @FXML private void onFicheAjuster() { if (produitSelectionne != null) ajusterDepuisLigne(produitSelectionne); }

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
        searchField.clear();   // ← vide la barre de recherche
        refreshTable();
    }

    private void onSearch() {
        String term = searchField.getText() == null ? "" : searchField.getText().trim();
        if (term.isEmpty()) { refreshTable(); return; }
        try { data.setAll(produitService.rechercherProduitsParNom(term)); }
        catch (SQLException e) { showError("Erreur SQL", e.getMessage()); }
        updateCompteur();
    }

    @FXML private void onNouveauProduit() {
        showInfo("Nouveau produit", "Fonctionnalité à implémenter.");
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
package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.service.ProduitService;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class StockController {

    @FXML private TextField searchField;
    @FXML private Spinner<Integer> lowStockSpinner;

    @FXML private TableView<Produit> tableProduits;
    @FXML private TableColumn<Produit, Number> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, Number> colQuantite;
    @FXML private TableColumn<Produit, BigDecimal> colPoids;
    @FXML private TableColumn<Produit, BigDecimal> colPrix;
    @FXML private TableColumn<Produit, BigDecimal> colTva;
    @FXML private TableColumn<Produit, Boolean> colActif;

    // ✅ Colonne actions (boutons dans la ligne)
    @FXML private TableColumn<Produit, Void> colActions;

    private final ProduitService produitService = new ProduitService();
    private final ObservableList<Produit> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        lowStockSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10_000, 5));

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));

        // Affichage intelligent : si produit vrac (poids != null), on affiche "-" en quantité
        colQuantite.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantite()));
        colQuantite.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) { setText(null); return; }
                Produit p = getTableView().getItems().get(getIndex());
                if (p.getPoid() != null) {
                    setText("-");
                } else {
                    setText(value == null ? "0" : value.toString());
                }
            }
        });

        // Affichage intelligent : si produit unitaire (poids == null), on affiche "-" en poids
        colPoids.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPoid()));
        colPoids.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) { setText(null); return; }
                setText(value == null ? "-" : value.toString());
            }
        });

        colPrix.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrix()));
        colTva.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTauxTva()));
        colActif.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().isActif()));

        // ✅ Boutons "Entrée (+) / Sortie (-) / Ajusté (=)" dans chaque ligne
        installerBoutonsParLigne();

        tableProduits.setItems(data);
        refreshTable();
    }

    /**
     * ✅ Ajoute les boutons dans chaque ligne (colActions)
     * - Sans couleurs forcées
     * - Avec libellés complets : Entrée (+), Sortie (-), Ajusté (=)
     */
    private void installerBoutonsParLigne() {
        colActions.setCellFactory(col -> new TableCell<>() {

            private final Button btnEntree = new Button("Entrée (+)");
            private final Button btnSortie = new Button("Sortie (-)");
            private final Button btnAjuster = new Button("Ajusté (=)");

            {
                // ✅ Évite le focus au clic (plus agréable en tableau)
                btnEntree.setFocusTraversable(false);
                btnSortie.setFocusTraversable(false);
                btnAjuster.setFocusTraversable(false);

                // ✅ Largeurs homogènes (évite l’effet "carré")
                btnEntree.setPrefWidth(95);
                btnSortie.setPrefWidth(95);
                btnAjuster.setPrefWidth(95);

                // ✅ Hauteur cohérente
                btnEntree.setPrefHeight(26);
                btnSortie.setPrefHeight(26);
                btnAjuster.setPrefHeight(26);

                // ✅ Actions : on réutilise tes méthodes existantes
                btnEntree.setOnAction(e -> entreeDepuisLigne(getProduitLigne()));
                btnSortie.setOnAction(e -> sortieDepuisLigne(getProduitLigne()));
                btnAjuster.setOnAction(e -> ajusterDepuisLigne(getProduitLigne()));
            }

            private Produit getProduitLigne() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(6, btnEntree, btnSortie, btnAjuster));
                }
            }
        });
    }

    // ----------------------------
    // Actions depuis la ligne
    // ----------------------------

    private void entreeDepuisLigne(Produit p) {
        if (p == null) return;
        try {
            if (p.getPoid() != null) {
                // VRAC -> entrée poids
                BigDecimal val = askPositiveBigDecimal("Entrée (vrac)", "Poids à ajouter (ex: 2.5) :");
                if (val == null) return;
                produitService.mettreAJourStockAuto(p.getId(), null, val);
            } else {
                // UNITAIRE -> entrée quantité
                Integer qte = askPositiveInt("Entrée", "Quantité à ajouter :");
                if (qte == null) return;
                produitService.mettreAJourStockAuto(p.getId(), qte, null);
            }
            refreshTable();
        } catch (Exception e) {
            showError("Entrée impossible", e.getMessage());
        }
    }

    private void sortieDepuisLigne(Produit p) {
        if (p == null) return;
        try {
            if (p.getPoid() != null) {
                // VRAC -> sortie poids
                BigDecimal val = askPositiveBigDecimal("Sortie (vrac)", "Poids à retirer (ex: 2.5) :");
                if (val == null) return;
                produitService.mettreAJourStockAuto(p.getId(), null, val.negate());
            } else {
                // UNITAIRE -> sortie quantité
                Integer qte = askPositiveInt("Sortie", "Quantité à retirer :");
                if (qte == null) return;
                produitService.mettreAJourStockAuto(p.getId(), -qte, null);
            }
            refreshTable();
        } catch (Exception e) {
            showError("Sortie impossible", e.getMessage());
        }
    }

    private void ajusterDepuisLigne(Produit p) {
        if (p == null) return;
        try {
            if (p.getPoid() != null) {
                // VRAC -> ajuster poids (valeur finale)
                BigDecimal newWeight = askPositiveBigDecimal("Ajuster (vrac)", "Nouveau poids (ex: 2.5) :");
                if (newWeight == null) return;

                BigDecimal variationPoids = newWeight.subtract(p.getPoid());
                produitService.mettreAJourStockAuto(p.getId(), null, variationPoids);
            } else {
                // UNITAIRE -> ajuster quantité (valeur finale)
                Integer newValue = askPositiveInt("Ajuster", "Nouvelle quantité :");
                if (newValue == null) return;

                int variationQuantite = newValue - p.getQuantite();
                produitService.mettreAJourStockAuto(p.getId(), variationQuantite, null);
            }
            refreshTable();
        } catch (Exception e) {
            showError("Ajustement impossible", e.getMessage());
        }
    }

    // ----------------------------
    // Barre du haut : recherche / filtre / refresh
    // ----------------------------

    @FXML
    private void onRefresh() {
        refreshTable();
    }

    @FXML
    private void onSearch() {
        String term = (searchField.getText() == null) ? "" : searchField.getText().trim();
        if (term.isEmpty()) {
            refreshTable();
            return;
        }
        try {
            data.setAll(produitService.rechercherProduitsParNom(term));
        } catch (SQLException e) {
            showError("Erreur SQL", e.getMessage());
        }
    }

    @FXML
    private void onLowStock() {
        int seuil = lowStockSpinner.getValue();
        try {
            data.setAll(produitService.obtenirProduitsStockBas(seuil));
        } catch (SQLException e) {
            showError("Erreur SQL", e.getMessage());
        }
    }

    // ----------------------------
    // Chargement table
    // ----------------------------

    private void refreshTable() {
        try {
            List<Produit> produits = produitService.obtenirTousLesProduits();
            data.setAll(produits);
        } catch (SQLException e) {
            showError("Erreur SQL", e.getMessage());
        }
    }

    // ----------------------------
    // Dialogs
    // ----------------------------

    private Integer askPositiveInt(String title, String msg) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(msg);

        Optional<String> r = d.showAndWait();
        if (r.isEmpty()) return null;

        try {
            int v = Integer.parseInt(r.get().trim());
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException ex) {
            showError("Valeur invalide", "Entre un entier positif.");
            return null;
        }
    }

    private BigDecimal askPositiveBigDecimal(String title, String msg) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(msg);

        Optional<String> r = d.showAndWait();
        if (r.isEmpty()) return null;

        try {
            BigDecimal v = new BigDecimal(r.get().trim().replace(',', '.'));
            if (v.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            return v;
        } catch (Exception ex) {
            showError("Valeur invalide", "Entre un nombre positif (ex: 2.5).");
            return null;
        }
    }

    // ----------------------------
    // Alerts
    // ----------------------------

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
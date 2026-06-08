package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Produit;
import com.eseo.steevejobs.service.ProduitService;

import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Contrôleur FXML de gestion du stock produits.
 * Liaisons FXML : {@code tableProduits}, {@code searchField}, fiche produit et actions stock.
 */
public class StockController {

    // ──────────────────────────────────────────────────────────────
    // Barre du haut
    // ──────────────────────────────────────────────────────────────
    /** Champ de recherche de produits. */
    @FXML private TextField searchField;
    /** Label affichant le nombre de produits listés. */
    @FXML private Label labelNbProduits;

    // ──────────────────────────────────────────────────────────────
    // Tableau
    // ──────────────────────────────────────────────────────────────
    /** Tableau listant les produits en stock. */
    @FXML private TableView<Produit> tableProduits;
    /** Colonne identifiant du produit. */
    @FXML private TableColumn<Produit, Number> colId;
    /** Colonne nom du produit. */
    @FXML private TableColumn<Produit, String> colNom;
    /** Colonne quantité ou poids du produit. */
    @FXML private TableColumn<Produit, Number> colQuantite;
    /** Colonne statut stock du produit. */
    @FXML private TableColumn<Produit, String> colStatut;

    // ──────────────────────────────────────────────────────────────
    // Fiche produit
    // ──────────────────────────────────────────────────────────────
    /** Label affichant le nom du produit sélectionné. */
    @FXML private Label ficheNom;
    /** Label affichant le prix du produit sélectionné. */
    @FXML private Label fichePrix;
    /** Label affichant la TVA du produit sélectionné. */
    @FXML private Label ficheTva;
    /** Label affichant la quantité du produit sélectionné. */
    @FXML private Label ficheQte;
    /** Label affichant le poids du produit sélectionné. */
    @FXML private Label fichePoids;
    /** Label affichant le seuil d'alerte du produit sélectionné. */
    @FXML private Label ficheSeuilAlerte;
    /** Label affichant le statut du produit sélectionné. */
    @FXML private Label ficheStatut;

    /** Bouton d'entrée de stock depuis la fiche produit. */
    @FXML private Button btnFicheEntree;
    /** Bouton de sortie de stock depuis la fiche produit. */
    @FXML private Button btnFicheSortie;
    /** Bouton d'ajustement de stock depuis la fiche produit. */
    @FXML private Button btnFicheAjuster;
    /** Bouton de modification du produit sélectionné. */
    @FXML private Button btnFicheModifier;

    // ──────────────────────────────────────────────────────────────
    // Données
    // ──────────────────────────────────────────────────────────────
    /** Service de gestion des produits. */
    private final ProduitService produitService = new ProduitService();
    /** Liste observable des produits affichés dans le tableau. */
    private final ObservableList<Produit> data = FXCollections.observableArrayList();
    /** Produit actuellement sélectionné dans le tableau. */
    private Produit produitSelectionne = null;

    // ──────────────────────────────────────────────────────────────
    // Chargement CSS global (optionnel)
    // ──────────────────────────────────────────────────────────────
    /**
     * Applique la feuille de style globale à une scène.
     *
     * @param scene scène cible
     */
    private void applyCSS(Scene scene) {
        var url = getClass().getResource("/style/style.css");
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
    }

    // ──────────────────────────────────────────────────────────────
    // Initialisation
    // ──────────────────────────────────────────────────────────────
    /**
     * Configure le tableau, la recherche et charge la liste des produits.
     */
    @FXML
    public void initialize() {

        tableProduits.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));

        colQuantite.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantite()));
        colQuantite.setCellFactory(col -> new TableCell<>() {
            /**
             * Affiche la quantité ou le poids selon le type de produit.
             *
             * @param val valeur numérique de la cellule
             * @param empty {@code true} si la ligne est hors plage
             */
            @Override protected void updateItem(Number val, boolean empty) {
                super.updateItem(val, empty);
                if (empty) { setText(null); return; }
                Produit p = getTableView().getItems().get(getIndex());
                setText(p.getPoid() != null ? p.getPoid() + " kg" : String.valueOf(val));
            }
        });

        colStatut.setCellValueFactory(c -> new SimpleStringProperty(calculerStatut(c.getValue())));
        colStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            /**
             * Affiche un badge coloré selon le statut stock du produit.
             *
             * @param statut libellé du statut
             * @param empty {@code true} si la ligne est hors plage
             */
            @Override protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { setGraphic(null); return; }
                badge.setText(statut);
                badge.setStyle(styleBadge(statut));
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

    /**
     * Lance la recherche lors de la saisie clavier.
     * Liaison FXML : {@code searchField}.
     *
     * @param e événement clavier (non utilisé)
     */
    @FXML private void onSearchRealTime(KeyEvent e) { onSearch(); }

    /**
     * Calcule le libellé de statut stock d'un produit.
     *
     * @param p produit évalué
     * @return statut affiché (En stock, A recommander, Rupture ou Inactif)
     */
    private String calculerStatut(Produit p) {
        if (!p.isActif()) return "Inactif";
        int seuil = p.getSeuilAlerte();

        if (p.getPoid() != null) {
            BigDecimal poids = p.getPoid();
            if (poids.compareTo(BigDecimal.ZERO) <= 0) return "Rupture";
            if (poids.compareTo(new BigDecimal(seuil)) <= 0) return "A recommander";
            return "En stock";
        }

        int qte = p.getQuantite();
        if (qte <= 0) return "Rupture";
        if (qte <= seuil) return "A recommander";
        return "En stock";
    }

    /**
     * Retourne le style CSS inline d'un badge selon le statut.
     *
     * @param statut libellé du statut
     * @return chaîne de styles JavaFX
     */
    private String styleBadge(String statut) {
        return switch (statut) {
            case "En stock" -> "-fx-background-color:#d1fae5; -fx-text-fill:#065f46; -fx-padding:4 8; -fx-background-radius:6;";
            case "A recommander" -> "-fx-background-color:#fff7ed; -fx-text-fill:#92400e; -fx-padding:4 8; -fx-background-radius:6;";
            default -> "-fx-background-color:#fee2e2; -fx-text-fill:#7f1d1d; -fx-padding:4 8; -fx-background-radius:6;";
        };
    }

    /**
     * Met à jour la fiche produit lors d'une sélection dans le tableau.
     *
     * @param p produit sélectionné ou {@code null}
     */
    private void afficherFiche(Produit p) {
        produitSelectionne = p;
        rafraichirFiche(p);
    }

    /**
     * Rafraîchit le contenu de la fiche produit et l'état des boutons d'action.
     *
     * @param p produit affiché ou {@code null} pour l'état vide
     */
    private void rafraichirFiche(Produit p) {


        boolean actif = (p != null);
        btnFicheEntree.setDisable(!actif);
        btnFicheSortie.setDisable(!actif);
        btnFicheAjuster.setDisable(!actif);
        btnFicheModifier.setDisable(!actif);

        ficheStatut.setStyle("");

        if (!actif) {
            ficheNom.setText("Aucun produit sélectionné");
            fichePrix.setText("—");
            ficheTva.setText("—");
            ficheQte.setText("—");
            fichePoids.setText("—");
            ficheSeuilAlerte.setText("—");
            ficheStatut.setText("—");
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
        ficheStatut.setStyle(styleBadge(statut) + " -fx-font-weight:bold; -fx-padding:6;");
    }


    /**
     * Enregistre une entrée de stock pour le produit sélectionné.
     * Liaison FXML : {@code btnFicheEntree}.
     */
    @FXML
    private void onFicheEntree() {
        if (produitSelectionne != null)
            entreeDepuisLigne(produitSelectionne);
    }

    /**
     * Enregistre une sortie de stock pour le produit sélectionné.
     * Liaison FXML : {@code btnFicheSortie}.
     */
    @FXML
    private void onFicheSortie() {
        if (produitSelectionne != null)
            sortieDepuisLigne(produitSelectionne);
    }

    /**
     * Ajuste le stock du produit sélectionné à une valeur absolue.
     * Liaison FXML : {@code btnFicheAjuster}.
     */
    @FXML
    private void onFicheAjuster() {
        if (produitSelectionne != null)
            ajusterDepuisLigne(produitSelectionne);
    }

    /**
     * Ouvre la popup de modification du produit sélectionné.
     * Liaison FXML : {@code btnFicheModifier}.
     */
    @FXML
    private void onFicheModifier() {
        if (produitSelectionne != null)
            ouvrirPopupModifierProduit();
    }

    // ─────────────────────────────────────────────────────────────
    // Popup Nouveau Produit
    // ─────────────────────────────────────────────────────────────
    /**
     * Ouvre la popup de création d'un nouveau produit.
     * Liaison FXML : bouton nouveau produit.
     */
    @FXML
    private void onNouveauProduit() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouveau Produit");

        // Boutons
        ButtonType btnCreer = new ButtonType("Créer le produit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCreer, ButtonType.CANCEL);

        // Champs
        TextField champNom   = new TextField();
        TextField champPrix  = new TextField();
        TextField champTva   = new TextField();
        TextField champPoids = new TextField();
        TextField champSeuil = new TextField();

        champNom.setPromptText("Ex : Briques, Câble électrique...");
        champPrix.setPromptText("Ex : 29.90");
        champTva.setPromptText("Ex : 20");
        champPoids.setPromptText("Laisser vide si produit unitaire");
        champSeuil.setPromptText("Ex : 5");

        // Ajout des classes CSS
        champNom.getStyleClass().add("champform");
        champPrix.getStyleClass().add("champform");
        champTva.getStyleClass().add("champform");
        champPoids.getStyleClass().add("champform");
        champSeuil.getStyleClass().add("champform");

        Label errNom   = new Label();
        Label errPrix  = new Label();
        Label errTva   = new Label();
        Label errPoids = new Label();
        Label errSeuil = new Label();

        errNom.getStyleClass().add("erreur-champ");
        errPrix.getStyleClass().add("erreur-champ");
        errTva.getStyleClass().add("erreur-champ");
        errPoids.getStyleClass().add("erreur-champ");
        errSeuil.getStyleClass().add("erreur-champ");

        // Formulaire en GridPane
        GridPane form = new GridPane();
        form.getStyleClass().add("popup-carte");
        form.setHgap(10);
        form.setVgap(6);

        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setPercentWidth(40);

        ColumnConstraints colField = new ColumnConstraints();
        colField.setPercentWidth(60);

        form.getColumnConstraints().addAll(colLabel, colField);

        int row = 0;

        // Nom
        Label lNom = new Label("Nom du produit :");
        lNom.getStyleClass().add("label-champ");
        form.add(lNom, 0, row);
        form.add(champNom, 1, row++);
        form.add(errNom, 1, row++);

        // Prix
        Label lPrix = new Label("Prix HT (€) :");
        lPrix.getStyleClass().add("label-champ");
        form.add(lPrix, 0, row);
        form.add(champPrix, 1, row++);
        form.add(errPrix, 1, row++);

        // TVA
        Label lTva = new Label("Taux TVA (%) :");
        lTva.getStyleClass().add("label-champ");
        form.add(lTva, 0, row);
        form.add(champTva, 1, row++);
        form.add(errTva, 1, row++);

        // Poids
        Label lPoids = new Label("Poids initial (kg) :");
        lPoids.getStyleClass().add("label-champ");
        form.add(lPoids, 0, row);
        form.add(champPoids, 1, row++);
        form.add(errPoids, 1, row++);

        // Seuil
        Label lSeuil = new Label("Seuil d'alerte :");
        lSeuil.getStyleClass().add("label-champ");
        form.add(lSeuil, 0, row);
        form.add(champSeuil, 1, row++);
        form.add(errSeuil, 1, row++);

        // Conteneur principal
        VBox contenu = new VBox(10);
        contenu.getStyleClass().add("popup-contenu");
        contenu.getChildren().addAll(
                buildHeader("Nouveau Produit"),
                form
        );

        ScrollPane scroll = new ScrollPane(contenu);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("popup-root");

        dialog.getDialogPane().setContent(scroll);

        // Application du popup.css
        appliquerStyleDialog(dialog.getDialogPane());

        // Bouton OK désactivé tant que le nom est vide
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btnCreer);
        okBtn.setDisable(true);

        champNom.textProperty().addListener((obs, oldV, newV) ->
                okBtn.setDisable(newV.trim().isEmpty())
        );

        // Validation
        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == btnCreer) {

            errNom.setText(""); errPrix.setText(""); errTva.setText("");
            errPoids.setText(""); errSeuil.setText("");

            boolean valide = true;

            String nom = champNom.getText().trim();
            if (nom.isEmpty()) { errNom.setText("Le nom est obligatoire."); valide = false; }

            BigDecimal prix = BigDecimal.ZERO;
            try {
                String txt = champPrix.getText().trim().replace(',', '.');
                if (!txt.isEmpty()) prix = new BigDecimal(txt);
            } catch (Exception ex) { errPrix.setText("Prix invalide."); valide = false; }

            BigDecimal tva = BigDecimal.ZERO;
            try {
                String txt = champTva.getText().trim().replace(',', '.');
                if (!txt.isEmpty()) tva = new BigDecimal(txt);
            } catch (Exception ex) { errTva.setText("TVA invalide."); valide = false; }

            BigDecimal poids = null;
            try {
                String txt = champPoids.getText().trim().replace(',', '.');
                if (!txt.isEmpty()) poids = new BigDecimal(txt);
            } catch (Exception ex) { errPoids.setText("Poids invalide."); valide = false; }

            int seuil = 0;
            try {
                String txt = champSeuil.getText().trim();
                if (!txt.isEmpty()) seuil = Integer.parseInt(txt);
            } catch (Exception ex) { errSeuil.setText("Seuil invalide."); valide = false; }

            if (!valide) {
                afficherErreur("Veuillez corriger les champs en erreur.");
                return;
            }

            Produit nouveau = new Produit(0, nom, prix, tva, 0, poids, true, seuil);

            try {
                produitService.ajouterProduit(nouveau);
                refreshTable();
            } catch (Exception ex) {
                afficherErreur("Erreur : " + ex.getMessage());
            }
        }
    }
    // ─────────────────────────────────────────────────────────────
    // Popup Modifier Produit
    // ─────────────────────────────────────────────────────────────
    /**
     * Ouvre la popup de modification du produit actuellement sélectionné.
     */
    @FXML
    private void ouvrirPopupModifierProduit() {

        if (produitSelectionne == null) return;
        Produit p = produitSelectionne;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le produit");

        ButtonType btnEnregistrer = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnEnregistrer, ButtonType.CANCEL);

        // Champs pré-remplis
        TextField champNom   = new TextField(p.getNom());
        TextField champPrix  = new TextField(p.getPrix() != null ? p.getPrix().toPlainString() : "");
        TextField champTva   = new TextField(p.getTauxTva() != null ? p.getTauxTva().toPlainString() : "");
        TextField champPoids = new TextField(p.getPoid() != null ? p.getPoid().toPlainString() : "");
        TextField champSeuil = new TextField(String.valueOf(p.getSeuilAlerte()));

        champNom.getStyleClass().add("champform");
        champPrix.getStyleClass().add("champform");
        champTva.getStyleClass().add("champform");
        champPoids.getStyleClass().add("champform");
        champSeuil.getStyleClass().add("champform");

        Label errNom   = new Label();
        Label errPrix  = new Label();
        Label errTva   = new Label();
        Label errPoids = new Label();
        Label errSeuil = new Label();

        errNom.getStyleClass().add("erreur-champ");
        errPrix.getStyleClass().add("erreur-champ");
        errTva.getStyleClass().add("erreur-champ");
        errPoids.getStyleClass().add("erreur-champ");
        errSeuil.getStyleClass().add("erreur-champ");

        // Formulaire en GridPane
        GridPane form = new GridPane();
        form.getStyleClass().add("popup-carte");
        form.setHgap(10);
        form.setVgap(6);

        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setPercentWidth(40);

        ColumnConstraints colField = new ColumnConstraints();
        colField.setPercentWidth(60);

        form.getColumnConstraints().addAll(colLabel, colField);

        int row = 0;

        // Nom
        Label lNom = new Label("Nom du produit : ");
        lNom.getStyleClass().add("label-champ");
        form.add(lNom, 0, row);
        form.add(champNom, 1, row++);
        form.add(errNom, 1, row++);

        // Prix
        Label lPrix = new Label("Prix HT (€) : ");
        lPrix.getStyleClass().add("label-champ");
        form.add(lPrix, 0, row);
        form.add(champPrix, 1, row++);
        form.add(errPrix, 1, row++);

        // TVA
        Label lTva = new Label("Taux TVA (%) : ");
        lTva.getStyleClass().add("label-champ");
        form.add(lTva, 0, row);
        form.add(champTva, 1, row++);
        form.add(errTva, 1, row++);

        // Poids
        Label lPoids = new Label("Poids (kg) : ");
        lPoids.getStyleClass().add("label-champ");
        form.add(lPoids, 0, row);
        form.add(champPoids, 1, row++);
        form.add(errPoids, 1, row++);

        // Seuil
        Label lSeuil = new Label("Seuil d'alerte : ");
        lSeuil.getStyleClass().add("label-champ");
        form.add(lSeuil, 0, row);
        form.add(champSeuil, 1, row++);
        form.add(errSeuil, 1, row++);

        // Conteneur principal
        VBox contenu = new VBox(10);
        contenu.getStyleClass().add("popup-contenu");
        contenu.getChildren().addAll(
                buildHeader("Modifier : " + p.getNom()),
                form
        );

        ScrollPane scroll = new ScrollPane(contenu);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("popup-root");

        dialog.getDialogPane().setContent(scroll);

        // Application du CSS
        appliquerStyleDialog(dialog.getDialogPane());

        // Validation
        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == btnEnregistrer) {

            errNom.setText(""); errPrix.setText(""); errTva.setText("");
            errPoids.setText(""); errSeuil.setText("");

            boolean valide = true;

            String nom = champNom.getText().trim();
            if (nom.isEmpty()) { errNom.setText("Le nom est obligatoire."); valide = false; }

            BigDecimal prix = null;
            try {
                prix = new BigDecimal(champPrix.getText().trim().replace(',', '.'));
                if (prix.compareTo(BigDecimal.ZERO) < 0) throw new Exception();
            } catch (Exception ex) { errPrix.setText("Prix invalide."); valide = false; }

            BigDecimal tva = null;
            try {
                tva = new BigDecimal(champTva.getText().trim().replace(',', '.'));
                if (tva.compareTo(BigDecimal.ZERO) < 0) throw new Exception();
            } catch (Exception ex) { errTva.setText("TVA invalide."); valide = false; }

            BigDecimal poids = null;
            try {
                String txt = champPoids.getText().trim().replace(',', '.');
                if (!txt.isEmpty()) poids = new BigDecimal(txt);
            } catch (Exception ex) { errPoids.setText("Poids invalide."); valide = false; }

            int seuil = 0;
            try {
                String txt = champSeuil.getText().trim();
                if (!txt.isEmpty()) seuil = Integer.parseInt(txt);
            } catch (Exception ex) { errSeuil.setText("Seuil invalide."); valide = false; }

            if (!valide) {
                afficherErreur("Veuillez corriger les champs en erreur.");
                return;
            }

            p.setNom(nom);
            p.setPrix(prix);
            p.setTauxTva(tva);
            p.setPoid(poids);
            p.setSeuilAlerte(seuil);

            try {
                produitService.modifierProduit(p);
                refreshTable();
                rafraichirFiche(p);
            } catch (Exception ex) {
                afficherErreur("Erreur : " + ex.getMessage());
            }
        }
    }
    /**
     * Construit l'en-tête visuel d'une popup (titre centré à gauche).
     *
     * @param titre texte affiché
     * @return conteneur d'en-tête
     */
    private HBox buildHeader(String titre) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("popup-header");

        Label titreLabel = new Label(titre);
        titreLabel.getStyleClass().add("popup-header-title");

        header.getChildren().add(titreLabel);
        return header;
    }
    // ─────────────────────────────────────────────────────────────
    // Actions stock
    // ─────────────────────────────────────────────────────────────
    /**
     * Demande et enregistre une entrée de stock pour un produit.
     *
     * @param p produit concerné
     */
    private void entreeDepuisLigne(Produit p) {
        if (p == null) return;
        try {
            if (p.getPoid() != null) {
                BigDecimal val = askPositiveBigDecimal("Entrée (vrac)", "Poids à ajouter (ex: 2.5)");
                if (val == null) return;
                produitService.mettreAJourStockAuto(p.getId(), null, val);
            } else {
                Integer qte = askPositiveInt("Entrée", "Quantité à ajouter");
                if (qte == null) return;
                produitService.mettreAJourStockAuto(p.getId(), qte, null);
            }
            refreshTable();
            rechargerFicheApresAction(p.getId());
        } catch (Exception e) {
            afficherErreur("Entrée impossible : " + e.getMessage());
        }
    }

    /**
     * Demande et enregistre une sortie de stock pour un produit.
     *
     * @param p produit concerné
     */
    private void sortieDepuisLigne(Produit p) {
        if (p == null) return;
        try {
            if (p.getPoid() != null) {
                BigDecimal val = askPositiveBigDecimal("Sortie (vrac)", "Poids à retirer (ex: 2.5)");
                if (val == null) return;
                produitService.mettreAJourStockAuto(p.getId(), null, val.negate());
            } else {
                Integer qte = askPositiveInt("Sortie", "Quantité à retirer");
                if (qte == null) return;
                produitService.mettreAJourStockAuto(p.getId(), -qte, null);
            }
            refreshTable();
            rechargerFicheApresAction(p.getId());
        } catch (Exception e) {
            afficherErreur("Sortie impossible : " + e.getMessage());
        }
    }

    /**
     * Demande et enregistre un ajustement absolu de stock pour un produit.
     *
     * @param p produit concerné
     */
    private void ajusterDepuisLigne(Produit p) {
        if (p == null) return;
        try {
            if (p.getPoid() != null) {
                BigDecimal nv = askPositiveBigDecimal("Ajuster (vrac)", "Nouveau poids (ex: 2.5)");
                if (nv == null) return;
                produitService.mettreAJourStockAuto(p.getId(), null, nv.subtract(p.getPoid()));
            } else {
                Integer nv = askPositiveInt("Ajuster", "Nouvelle quantité");
                if (nv == null) return;
                produitService.mettreAJourStockAuto(p.getId(), nv - p.getQuantite(), null);
            }
            refreshTable();
            rechargerFicheApresAction(p.getId());
        } catch (Exception e) {
            afficherErreur("Ajustement impossible : " + e.getMessage());
        }
    }

    /**
     * Recharge la fiche produit après une action stock si le produit est toujours sélectionné.
     *
     * @param id identifiant du produit modifié
     */
    private void rechargerFicheApresAction(int id) {
        if (produitSelectionne != null && produitSelectionne.getId() == id) {
            try {
                rafraichirFiche(produitService.obtenirProduitParId(id));
            } catch (SQLException ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Recherche / Refresh
    // ─────────────────────────────────────────────────────────────
    /**
     * Réinitialise la recherche et recharge la liste complète des produits.
     * Liaison FXML : bouton actualiser.
     */
    @FXML
    private void onRefresh() {
        searchField.clear();
        refreshTable();
    }

    /**
     * Filtre le tableau par nom ou recharge la liste complète si la recherche est vide.
     */
    private void onSearch() {
        String term = searchField.getText() == null ? "" : searchField.getText().trim();
        if (term.isEmpty()) {
            refreshTable();
            return;
        }
        try {
            data.setAll(produitService.rechercherProduitsParNom(term));
        } catch (SQLException e) {
            afficherErreur("Erreur SQL : " + e.getMessage());
        }
        updateCompteur();
    }

    /**
     * Recharge tous les produits depuis la base et met à jour le compteur.
     */
    private void refreshTable() {
        try {
            data.setAll(produitService.obtenirTousLesProduits());
        } catch (SQLException e) {
            afficherErreur("Erreur SQL : " + e.getMessage());
        }
        updateCompteur();
    }

    /**
     * Met à jour le label indiquant le nombre de produits affichés.
     */
    private void updateCompteur() {
        if (labelNbProduits != null)
            labelNbProduits.setText(data.size() + " produit(s)");
    }

    // ─────────────────────────────────────────────────────────────
    // Dialogs & helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Demande à l'utilisateur un entier positif via une boîte de dialogue.
     *
     * @param title titre de la fenêtre
     * @param msg message explicatif
     * @return valeur saisie ou {@code null} si annulé ou invalide
     */
    private Integer askPositiveInt(String title, String msg) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(msg);

        appliquerStyleDialog(d.getDialogPane());

        Optional<String> r = d.showAndWait();
        if (r.isEmpty()) return null;

        try {
            int v = Integer.parseInt(r.get().trim());
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (Exception ex) {
            afficherErreur("Entrez un entier positif.");
            return null;
        }
    }

    /**
     * Demande à l'utilisateur un nombre décimal positif via une boîte de dialogue.
     *
     * @param title titre de la fenêtre
     * @param msg message explicatif
     * @return valeur saisie ou {@code null} si annulé ou invalide
     */
    private BigDecimal askPositiveBigDecimal(String title, String msg) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(msg);

        appliquerStyleDialog(d.getDialogPane());

        Optional<String> r = d.showAndWait();
        if (r.isEmpty()) return null;

        try {
            BigDecimal v = new BigDecimal(r.get().trim().replace(',', '.'));
            if (v.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            return v;
        } catch (Exception ex) {
            afficherErreur("Entrez un nombre positif (ex: 2.5).");
            return null;
        }
    }


    /**
     * Applique la feuille de style popup à un panneau de dialogue.
     *
     * @param pane panneau cible
     */
    private void appliquerStyleDialog(DialogPane pane) {
        if (pane == null) return;

        var url = getClass().getResource("/style/popup.css");
        if (url != null) {
            String css = url.toExternalForm();
            if (!pane.getStylesheets().contains(css))
                pane.getStylesheets().add(css);
        }

        pane.getStyleClass().add("popup-root");
    }

    /**
     * Affiche une boîte de dialogue d'erreur stylisée.
     *
     * @param msg message à afficher
     */
    private void afficherErreur(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);

        appliquerStyleDialog(a.getDialogPane());
        a.showAndWait();
    }

    /**
     * Affiche une boîte de dialogue de succès stylisée.
     *
     * @param msg message à afficher
     */
    private void afficherSucces(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès");
        a.setHeaderText(null);
        a.setContentText(msg);

        appliquerStyleDialog(a.getDialogPane());
        a.showAndWait();
    }
}

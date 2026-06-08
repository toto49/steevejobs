package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.service.UserService;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.FichePayeService;
import com.eseo.steevejobs.service.HeuresTravailService;
import com.eseo.steevejobs.service.WebDavService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur FXML de gestion RH des fiches de paie (génération, filtres, PDF, NAS).
 * Liaisons FXML : {@code tableFiches}, filtres employé/année, actions par ligne.
 */
public class FichePayeController implements Initializable {

    /** Filtre par employé pour le tableau des fiches. */
    @FXML private ComboBox<User> comboEmployeFiltre;
    /** Filtre par année pour le tableau des fiches. */
    @FXML private ComboBox<Integer> comboAnneeFiltre;
    /** Tableau listant les fiches de paie filtrées. */
    @FXML private TableView<FichePaye> tableFiches;
    /** Colonne affichant le nom de l'employé. */
    @FXML private TableColumn<FichePaye, String> colEmploye;
    /** Colonne affichant l'e-mail de l'employé. */
    @FXML private TableColumn<FichePaye, String> colEmail;
    /** Colonne affichant le service ou rôle de l'employé. */
    @FXML private TableColumn<FichePaye, String> colService;
    /** Colonne affichant le mois de la fiche. */
    @FXML private TableColumn<FichePaye, String> colMois;
    /** Colonne affichant le poste de l'employé. */
    @FXML private TableColumn<FichePaye, String> colPoste;
    /** Colonne des actions (ouvrir PDF, supprimer). */
    @FXML private TableColumn<FichePaye, Void> colActions;
    /** Libellé indiquant le nombre de fiches affichées. */
    @FXML private Label lblNbFiches;

    /** Service de gestion et génération des fiches de paie. */
    private final FichePayeService fichePayeService = new FichePayeService();
    /** Service de chargement des employés pour les filtres. */
    private final UserService userService = new UserService();

    /** Format d'affichage du mois de paie (libellé long en français). */
    private static final DateTimeFormatter FMT_MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    /**
     * Configure les colonnes, les filtres et charge l'ensemble des fiches.
     *
     * @param url URL du FXML (non utilisée)
     * @param rb ressources de localisation (non utilisées)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        chargerFiltreEmployes();
        chargerFiltreAnnees();
        chargerToutesFiches();
    }

    // ==========================================
    // CONFIGURATION
    // ==========================================

    /**
     * Configure les colonnes du tableau et la cellule d'actions par ligne.
     */
    private void configurerColonnes() {
        colEmploye.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmploye().getPrenom() + " " + data.getValue().getEmploye().getNom()));
        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmploye().getEmail() != null ?
                        data.getValue().getEmploye().getEmail() : "Non renseigné"));
        colService.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmploye().getRole() != null ?
                        data.getValue().getEmploye().getRole() : "Non renseigné"));
        colMois.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDate().format(FMT_MOIS)));

        colPoste.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmploye().getPoste()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnOuvrir = new Button("📄");
            private final Button btnSuppr = new Button("🗑");
            private final HBox box = new HBox(8, btnOuvrir, btnSuppr);

            {
                btnOuvrir.setStyle("-fx-background-color: #4B78CC; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12; -fx-padding: 5 10; -fx-background-radius: 5;");
                btnSuppr.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12; -fx-padding: 5 10; -fx-background-radius: 5;");
                btnOuvrir.setOnAction(e -> ouvrirPdf(getTableView().getItems().get(getIndex())));
                btnSuppr.setOnAction(e -> confirmerSuppression(getTableView().getItems().get(getIndex())));
                box.setAlignment(Pos.CENTER);

                // Ajustement automatique des largeurs
                colEmploye.prefWidthProperty().bind(tableFiches.widthProperty().multiply(0.20));
                colEmail.prefWidthProperty().bind(tableFiches.widthProperty().multiply(0.22));
                colService.prefWidthProperty().bind(tableFiches.widthProperty().multiply(0.15));
                colMois.prefWidthProperty().bind(tableFiches.widthProperty().multiply(0.15));
                colPoste.prefWidthProperty().bind(tableFiches.widthProperty().multiply(0.18));
                colActions.prefWidthProperty().bind(tableFiches.widthProperty().multiply(0.10));
            }

            /**
             * Affiche les boutons ouvrir/supprimer sur chaque ligne.
             *
             * @param item non utilisé
             * @param empty {@code true} si la ligne est hors plage
             */
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    /**
     * Charge la liste des employés dans le filtre combo.
     */
    private void chargerFiltreEmployes() {
        try {
            ObservableList<User> employes = FXCollections.observableArrayList();
            employes.add(null);
            employes.addAll(userService.getAllUsers());
            comboEmployeFiltre.setItems(employes);
            comboEmployeFiltre.setConverter(new StringConverter<>() {
                /**
                 * Affiche le nom de l'employé dans le filtre.
                 *
                 * @param u employé ou {@code null} pour « Tous »
                 * @return libellé affiché
                 */
                @Override public String toString(User u) {
                    return u == null ? "Tous les employés" : u.getPrenom() + " " + u.getNom();
                }
                /**
                 * Non utilisé pour un filtre en lecture seule.
                 *
                 * @param s chaîne saisie
                 * @return toujours {@code null}
                 */
                @Override public User fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les employés : " + e.getMessage());
        }
    }

    /**
     * Initialise le filtre combo des années (courante et cinq précédentes).
     */
    private void chargerFiltreAnnees() {
        int anneeActuelle = LocalDateTime.now().getYear();
        ObservableList<Integer> annees = FXCollections.observableArrayList();
        annees.add(null);
        for (int a = anneeActuelle; a >= anneeActuelle - 5; a--) {
            annees.add(a);
        }
        comboAnneeFiltre.setItems(annees);
        comboAnneeFiltre.setConverter(new StringConverter<>() {
            /**
             * Affiche l'année dans le filtre combo.
             *
             * @param a année ou {@code null} pour « Toutes »
             * @return libellé affiché
             */
            @Override public String toString(Integer a) {
                return a == null ? "Toutes les années" : String.valueOf(a);
            }
            /**
             * Non utilisé pour un filtre en lecture seule.
             *
             * @param s chaîne saisie
             * @return toujours {@code null}
             */
            @Override public Integer fromString(String s) { return null; }
        });
    }

    // ==========================================
    // ACTIONS
    // ==========================================

    /**
     * Navigue vers la vue calendrier RH.
     * Liaison FXML : bouton calendrier.
     */
    @FXML
    private void ouvrirCalendrierRh() {
        if (MenuController.getInstance() != null) {
            MenuController.getInstance().chargerPage("calendrier-rh");
            MenuController.getInstance().changerTitre("Calendrier RH");
        }
    }

    /**
     * Ouvre le dialogue de génération d'une fiche de paie et l'envoie sur le NAS.
     * Liaison FXML : bouton de génération.
     *
     * @throws SQLException affichée via alerte en cas d'échec
     */
    @FXML
    private void ouvrirFormulaireGeneration() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Générer");
        dialog.setHeaderText("Générer une fiche de paie");
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");

        ButtonType btnGenerer = new ButtonType("Générer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGenerer, ButtonType.CANCEL);

        // Création du GridPane
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");

        // CHAMPS
        ComboBox<User> comboEmploye = new ComboBox<>();
        ComboBox<String> comboMois = new ComboBox<>();
        ComboBox<Integer> comboAnnee = new ComboBox<>();
        TextField txtHeuresTravaillees = new TextField();     // Heures travaillées (auto-calculé mais possibilité de modif)
        TextField txtTauxHoraire = new TextField();           // Taux horaire (€)
        TextField txtTauxCotisationsPatronales = new TextField();  // Taux patronal (%)

        // Heures travaillées : est modifiable, calculé automatiquement depuis la BDD
        txtHeuresTravaillees.setEditable(true);
        txtHeuresTravaillees.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-padding: 6; -fx-text-fill: black;");

        txtTauxHoraire.setPromptText("Ex : 15.50");
        txtTauxCotisationsPatronales.setPromptText("Ex : 45");

        // Style des champs
        String fieldStyle = "-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-padding: 6; -fx-text-fill: black;";
        txtTauxHoraire.setStyle(fieldStyle);
        txtTauxCotisationsPatronales.setStyle(fieldStyle);
        comboEmploye.setStyle(fieldStyle);
        comboMois.setStyle(fieldStyle);
        comboAnnee.setStyle(fieldStyle);

        // Configuration des années
        int anneeActuelle = LocalDate.now().getYear();
        for (int a = anneeActuelle - 2; a <= anneeActuelle + 1; a++) {
            comboAnnee.getItems().add(a);
        }
        comboAnnee.setValue(anneeActuelle);

        // Configuration des mois
        String[] mois = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        comboMois.getItems().addAll(mois);
        comboMois.setValue(mois[LocalDate.now().getMonthValue() - 1]);

        // Configuration employé
        try {
            comboEmploye.setItems(FXCollections.observableArrayList(userService.getActiveUsers()));
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les employés : " + e.getMessage());
            return;
        }

        comboEmploye.setConverter(new StringConverter<User>() {
            /**
             * Affiche l'employé et son poste dans le formulaire de génération.
             *
             * @param u employé sélectionné
             * @return libellé affiché
             */
            @Override public String toString(User u) {
                return u == null ? "" : u.getPrenom() + " " + u.getNom() + " — " + u.getPoste();
            }
            /**
             * Non utilisé pour une sélection par liste.
             *
             * @param s chaîne saisie
             * @return toujours {@code null}
             */
            @Override public User fromString(String s) { return null; }

        });

        // ==========================================
        // CALCUL DES HEURES
        // ==========================================
        HeuresTravailService heuresService = new HeuresTravailService();

        Runnable calculerHeures = () -> {
            User employe = comboEmploye.getValue();
            Integer annee = comboAnnee.getValue();
            String moisStr = comboMois.getValue();

            if (employe != null && annee != null && moisStr != null) {
                int moisValue = comboMois.getSelectionModel().getSelectedIndex() + 1;
                try {
                    double total = heuresService.getTotalHeuresByMonth(employe.getId(), annee, moisValue);
                    txtHeuresTravaillees.setText(String.format("%.2f", total));
                } catch (SQLException e) {
                    txtHeuresTravaillees.setText("0,00");
                }
            } else {
                txtHeuresTravaillees.setText("0,00");
            }
        };
        // ==========================================
        // GESTION DU TAUX PATRONAL
        // ==========================================

        // Charger le taux horaire quand on sélectionne un employé
        comboEmploye.setOnAction(e -> {
            User employe = comboEmploye.getValue();
            if (employe != null) {
                try {
                    User employeComplet = userService.getUserById(employe.getId());
                    if (employeComplet != null) {
                        // Taux horaire
                        if (employeComplet.getTaux() > 0) {
                            txtTauxHoraire.setText(String.valueOf(employeComplet.getTaux()));
                        } else {
                            txtTauxHoraire.setText("");
                        }
                        // Taux patronal
                        if (employeComplet.getTauxPatronal() > 0) {
                            txtTauxCotisationsPatronales.setText(String.valueOf(employeComplet.getTauxPatronal()));
                        } else {
                            txtTauxCotisationsPatronales.setText("");
                        }
                    }
                } catch (SQLException ex) {
                    txtTauxHoraire.setText("");
                    txtTauxCotisationsPatronales.setText("");
                }
            } else {
                txtTauxHoraire.setText("");
                txtTauxCotisationsPatronales.setText("");
            }
            calculerHeures.run();
        });

// Sauvegarder le taux patronal quand le champ perd le focus
        txtTauxCotisationsPatronales.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                User employe = comboEmploye.getValue();
                if (employe != null && !txtTauxCotisationsPatronales.getText().isBlank()) {
                    try {
                        int tauxPatronal = Integer.parseInt(txtTauxCotisationsPatronales.getText().replace(",", "."));
                        userService.updateTauxPatronal(employe.getId(), tauxPatronal);
                    } catch (NumberFormatException | SQLException ex) {
                        // Ignorer
                    }
                }
            }
        });

// Sauvegarder le taux horaire quand le champ perd le focus
        txtTauxHoraire.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Perte du focus
                User employe = comboEmploye.getValue();
                if (employe != null && !txtTauxHoraire.getText().isBlank()) {
                    try {
                        int tauxHoraire = Integer.parseInt(txtTauxHoraire.getText().replace(",", "."));
                        userService.updateTaux(employe.getId(), tauxHoraire);  // ← Sauvegarde dans taux
                    } catch (NumberFormatException | SQLException ex) {
                        // Ignorer
                    }
                }
            }
        });

        // Recalculer quand on change mois/année
        comboMois.setOnAction(e -> calculerHeures.run());
        comboAnnee.setOnAction(e -> calculerHeures.run());

        // Calculer une première fois au chargement
        calculerHeures.run();

        // Labels
        String labelStyle = "-fx-text-fill: #333333; -fx-font-weight: bold;";

        Label lblEmploye = new Label("Employé :"); lblEmploye.setStyle(labelStyle);
        Label lblMois = new Label("Mois :"); lblMois.setStyle(labelStyle);
        Label lblAnnee = new Label("Année :"); lblAnnee.setStyle(labelStyle);
        Label lblHeures = new Label("Heures travaillées :"); lblHeures.setStyle(labelStyle);
        Label lblTauxHoraire = new Label("Taux horaire (€) :"); lblTauxHoraire.setStyle(labelStyle);
        Label lblTauxCotisations = new Label("Cotisations patronales (%) :"); lblTauxCotisations.setStyle(labelStyle);
        Label lblExemple = new Label("(ex: 45 = 45%)"); lblExemple.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        // Ajout au grid
        grid.add(lblEmploye, 0, 0);
        grid.add(comboEmploye, 1, 0);
        grid.add(lblMois, 0, 1);
        grid.add(comboMois, 1, 1);
        grid.add(lblAnnee, 0, 2);
        grid.add(comboAnnee, 1, 2);
        grid.add(lblHeures, 0, 3);
        grid.add(txtHeuresTravaillees, 1, 3);
        grid.add(lblTauxHoraire, 0, 4);
        grid.add(txtTauxHoraire, 1, 4);
        grid.add(lblTauxCotisations, 0, 5);
        grid.add(txtTauxCotisationsPatronales, 1, 5);
        grid.add(lblExemple, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(550);

        // Style des boutons
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(btnGenerer);
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);

        if (btnOk != null) {
            btnOk.setStyle("-fx-background-color: #4B78CC; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16;");
        }
        if (btnCancel != null) {
            btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333333; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 16;");
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != btnGenerer) return;

        // Validation
        if (comboEmploye.getValue() == null || comboMois.getValue() == null || comboAnnee.getValue() == null
                || txtHeuresTravaillees.getText().isBlank() || txtTauxHoraire.getText().isBlank()
                || txtTauxCotisationsPatronales.getText().isBlank()) {
            afficherErreur("Tous les champs sont obligatoires.");
            return;
        }

        // Récupération et calcul des valeurs
        User employe = comboEmploye.getValue();
        double heuresTravaillees, tauxHoraire, tauxCotisationsPatronales;
        try {
            heuresTravaillees = Double.parseDouble(txtHeuresTravaillees.getText().replace(",", "."));
            tauxHoraire = Double.parseDouble(txtTauxHoraire.getText().replace(",", "."));
            tauxCotisationsPatronales = Double.parseDouble(txtTauxCotisationsPatronales.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            afficherErreur("Les valeurs doivent être des nombres valides.");
            return;
        }

        // Calcul du salaire brut
        double salaireBrut = heuresTravaillees * tauxHoraire;

        int moisValue = comboMois.getSelectionModel().getSelectedIndex() + 1;
        int anneeValue = comboAnnee.getValue();
        LocalDateTime date = LocalDateTime.of(anneeValue, moisValue, 1, 0, 0);

        try {
            FichePaye fiche = fichePayeService.genererFichePaye(employe, date, salaireBrut,
                    tauxCotisationsPatronales / 100, heuresTravaillees, tauxHoraire);

            String nomFichier = String.format("fiche_%d_%d_%02d.pdf", employe.getId(), date.getYear(), date.getMonthValue());
            String dossierEmploye = "employe_" + employe.getId();
            if (employe.getPrenom() != null && employe.getNom() != null) {
                dossierEmploye = (employe.getPrenom() + "_" + employe.getNom()).toLowerCase().replaceAll("[^a-z0-9_]", "");
            }
            final String finalDossier = dossierEmploye;

            CompletableFuture.runAsync(() -> {
                try {
                    WebDavService.envoyerFichierLocalSurNAS(finalDossier, nomFichier, fiche.getUrl());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAcceptAsync(v -> {
                Platform.runLater(() -> {
                    afficherSucces("Fiche générée et sauvegardée sur le NAS avec succès !");
                    chargerToutesFiches();
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    afficherErreur("Fiche générée localement, mais échec de l'envoi sur le NAS : " + ex.getCause().getMessage());
                    chargerToutesFiches();
                });
                return null;
            });

        } catch (IllegalStateException | IllegalArgumentException e) {
            afficherErreur(e.getMessage());
        } catch (Exception e) {
            afficherErreur("Erreur lors de la génération : " + e.getMessage());
        }
    }

    /**
     * Filtre le tableau des fiches selon l'employé et l'année sélectionnés.
     * Liaison FXML : filtres employé et année.
     *
     * @throws SQLException affichée via alerte en cas d'échec
     */
    @FXML
    private void filtrerParEmploye() {
        User employe = comboEmployeFiltre.getValue();
        Integer annee = comboAnneeFiltre.getValue();
        try {
            List<FichePaye> fiches;
            if (employe != null && annee != null) {
                fiches = fichePayeService.findByEmployeId(employe.getId()).stream()
                        .filter(f -> f.getDate().getYear() == annee)
                        .toList();
            } else if (employe != null) {
                fiches = fichePayeService.findByEmployeId(employe.getId());
            } else if (annee != null) {
                fiches = fichePayeService.findByAnnee(annee);
            } else {
                fiches = fichePayeService.findAll();
            }
            tableFiches.setItems(FXCollections.observableArrayList(fiches));
            lblNbFiches.setText(fiches.size() + " fiche(s)");
        } catch (SQLException e) {
            afficherErreur("Erreur filtre : " + e.getMessage());
        }
    }

    // ==========================================
    // GESTION PDF
    // ==========================================

    /**
     * Ouvre le fichier PDF local associé à une fiche de paie.
     *
     * @param fiche fiche dont le PDF doit être ouvert
     */
    private void ouvrirPdf(FichePaye fiche) {
        try {
            File f = new File(fiche.getUrl());
            if (f.exists()) {
                Desktop.getDesktop().open(f);
            } else {
                afficherErreur("Fichier introuvable :\n" + fiche.getUrl());
            }
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir le PDF : " + e.getMessage());
        }
    }

    /**
     * Demande confirmation puis supprime une fiche de paie et son fichier.
     *
     * @param fiche fiche à supprimer
     */
    private void confirmerSuppression(FichePaye fiche) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la fiche");
        confirm.setHeaderText("Supprimer la fiche de " +
                fiche.getEmploye().getPrenom() + " " + fiche.getEmploye().getNom() +
                " — " + fiche.getDate().format(FMT_MOIS) + " ?");
        confirm.setContentText("Le fichier PDF sera également supprimé. Action irréversible.");
        confirm.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    fichePayeService.supprimer(fiche.getId());
                    new File(fiche.getUrl()).delete();
                    String nomFichier = String.format("fiche_%d_%d_%02d.pdf", fiche.getEmploye().getId(), fiche.getDate().getYear(), fiche.getDate().getMonthValue());
                    String dossierEmploye = "employe_" + fiche.getEmploye().getId();
                    if (fiche.getEmploye().getPrenom() != null && fiche.getEmploye().getNom() != null) {
                        dossierEmploye = (fiche.getEmploye().getPrenom() + "_" + fiche.getEmploye().getNom()).toLowerCase().replaceAll("[^a-z0-9_]", "");
                    }
                    final String finalDossier = dossierEmploye;
                    CompletableFuture.runAsync(() -> {
                        WebDavService.supprimerFichierDuNAS(finalDossier, nomFichier);
                    });

                    chargerToutesFiches();
                } catch (SQLException e) {
                    afficherErreur("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    // ==========================================
    // CHARGEMENT
    // ==========================================

    /**
     * Recharge toutes les fiches de paie dans le tableau.
     */
    private void chargerToutesFiches() {
        try {
            tableFiches.setItems(FXCollections.observableArrayList(fichePayeService.findAll()));
            lblNbFiches.setText(fichePayeService.findAll().size() + " fiche(s)");
        } catch (SQLException e) {
            afficherErreur("Impossible de charger les fiches : " + e.getMessage());
        }
    }

    // ==========================================
    // MESSAGES
    // ==========================================

    /**
     * Affiche une alerte d'erreur.
     *
     * @param msg message affiché
     */
    private void afficherErreur(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");
        alert.showAndWait();
    }

    /**
     * Affiche une alerte de succès.
     *
     * @param msg message affiché
     */
    private void afficherSucces(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10;");
        alert.showAndWait();
    }
}
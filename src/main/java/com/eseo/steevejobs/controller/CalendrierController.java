package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.config.ColorContrastUtil;
import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.HeuresTravail;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.SoldeConge;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.CongeUtil;
import com.eseo.steevejobs.service.DemandeCongeService;
import com.eseo.steevejobs.service.HeuresTravailService;
import com.eseo.steevejobs.service.PlanningService;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.util.TestRuntime;
import com.eseo.steevejobs.service.UserService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.util.StringConverter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Contrôleur FXML du calendrier hebdomadaire (employé ou RH selon la vue chargée).
 * Liaisons FXML : labels de jours, boutons heures, {@code gridPlanning}, {@code comboEmploye} (vue RH).
 * Gère le planning, les heures de travail et les demandes de congés.
 */
public class CalendrierController {

    private static final String STYLE_CLASSE_HEURES = "btn-mes-heures";
    private static final String STYLE_CLASSE_HEURES_ACTIF = "btn-mes-heures-actif";
    private static final String STYLE_CLASSE_HEURES_INACTIF = "btn-mes-heures-inactif";
    private static final int RECHERCHE_EMPLOYE_MIN_CHARS = 2;
    private static final int RECHERCHE_EMPLOYE_MAX_SUGGESTIONS = 8;

    @FXML
    private Label lundiLabel, mardiLabel, mercrediLabel, jeudiLabel, vendrediLabel, samediLabel, dimancheLabel;
    @FXML
    private Button lundiHeuresBtn, mardiHeuresBtn, mercrediHeuresBtn, jeudiHeuresBtn, vendrediHeuresBtn, samediHeuresBtn, dimancheHeuresBtn;
    @FXML
    private Label labelSemaine;
    @FXML
    private DatePicker datePickerSemaine;
    @FXML
    private ComboBox<User> comboEmploye;
    @FXML
    private GridPane gridPlanning;

    private LocalDate dateDebutSemaineAffichee;
    private List<Planning> events;
    private User utilisateurConnecte;
    private User utilisateurAffiche;
    private PlanningService planningService;
    private HeuresTravailService heuresTravailService;
    private DemandeCongeService demandeCongeService;
    private UserService userService;
    private boolean miseAJourRechercheEmploye;
    private int employeSelectionneId = -1;
    private List<DemandeConge> demandesEnAttente = new ArrayList<>();


    // ==========================================
    // INITIALISATION
    // ==========================================

    /**
     * Initialise les services, la semaine courante et le sélecteur employé (vue RH).
     * En mode test, sort sans charger les données.
     *
     * @throws SQLException si le chargement initial du planning échoue
     */
    @FXML
    public void initialize() throws SQLException {
        planningService = new PlanningService();
        heuresTravailService = new HeuresTravailService();
        demandeCongeService = new DemandeCongeService();
        userService = new UserService();
        utilisateurConnecte = SessionService.getUtilisateurConnecte();
        dateDebutSemaineAffichee = LocalDate.now().with(DayOfWeek.MONDAY);

        if (TestRuntime.isEnabled()) {
            events = new ArrayList<>();
            demandesEnAttente = new ArrayList<>();
            return;
        }

        if (comboEmploye != null) {
            utilisateurAffiche = null;
            events = new ArrayList<>();
            initialiserSelecteurEmploye();
        } else {
            if (utilisateurConnecte != null) {
                utilisateurAffiche = utilisateurConnecte;
                rechargerDonneesPlanning();
            } else {
                utilisateurAffiche = null;
                events = new ArrayList<>();
                demandesEnAttente = new ArrayList<>();
            }
        }

        rafraichirCalendrier();

        datePickerSemaine.setOnAction(event -> {
            LocalDate dateChoisie = datePickerSemaine.getValue();
            if (dateChoisie != null) {
                dateDebutSemaineAffichee = dateChoisie.with(DayOfWeek.MONDAY);
                try { rafraichirCalendrier(); } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    /**
     * Charge les événements de planning pour l'employé actuellement affiché.
     *
     * @return liste des plannings ; liste vide si aucun employé sélectionné
     * @throws SQLException en cas d'erreur d'accès base de données
     */
    public List<Planning> initEvent() throws SQLException {
        if (utilisateurAffiche == null) {
            return new ArrayList<>();
        }
        return planningService.obtenirPlanningsParUtilisateur(utilisateurAffiche.getId());
    }

    private void rechargerDonneesPlanning() throws SQLException {
        events = initEvent();
        if (estCalendrierEmploye() && utilisateurAffiche != null) {
            demandesEnAttente = demandeCongeService.listerEnAttenteParEmploye(utilisateurAffiche.getId());
        } else {
            demandesEnAttente = new ArrayList<>();
        }
    }

    private boolean estCalendrierEmploye() {
        return comboEmploye == null;
    }

    private boolean estEvenementConge(Planning event) {
        return event != null && CongeUtil.estTypeConge(event.getType());
    }

    private void initialiserSelecteurEmploye() {
        comboEmploye.setEditable(true);
        comboEmploye.setItems(FXCollections.observableArrayList());
        comboEmploye.setPromptText("Nom, prénom ou email…");
        comboEmploye.setConverter(new StringConverter<>() {
            /**
             * @param user employé à afficher
             * @return nom complet formaté
             */
            @Override
            public String toString(User user) {
                return formaterNomEmploye(user);
            }

            /**
             * @param string saisie utilisateur
             * @return employé correspondant ou {@code null}
             */
            @Override
            public User fromString(String string) {
                if (string == null || string.isBlank()) {
                    return null;
                }
                String recherche = string.trim();
                for (User user : comboEmploye.getItems()) {
                    if (formaterNomEmploye(user).equalsIgnoreCase(recherche)) {
                        return user;
                    }
                }
                return null;
            }
        });

        miseAJourRechercheEmploye = true;
        comboEmploye.setValue(null);
        comboEmploye.getEditor().clear();
        employeSelectionneId = -1;
        miseAJourRechercheEmploye = false;

        comboEmploye.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (miseAJourRechercheEmploye) {
                return;
            }
            if (utilisateurAffiche != null && newText != null
                    && formaterNomEmploye(utilisateurAffiche).equalsIgnoreCase(newText.trim())) {
                return;
            }
            if (newText == null || newText.isBlank()) {
                reinitialiserSelectionEmploye();
                return;
            }
            proposerEmployesCorrespondants(newText);
        });

        comboEmploye.getSelectionModel().selectedItemProperty().addListener((obs, ancien, selection) -> {
            if (miseAJourRechercheEmploye || selection == null) {
                return;
            }
            try {
                appliquerEmployeSelectionne(selection);
            } catch (SQLException e) {
                afficherErreur("Impossible de charger le planning : " + e.getMessage());
            }
        });
    }

    private void reinitialiserSelectionEmploye() {
        utilisateurAffiche = null;
        employeSelectionneId = -1;
        events = new ArrayList<>();
        comboEmploye.getItems().clear();
        comboEmploye.hide();
        try {
            rafraichirCalendrier();
        } catch (SQLException e) {
            afficherErreur("Impossible de rafraîchir le calendrier : " + e.getMessage());
        }
    }

    private boolean employeRhSelectionne() {
        if (comboEmploye == null) {
            return true;
        }
        if (utilisateurAffiche != null) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Employé requis");
        alert.setHeaderText(null);
        alert.setContentText("Recherchez et sélectionnez un employé pour afficher son planning.");
        appliquerStyleAlert(alert);
        alert.showAndWait();
        return false;
    }

    private void proposerEmployesCorrespondants(String saisie) {
        if (saisie == null || saisie.trim().length() < RECHERCHE_EMPLOYE_MIN_CHARS) {
            Platform.runLater(() -> {
                if (!miseAJourRechercheEmploye) {
                    comboEmploye.getItems().clear();
                    comboEmploye.hide();
                }
            });
            return;
        }

        final String terme = saisie.trim();
        Thread recherche = new Thread(() -> {
            try {
                List<User> resultats = userService.searchUsersByName(terme).stream()
                        .limit(RECHERCHE_EMPLOYE_MAX_SUGGESTIONS)
                        .toList();
                Platform.runLater(() -> {
                    if (miseAJourRechercheEmploye) {
                        return;
                    }
                    String saisieActuelle = comboEmploye.getEditor().getText();
                    if (saisieActuelle == null || !saisieActuelle.trim().equals(terme)) {
                        return;
                    }
                    comboEmploye.getItems().setAll(resultats);
                    if (resultats.isEmpty()) {
                        comboEmploye.hide();
                    } else {
                        comboEmploye.show();
                    }
                });
            } catch (SQLException e) {
                Platform.runLater(() -> afficherErreur("Recherche employé impossible : " + e.getMessage()));
            }
        }, "recherche-employe-rh");
        recherche.setDaemon(true);
        recherche.start();
    }
    private void appliquerStyleDialog(DialogPane dp) {
        try {
            java.net.URL popupUrl = getClass().getResource("/style/popup.css");
            if (popupUrl != null) dp.getStylesheets().add(popupUrl.toExternalForm());
        } catch (Exception ignored) {}

        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);
        if (btnOk != null) btnOk.getStyleClass().add("button-ok");

        Button btnCancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (btnCancel != null) btnCancel.getStyleClass().add("button-cancel");
    }

    private void appliquerEmployeSelectionne(User employe) throws SQLException {
        if (employe == null || employe.getId() == employeSelectionneId) {
            return;
        }

        employeSelectionneId = employe.getId();
        utilisateurAffiche = employe;
        events = initEvent();
        demandesEnAttente = new ArrayList<>();
        rafraichirCalendrier();

        String nomAffiche = formaterNomEmploye(employe);
        miseAJourRechercheEmploye = true;
        comboEmploye.getItems().setAll(employe);
        comboEmploye.getSelectionModel().select(employe);
        comboEmploye.setValue(employe);
        comboEmploye.getEditor().setText(nomAffiche);
        comboEmploye.hide();

        Platform.runLater(() -> {
            comboEmploye.hide();
            if (gridPlanning != null) {
                gridPlanning.requestFocus();
            } else if (comboEmploye.getScene() != null) {
                comboEmploye.getScene().getRoot().requestFocus();
            }
            miseAJourRechercheEmploye = false;
        });
    }

    private String formaterNomEmploye(User user) {
        if (user == null) {
            return "";
        }
        String prenom = user.getPrenom() != null ? user.getPrenom().trim() : "";
        String nom = user.getNom() != null ? user.getNom().trim() : "";
        return (prenom + " " + nom).trim();
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        appliquerStyleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Affiche la semaine suivante.
     *
     * @param e événement du bouton (non utilisé)
     * @throws SQLException si le rafraîchissement du calendrier échoue
     */
    public void nextWeek(ActionEvent e) throws SQLException {
        dateDebutSemaineAffichee = dateDebutSemaineAffichee.plusWeeks(1); rafraichirCalendrier();
    }

    /**
     * Affiche la semaine précédente.
     *
     * @param e événement du bouton (non utilisé)
     * @throws SQLException si le rafraîchissement du calendrier échoue
     */
    public void lastWeek(ActionEvent e) throws SQLException {
        dateDebutSemaineAffichee = dateDebutSemaineAffichee.minusWeeks(1); rafraichirCalendrier();
    }

    /**
     * Met à jour les en-têtes de jours, le libellé de semaine et les blocs d'événements.
     *
     * @throws SQLException si le rechargement des données est nécessaire et échoue
     */
    @FXML
    public void rafraichirCalendrier() throws SQLException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH);
        Label[] labels = {lundiLabel, mardiLabel, mercrediLabel, jeudiLabel, vendrediLabel, samediLabel, dimancheLabel};
        Button[] boutonsHeures = {lundiHeuresBtn, mardiHeuresBtn, mercrediHeuresBtn, jeudiHeuresBtn, vendrediHeuresBtn, samediHeuresBtn, dimancheHeuresBtn};

        LocalDate aujourdhui = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            LocalDate dateJour = dateDebutSemaineAffichee.plusDays(i);
            labels[i].setText(dateJour.format(dtf).toUpperCase());

            appliquerStyleBoutonHeures(boutonsHeures[i], dateJour.isAfter(aujourdhui));
        }

        DateTimeFormatter sf = DateTimeFormatter.ofPattern("dd MMMM", Locale.FRENCH);
        labelSemaine.setText("Semaine du " + dateDebutSemaineAffichee.format(sf) + " au " + dateDebutSemaineAffichee.plusDays(6).format(sf));

        showEvent();
    }

    private void appliquerStyleBoutonHeures(Button bouton, boolean desactive) {
        bouton.getStyleClass().removeAll(STYLE_CLASSE_HEURES_ACTIF, STYLE_CLASSE_HEURES_INACTIF);
        if (!bouton.getStyleClass().contains(STYLE_CLASSE_HEURES)) {
            bouton.getStyleClass().add(STYLE_CLASSE_HEURES);
        }
        bouton.setDisable(desactive);
        bouton.getStyleClass().add(desactive ? STYLE_CLASSE_HEURES_INACTIF : STYLE_CLASSE_HEURES_ACTIF);
    }

    // ==========================================
    // GESTION DES HEURES DE TRAVAIL
    // ==========================================

    /**
     * Ouvre la popup de traitement des demandes de congés (vue RH uniquement).
     * Liaison FXML : bouton « Demandes de congés ».
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    public void ouvrirPopupDemandesConge(ActionEvent event) {
        if (comboEmploye == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/eseo/steevejobs/view/demandes-conge-popup.fxml"));
            VBox contenu = loader.load();
            DemandesCongeController controller = loader.getController();
            controller.setOnDemandeTraitee(() -> {
                try {
                    if (utilisateurAffiche != null) {
                        events = initEvent();
                        rafraichirCalendrier();
                    }
                } catch (SQLException e) {
                    afficherErreur("Impossible de rafraîchir le planning : " + e.getMessage());
                }
            });

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Demandes de congés");
            dialog.setResizable(true);

            DialogPane dp = dialog.getDialogPane();
            dp.setContent(contenu);
            dp.getButtonTypes().add(ButtonType.CLOSE);
            dp.getStylesheets().add(getClass().getResource("/style/popup.css").toExternalForm());
            dp.setMinWidth(1080);
            dp.setPrefWidth(1120);
            dp.setMinHeight(640);
            dp.setPrefHeight(660);

            Button btnFermer = (Button) dp.lookupButton(ButtonType.CLOSE);
            if (btnFermer != null) {
                btnFermer.setText("Fermer");
                btnFermer.getStyleClass().add("button-cancel");
            }

            dialog.showAndWait();
        } catch (Exception e) {
            afficherErreur("Impossible d'ouvrir les demandes de congés : " + e.getMessage());
        }
    }

    /**
     * Ouvre la popup de saisie ou consultation des heures pour le jour lié au bouton.
     * Vérifie qu'un employé est sélectionné en vue RH. Mode lecture seule au-delà de 7 jours.
     * Liaison FXML : boutons heures ({@code userData} = index du jour).
     *
     * @param event événement du bouton source
     */
    @FXML
    public void ouvrirPopupHeures(ActionEvent event) {
        if (!employeRhSelectionne()) {
            return;
        }
        Node source = (Node) event.getSource();
        int dayIndex = Integer.parseInt(source.getUserData().toString());
        LocalDate dateCible = dateDebutSemaineAffichee.plusDays(dayIndex);

        // --- DÉTECTION DU MODE LECTURE SEULE ---
        final boolean isReadOnly = dateCible.isBefore(LocalDate.now().minusDays(7));

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isReadOnly ? "Consultation des heures" : "Saisie des heures");
        dialog.setHeaderText("Heures du " + dateCible.format(DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH)));

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dp.getStylesheets().add(getClass().getResource("/style/popup.css").toExternalForm());

        // 1. récupération BDD
        HeuresTravail hr = null;
        try {
            hr = heuresTravailService.getHeuresParDate(utilisateurAffiche.getId(), dateCible);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        boolean aDejaSaisi = (hr != null);

        // 2. Création des ComboBox horaires (tranches de 15 min)
        ComboBox<String> cbDebutM = creerComboBoxTemps("matin");
        ComboBox<String> cbFinM = creerComboBoxTemps("matin");
        ComboBox<String> cbDebutA = creerComboBoxTemps("aprem");
        ComboBox<String> cbFinA = creerComboBoxTemps("aprem");

        cbDebutM.setValue(aDejaSaisi && hr.getDebutMatin() != null ? hr.getDebutMatin().toString() : "08:00");
        cbFinM.setValue(aDejaSaisi && hr.getFinMatin() != null ? hr.getFinMatin().toString() : "12:00");
        cbDebutA.setValue(aDejaSaisi && hr.getDebutAprem() != null ? hr.getDebutAprem().toString() : "13:30");
        cbFinA.setValue(aDejaSaisi && hr.getFinAprem() != null ? hr.getFinAprem().toString() : "17:30");

        if (isReadOnly) {
            cbDebutM.setDisable(true); cbFinM.setDisable(true);
            cbDebutA.setDisable(true); cbFinA.setDisable(true);
        }

        // 3. Création des Labels totaux avec des styles forts pour assurer la visibilité
        Label lblTotalMatin = new Label("0 h 00");
        lblTotalMatin.setStyle("-fx-font-weight: bold; -fx-text-fill: #2ecc71; -fx-font-size: 13px;"); // Vert

        Label lblTotalAprem = new Label("0 h 00");
        lblTotalAprem.setStyle("-fx-font-weight: bold; -fx-text-fill: #2ecc71; -fx-font-size: 13px;"); // Vert

        Label lblTotalJour = new Label("0 h 00");
        lblTotalJour.setStyle("-fx-font-weight: bold; -fx-text-fill: #6588d9; -fx-font-size: 15px;"); // Bleu

        // Calcul dynamique
        ChangeListener<String> calculListener = (obs, oldV, newV) -> {
            long minMatin = calculerDureeMinutes(cbDebutM.getValue(), cbFinM.getValue());
            long minAprem = calculerDureeMinutes(cbDebutA.getValue(), cbFinA.getValue());

            lblTotalMatin.setText((minMatin / 60) + " h " + String.format("%02d", minMatin % 60));
            lblTotalAprem.setText((minAprem / 60) + " h " + String.format("%02d", minAprem % 60));
            lblTotalJour.setText(((minMatin + minAprem) / 60) + " h " + String.format("%02d", (minMatin + minAprem) % 60));
        };

        cbDebutM.valueProperty().addListener(calculListener);
        cbFinM.valueProperty().addListener(calculListener);
        cbDebutA.valueProperty().addListener(calculListener);
        cbFinA.valueProperty().addListener(calculListener);
        calculListener.changed(null, null, null); // Premier calcul

        // 4. Interface Grid
        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(12);
        grid.setPrefWidth(450);

        // Section MATIN
        grid.add(new Label("MATIN :"), 0, 0, 6, 1);
        grid.add(cbDebutM, 0, 1);
        grid.add(new Label("à"), 1, 1);
        grid.add(cbFinM, 2, 1);
        grid.add(new Label("="), 3, 1);
        grid.add(lblTotalMatin, 4, 1);

        // Section APRÈS-MIDI
        grid.add(new Label("APRÈS-MIDI :"), 0, 2, 6, 1);
        grid.add(cbDebutA, 0, 3);
        grid.add(new Label("à"), 1, 3);
        grid.add(cbFinA, 2, 3);
        grid.add(new Label("="), 3, 3);
        grid.add(lblTotalAprem, 4, 3);

        // Section TOTAL
        grid.add(new Label("TOTAL JOURNÉE :"), 0, 4, 6, 1);
        grid.add(lblTotalJour, 0, 5, 6, 1);

        // Application du style "label-style" SAUF pour nos labels de totaux pour ne pas écraser leurs couleurs
        grid.getChildren().filtered(n -> n instanceof Label && n != lblTotalMatin && n != lblTotalAprem && n != lblTotalJour)
                .forEach(n -> n.getStyleClass().add("label-style"));

        dp.setContent(grid);

        // 5. Boutons
        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);
        if (btnOk != null) {
            btnOk.getStyleClass().add("button-ok");
            btnOk.setText(isReadOnly ? "Fermer" : (aDejaSaisi ? "Modifier" : "Enregistrer"));
        }

        Button btnCancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (btnCancel != null) {
            btnCancel.getStyleClass().add("button-cancel");
            btnCancel.setText("Annuler");
            if (isReadOnly) btnCancel.setVisible(false);
        }

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK && !isReadOnly) {
                try {
                    LocalTime tDebutM = LocalTime.parse(cbDebutM.getValue());
                    LocalTime tFinM = LocalTime.parse(cbFinM.getValue());
                    LocalTime tDebutA = LocalTime.parse(cbDebutA.getValue());
                    LocalTime tFinA = LocalTime.parse(cbFinA.getValue());

                    // --- NOUVEAU : Calcul du temps total ---
                    long minMatin = calculerDureeMinutes(cbDebutM.getValue(), cbFinM.getValue());
                    long minAprem = calculerDureeMinutes(cbDebutA.getValue(), cbFinA.getValue());
                    long totalMinutes = minMatin + minAprem;

                    // Conversion du total en LocalTime (Heures, Minutes)
                    LocalTime tTotal = LocalTime.of((int) (totalMinutes / 60), (int) (totalMinutes % 60));

                    // N'oublie pas d'ajouter tTotal dans les paramètres de ta méthode Service et DAO !
                    heuresTravailService.sauvegarderHeures(utilisateurAffiche.getId(), dateCible, tDebutM, tFinM, tDebutA, tFinA, tTotal);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // --- CORRECTION : Tranches de 15 minutes ---
    private ComboBox<String> creerComboBoxTemps(String moment) {
        ComboBox<String> cb = new ComboBox<>();
        if (moment.equals("matin")){
            for (int h = 6; h <= 14; h++) {
                cb.getItems().add(String.format("%02d:00", h));
                cb.getItems().add(String.format("%02d:15", h));
                cb.getItems().add(String.format("%02d:30", h));
                cb.getItems().add(String.format("%02d:45", h));
            }
        } else {
            for (int h = 14; h <= 20; h++) {
                cb.getItems().add(String.format("%02d:00", h));
                cb.getItems().add(String.format("%02d:15", h));
                cb.getItems().add(String.format("%02d:30", h));
                cb.getItems().add(String.format("%02d:45", h));
            }
        }


        cb.setEditable(true);
        cb.setPrefWidth(100);
        return cb;
    }

    private long calculerDureeMinutes(String debut, String fin) {
        try {
            LocalTime tDebut = LocalTime.parse(debut);
            LocalTime tFin = LocalTime.parse(fin);
            if (tFin.isBefore(tDebut)) return 0;
            return Duration.between(tDebut, tFin).toMinutes();
        } catch (Exception e) {
            return 0;
        }
    }

    // ==========================================
    // GESTION DE L'AFFICHAGE DES BLOCS PLANNING
    // ==========================================

    /**
     * Affiche les événements et demandes en attente dans la grille hebdomadaire.
     */
    public void showEvent() {
        gridPlanning.getChildren().removeIf(node -> node.getStyleClass().contains("event-block"));
        LocalDate dateFinSemaine = dateDebutSemaineAffichee.plusDays(6);

        for (Planning event : events) {
            LocalDate dateCourante = event.getJourDebut().toLocalDate();
            LocalDate dateFinEvent = event.getJourFin().toLocalDate();

            LocalDate derniereDateVisible = dateFinEvent.isAfter(dateFinSemaine) ? dateFinSemaine : dateFinEvent;

            while (!dateCourante.isAfter(dateFinEvent)) {
                if (!dateCourante.isBefore(dateDebutSemaineAffichee) && !dateCourante.isAfter(dateFinSemaine)) {
                    boolean estDerniereCaseVisible = dateCourante.isEqual(derniereDateVisible);
                    placerEvenementDansGrille(event, dateCourante, estDerniereCaseVisible);
                }
                dateCourante = dateCourante.plusDays(1);
            }
        }

        if (estCalendrierEmploye()) {
            for (DemandeConge demande : demandesEnAttente) {
                LocalDate dateCourante = demande.getJourDebut().toLocalDate();
                LocalDate dateFinDemande = demande.getJourFin().toLocalDate();

                while (!dateCourante.isAfter(dateFinDemande)) {
                    if (!dateCourante.isBefore(dateDebutSemaineAffichee) && !dateCourante.isAfter(dateFinSemaine)) {
                        placerDemandeEnAttenteDansGrille(demande, dateCourante);
                    }
                    dateCourante = dateCourante.plusDays(1);
                }
            }
        }
    }

    private void placerEvenementDansGrille(Planning event, LocalDate date, boolean afficherBoutons) {
        int col = date.getDayOfWeek().getValue();
        int hDebut = date.isEqual(event.getJourDebut().toLocalDate()) ? event.getJourDebut().getHour() : 8;
        int hFin = date.isEqual(event.getJourFin().toLocalDate()) ? event.getJourFin().getHour() : 18;

        int rowDebut = hDebut - 5;
        int rowSpan = Math.max(1, hFin - hDebut + 1);

        Node eventBlock = creerBlocEvenement(event, afficherBoutons);
        gridPlanning.add(eventBlock, col, rowDebut, 1, rowSpan);
        GridPane.setMargin(eventBlock, new Insets(2));
    }

    private void placerDemandeEnAttenteDansGrille(DemandeConge demande, LocalDate date) {
        int col = date.getDayOfWeek().getValue();
        int hDebut = date.isEqual(demande.getJourDebut().toLocalDate()) ? demande.getJourDebut().getHour() : 8;
        int hFin = date.isEqual(demande.getJourFin().toLocalDate()) ? demande.getJourFin().getHour() : 18;

        int rowDebut = hDebut - 5;
        int rowSpan = Math.max(1, hFin - hDebut + 1);

        Node bloc = creerBlocDemandeEnAttente(demande, date);
        gridPlanning.add(bloc, col, rowDebut, 1, rowSpan);
        GridPane.setMargin(bloc, new Insets(2));
    }

    private Node creerBlocDemandeEnAttente(DemandeConge demande, LocalDate date) {
        VBox box = new VBox(2);
        box.getStyleClass().add("event-block");
        box.setPadding(new Insets(4));
        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        box.setStyle("-fx-background-color: " + CongeUtil.COULEUR_DEMANDE_EN_ATTENTE
                + "; -fx-background-radius: 5; -fx-border-color: #FB8C00; -fx-border-width: 1; -fx-border-radius: 5;");

        String textFill = ColorContrastUtil.textFillForBackground(CongeUtil.COULEUR_DEMANDE_EN_ATTENTE);
        Label lblType = new Label(CongeUtil.TYPE_CONGE_AFFICHAGE);
        lblType.setWrapText(true);
        lblType.setStyle("-fx-text-fill: " + textFill + "; -fx-font-weight: bold; -fx-font-size: 11px;");

        Label lblStatut = new Label("En attente RH");
        lblStatut.setWrapText(true);
        lblStatut.setStyle("-fx-text-fill: " + textFill + "; -fx-font-size: 10px;");

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime debutAffiche = date.isEqual(demande.getJourDebut().toLocalDate())
                ? demande.getJourDebut()
                : DemandeCongeService.debutJournee(date);
        LocalDateTime finAffiche = date.isEqual(demande.getJourFin().toLocalDate())
                ? demande.getJourFin()
                : DemandeCongeService.finJournee(date);
        Label lblTime = new Label(debutAffiche.format(tf) + " - " + finAffiche.format(tf));
        lblTime.setStyle("-fx-text-fill: " + textFill + "; -fx-font-size: 10px;");

        box.getChildren().addAll(lblType, lblStatut, lblTime);
        return box;
    }

    private Node creerBlocEvenement(Planning event, boolean afficherBoutons) {
        VBox box = new VBox(2);
        box.getStyleClass().add("event-block");
        box.setPadding(new Insets(4));
        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        String color = (event.getCouleur() != null && !event.getCouleur().isEmpty()) ? event.getCouleur() : "#ffcc00";
        box.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");

        String textFill = ColorContrastUtil.textFillForBackground(color);
        String labelStyleBold = "-fx-text-fill: " + textFill + "; -fx-font-weight: bold; -fx-font-size: 11px;";
        String labelStyleSmall = "-fx-text-fill: " + textFill + "; -fx-font-size: 10px;";

        Label lblType = new Label(CongeUtil.estTypeConge(event.getType())
                ? CongeUtil.TYPE_CONGE_AFFICHAGE
                : event.getType());
        lblType.setStyle(labelStyleBold);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(lblType, spacer);
        header.setAlignment(Pos.CENTER_LEFT);

        if (afficherBoutons && !estEvenementConge(event)) {
            String btnTextFill = textFill;
            Button btnEdit = new Button("✎");
            btnEdit.setStyle("-fx-background-color: rgba(128, 128, 128, 0.35); -fx-text-fill: " + btnTextFill + "; -fx-padding: 1 5; -fx-font-size: 10px; -fx-cursor: hand; -fx-background-radius: 3;");
            btnEdit.setOnAction(e -> modifierEvenement(event));

            Button btnSuppr = new Button("X");
            btnSuppr.setStyle("-fx-background-color: rgba(255, 0, 0, 0.6); -fx-text-fill: white; -fx-padding: 1 5; -fx-font-size: 10px; -fx-cursor: hand; -fx-background-radius: 3;");
            btnSuppr.setOnAction(e -> supprimerEvenement(event));

            HBox actionsBox = new HBox(4, btnEdit, btnSuppr);
            header.getChildren().add(actionsBox);
        }

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        Label lblTime = new Label(event.getJourDebut().format(tf) + " - " + event.getJourFin().format(tf));
        lblTime.setStyle(labelStyleSmall);

        Label lblDesc = new Label(event.getDescription());
        lblDesc.setStyle(labelStyleSmall);
        lblDesc.setWrapText(true);

        box.getChildren().addAll(header, lblTime, lblDesc);
        return box;
    }

    // ==========================================
    // ACTIONS ET FORMULAIRE (AJOUT / MODIF PLANNING)
    // ==========================================

    /**
     * Ouvre le formulaire d'ajout d'événement après vérification de la sélection employé (vue RH).
     * Liaison FXML : bouton d'ajout.
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    public void ouvrirPopupAjout(ActionEvent event) {
        if (!employeRhSelectionne()) {
            return;
        }
        afficherFormulaire(null);
    }

    /**
     * Ouvre le formulaire de demande de congés pour l'employé connecté (vue employé uniquement).
     * Liaison FXML : bouton « Demander des congés ».
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    public void ouvrirPopupDemandeConge(ActionEvent event) {
        if (!estCalendrierEmploye() || utilisateurConnecte == null) {
            return;
        }
        utilisateurAffiche = utilisateurConnecte;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Demander des congés");
        dialog.setHeaderText(null);
        dialog.setResizable(true);

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dp.getStylesheets().add(getClass().getResource("/style/popup.css").toExternalForm());
        dp.setMinWidth(460);
        dp.setPrefWidth(500);

        Button okButton = (Button) dp.lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("button-ok");
        okButton.setText("Envoyer la demande");

        VBox form = new VBox(16);
        form.setPadding(new Insets(8, 4, 4, 4));
        form.setPrefWidth(440);

        Label intro = new Label("Votre demande sera transmise à la RH pour validation.");
        intro.setWrapText(true);
        intro.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");

        DatePicker dateDebut = new DatePicker(LocalDate.now().plusDays(1));
        dateDebut.setMaxWidth(Double.MAX_VALUE);
        DatePicker dateFin = new DatePicker(LocalDate.now().plusDays(1));
        dateFin.setMaxWidth(Double.MAX_VALUE);

        TextArea commentaire = new TextArea();
        commentaire.setPromptText("Motif ou précisions (optionnel)");
        commentaire.setWrapText(true);
        commentaire.setPrefRowCount(3);
        commentaire.setMaxWidth(Double.MAX_VALUE);

        ProgressBar progressSolde = new ProgressBar(0);
        progressSolde.setMaxWidth(Double.MAX_VALUE);
        Label lblSolde = new Label("Calcul du solde…");
        lblSolde.setWrapText(true);
        lblSolde.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px;");

        Runnable actualiserSolde = () -> {
            LocalDate debut = dateDebut.getValue();
            if (debut == null || utilisateurConnecte == null) {
                return;
            }
            try {
                SoldeConge solde = demandeCongeService.calculerSoldeConge(utilisateurConnecte.getId(), debut.getYear());
                progressSolde.setProgress(solde.getRatioUtilise());
                lblSolde.setText("Solde " + solde.getAnnee() + " : "
                        + solde.getJoursRestants() + " jour(s) restant(s) sur "
                        + solde.getJoursAcquis() + " (" + solde.getJoursPris() + " pris, "
                        + solde.getJoursEnAttente() + " en attente).");
            } catch (SQLException e) {
                lblSolde.setText("Impossible de calculer le solde.");
            }
        };

        dateDebut.valueProperty().addListener((obs, oldVal, newVal) -> actualiserSolde.run());
        dateFin.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && dateDebut.getValue() != null && newVal.isBefore(dateDebut.getValue())) {
                dateFin.setValue(dateDebut.getValue());
            }
        });
        actualiserSolde.run();

        form.getChildren().addAll(
                intro,
                creerChampDemandeConge("Date de début", dateDebut),
                creerChampDemandeConge("Date de fin", dateFin),
                creerChampDemandeConge("Commentaire (optionnel)", commentaire),
                creerChampDemandeConge("Votre solde de congés", new VBox(8, progressSolde, lblSolde))
        );

        dp.setContent(form);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    LocalDate debut = dateDebut.getValue();
                    LocalDate fin = dateFin.getValue();
                    if (debut == null || fin == null) {
                        afficherErreur("Les dates sont obligatoires.");
                        return;
                    }
                    if (fin.isBefore(debut)) {
                        afficherErreur("La date de fin doit être postérieure à la date de début.");
                        return;
                    }

                    LocalDateTime start = DemandeCongeService.debutJournee(debut);
                    LocalDateTime end = DemandeCongeService.finJournee(fin);
                    demandeCongeService.creerDemande(utilisateurConnecte, start, end, commentaire.getText());
                    rechargerDonneesPlanning();
                    rafraichirCalendrier();

                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Demande envoyée");
                    info.setHeaderText(null);
                    info.setContentText("Votre demande de congés a été transmise à la RH.");
                    appliquerStyleAlert(info);
                    info.showAndWait();
                } catch (IllegalArgumentException ex) {
                    afficherErreur(ex.getMessage());
                } catch (SQLException ex) {
                    afficherErreur("Impossible d'envoyer la demande : " + ex.getMessage());
                }
            }
        });
    }

    private void modifierEvenement(Planning event) {
        if (estEvenementConge(event)) {
            afficherInfoCongeNonModifiable();
            return;
        }
        afficherFormulaire(event);
    }

    private void afficherFormulaire(Planning eventToEdit) {
        boolean isEdit = (eventToEdit != null);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier l'événement" : "Ajouter un événement");

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        appliquerStyleDialog(dp);

        // GridPane 2 colonnes
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setMinWidth(120);
        colLabel.setPrefWidth(140);
        colLabel.setHalignment(HPos.LEFT);

        ColumnConstraints colField = new ColumnConstraints();
        colField.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(colLabel, colField);

        // Champs
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Réunion", "Autre");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        TextField descField = new TextField();
        descField.getStyleClass().add("champform");

        DatePicker dDP = new DatePicker();
        DatePicker dFP = new DatePicker();

        ComboBox<String> hDF = new ComboBox<>();
        ComboBox<String> hFF = new ComboBox<>();
        for (int h = 6; h <= 22; h++) {
            hDF.getItems().add(String.format("%02d:00", h));
            hDF.getItems().add(String.format("%02d:30", h));
            hFF.getItems().add(String.format("%02d:00", h));
            hFF.getItems().add(String.format("%02d:30", h));
        }
        hDF.setEditable(false);
        hFF.setEditable(false);
        hDF.setPrefWidth(90);
        hFF.setPrefWidth(90);

        ColorPicker colorPicker = new ColorPicker(Color.web("#7298E0"));

        if (isEdit) {
            if (estEvenementConge(eventToEdit)) {
                afficherInfoCongeNonModifiable();
                return;
            }
            typeBox.setValue(eventToEdit.getType());
            descField.setText(eventToEdit.getDescription());
            dDP.setValue(eventToEdit.getJourDebut().toLocalDate());
            hDF.setValue(eventToEdit.getJourDebut().format(DateTimeFormatter.ofPattern("HH:mm")));
            dFP.setValue(eventToEdit.getJourFin().toLocalDate());
            hFF.setValue(eventToEdit.getJourFin().format(DateTimeFormatter.ofPattern("HH:mm")));
            if (eventToEdit.getCouleur() != null && !eventToEdit.getCouleur().isEmpty()) {
                try {
                    colorPicker.setValue(Color.web(eventToEdit.getCouleur()));
                } catch (Exception ignored) {}
            }
        } else {
            typeBox.setValue("Réunion");
            colorPicker.setValue(Color.web("#7298E0"));
            dDP.setValue(LocalDate.now());
            hDF.setValue("08:00");
            dFP.setValue(LocalDate.now());
            hFF.setValue("10:00");
        }

        int row = 0;
        grid.add(new Label("TYPE :"), 0, row);
        grid.add(typeBox, 1, row++);

        grid.add(new Label("DESCRIPTION :"), 0, row);
        grid.add(descField, 1, row++);

        grid.add(new Label("DÉBUT :"), 0, row);
        HBox debutBox = new HBox(8, dDP, hDF);
        HBox.setHgrow(dDP, Priority.ALWAYS);
        grid.add(debutBox, 1, row++);

        grid.add(new Label("FIN :"), 0, row);
        HBox finBox = new HBox(8, dFP, hFF);
        HBox.setHgrow(dFP, Priority.ALWAYS);
        grid.add(finBox, 1, row++);

        grid.add(new Label("COULEUR :"), 0, row);
        grid.add(colorPicker, 1, row++);

        GridPane.setHgrow(typeBox, Priority.ALWAYS);
        GridPane.setHgrow(descField, Priority.ALWAYS);

        VBox content = new VBox(12);
        Label titre = new Label(isEdit ? "MODIFIER L'ÉVÉNEMENT" : "AJOUTER UN ÉVÉNEMENT");
        titre.getStyleClass().add("popup-header-title");
        titre.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#5882D6;");
        content.getChildren().addAll(titre, grid);
        content.getStyleClass().add("popup-contenu");
        content.setPadding(new Insets(8));
        content.setFillWidth(true);

        dp.setContent(content);

        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);
        if (btnOk != null) {
            btnOk.setText(isEdit ? "Enregistrer" : "Ajouter");
            btnOk.getStyleClass().add("button-primary");
            btnOk.addEventFilter(ActionEvent.ACTION, ev -> {
                // Validation
                if (typeBox.getValue() == null || typeBox.getValue().trim().isEmpty()) {
                    afficherErreur("Le type est obligatoire.");
                    ev.consume();
                    return;
                }
                if (dDP.getValue() == null || dFP.getValue() == null) {
                    afficherErreur("Les dates de début et de fin sont obligatoires.");
                    ev.consume();
                    return;
                }
                try {
                    LocalTime tDeb = LocalTime.parse(hDF.getValue());
                    LocalTime tFin = LocalTime.parse(hFF.getValue());
                    LocalDateTime start = LocalDateTime.of(dDP.getValue(), tDeb);
                    LocalDateTime end = LocalDateTime.of(dFP.getValue(), tFin);
                    if (!end.isAfter(start)) {
                        afficherErreur("La date de fin doit être après la date de début.");
                        ev.consume();
                        return;
                    }

                    String hexColor;
                    try {
                        String raw = colorPicker.getValue().toString();
                        hexColor = "#" + raw.substring(2, 8);
                    } catch (Exception ex) {
                        hexColor = "#7298E0";
                    }

                    traiterSauvegardeEvenement(start, end, typeBox.getValue(), descField.getText(),
                            hexColor, isEdit ? eventToEdit.getId() : -1, isEdit ? eventToEdit : null);

                } catch (Exception ex) {
                    afficherErreur("Format d'heure invalide.");
                    ev.consume();
                }
            });
        }

        Button btnCancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (btnCancel != null) {
            btnCancel.setText("Annuler");
            btnCancel.getStyleClass().add("button-cancel");
        }

        dp.setPrefWidth(720);
        dp.setMinWidth(520);
        dp.setMinHeight(Region.USE_PREF_SIZE);
        dp.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dp.setMaxHeight(800);

        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }


    private VBox creerChampDemandeConge(String titre, Node champ) {
        Label lbl = new Label(titre);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569; -fx-font-size: 12px;");
        VBox bloc = new VBox(6, lbl, champ);
        return bloc;
    }

    private void afficherInfoCongeNonModifiable() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Congés");
        alert.setHeaderText(null);
        if (estCalendrierEmploye()) {
            alert.setContentText("Les congés validés ne peuvent pas être modifiés ici. "
                    + "Pour une nouvelle absence, utilisez « Demander des congés ».");
        } else {
            alert.setContentText("Les congés ne peuvent pas être modifiés depuis le calendrier. "
                    + "Utilisez le bouton « Demandes de congés » pour modifier, valider ou refuser.");
        }
        appliquerStyleAlert(alert);
        alert.showAndWait();
    }

    private void ajouterLigneForm(GridPane g, String label, Node field, int row) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-style");
        g.add(lbl, 0, row);
        g.add(field, 1, row);
    }

    private void traiterSauvegardeEvenement(LocalDateTime start, LocalDateTime end, String type, String desc,
                                             String couleur, int idExistant, Planning existant) throws SQLException {
        if (utilisateurAffiche == null) {
            afficherErreur("Sélectionnez d'abord un employé.");
            return;
        }

        try {
            if (CongeUtil.estTypeConge(type)) {
                afficherErreur("Les congés ne peuvent pas être créés ou modifiés depuis le calendrier. "
                        + "Utilisez « Demandes de congés » pour la validation RH.");
                return;
            }
            if (idExistant > 0 && existant != null) {
                Planning modifie = new Planning(idExistant, start, end, type, desc, couleur, utilisateurAffiche);
                planningService.modifierPlanning(modifie);
            } else {
                Planning nouveau = new Planning(0, start, end, type, desc, couleur, utilisateurAffiche);
                planningService.ajouterPlanning(nouveau);
            }
            rechargerDonneesPlanning();
            rafraichirCalendrier();
        } catch (IllegalArgumentException ex) {
            afficherErreur(ex.getMessage());
        } catch (RuntimeException ex) {
            afficherErreur(ex.getMessage());
        }
    }

    private void supprimerEvenement(Planning event) {
        if (estEvenementConge(event)) {
            afficherInfoCongeNonModifiable();
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer cet événement ?", ButtonType.YES, ButtonType.NO);
        appliquerStyleAlert(a);

        a.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    planningService.supprimerPlanning(event.getId());
                    rechargerDonneesPlanning();
                    rafraichirCalendrier();
                } catch (IllegalArgumentException ex) {
                    afficherErreur(ex.getMessage());
                } catch (RuntimeException ex) {
                    afficherErreur(ex.getMessage());
                } catch (SQLException e) {
                    afficherErreur("Impossible de supprimer l'événement : " + e.getMessage());
                }
            }
        });
    }

    private void appliquerStyleAlert(Alert alert) {
        DialogPane dp = alert.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/style/popup.css").toExternalForm());
        Button btnOk = (Button) dp.lookupButton(ButtonType.YES);
        if (btnOk == null) btnOk = (Button) dp.lookupButton(ButtonType.OK);
        if (btnOk != null) btnOk.getStyleClass().add("button-ok");

        Button btnNo = (Button) dp.lookupButton(ButtonType.NO);
        if (btnNo == null) btnNo = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (btnNo != null) btnNo.getStyleClass().add("button-cancel");
    }
}
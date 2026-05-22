package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.HeuresTravail;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.HeuresTravailService;
import com.eseo.steevejobs.service.PlanningService;
import com.eseo.steevejobs.service.SessionService;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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

public class CalendrierController {

    // --- STYLES ---
    private static final String STYLE_ACTIF = "-fx-background-color: #e1f5fe; -fx-text-fill: #01579b; -fx-border-color: #01579b; -fx-border-radius: 20; -fx-background-radius: 20; -fx-font-size: 12; -fx-cursor: hand;";
    private static final String STYLE_INACTIF = "-fx-background-color: #e0e0e0; -fx-text-fill: #a0a0a0; -fx-background-radius: 20; -fx-font-size: 12;";

    @FXML
    private Label lundiLabel, mardiLabel, mercrediLabel, jeudiLabel, vendrediLabel, samediLabel, dimancheLabel;
    @FXML
    private Button lundiHeuresBtn, mardiHeuresBtn, mercrediHeuresBtn, jeudiHeuresBtn, vendrediHeuresBtn, samediHeuresBtn, dimancheHeuresBtn;
    @FXML
    private Label labelSemaine;
    @FXML
    private DatePicker datePickerSemaine;
    @FXML
    private GridPane gridPlanning;

    private LocalDate dateDebutSemaineAffichee;
    private List<Planning> events;
    private User utilisateur;
    private PlanningService planningService;
    private HeuresTravailService heuresTravailService;


    // ==========================================
    // INITIALISATION
    // ==========================================

    @FXML
    public void initialize() throws SQLException {
        PlanningDAO planningDAO = new PlanningDAO();
        planningService = new PlanningService(planningDAO);
        heuresTravailService = new HeuresTravailService();
        utilisateur = SessionService.getUtilisateurConnecte();

        events = initEvent();
        dateDebutSemaineAffichee = LocalDate.now().with(DayOfWeek.MONDAY);
        rafraichirCalendrier();

        datePickerSemaine.setOnAction(event -> {
            LocalDate dateChoisie = datePickerSemaine.getValue();
            if (dateChoisie != null) {
                dateDebutSemaineAffichee = dateChoisie.with(DayOfWeek.MONDAY);
                try { rafraichirCalendrier(); } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    public List<Planning> initEvent() throws SQLException {
        return planningService.obtenirPlanningsParUtilisateur(utilisateur.getId());
    }

    public void nextWeek(ActionEvent e) throws SQLException {
        dateDebutSemaineAffichee = dateDebutSemaineAffichee.plusWeeks(1); rafraichirCalendrier();
    }
    public void lastWeek(ActionEvent e) throws SQLException {
        dateDebutSemaineAffichee = dateDebutSemaineAffichee.minusWeeks(1); rafraichirCalendrier();
    }

    @FXML
    public void rafraichirCalendrier() throws SQLException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH);
        Label[] labels = {lundiLabel, mardiLabel, mercrediLabel, jeudiLabel, vendrediLabel, samediLabel, dimancheLabel};
        Button[] boutonsHeures = {lundiHeuresBtn, mardiHeuresBtn, mercrediHeuresBtn, jeudiHeuresBtn, vendrediHeuresBtn, samediHeuresBtn, dimancheHeuresBtn};

        LocalDate aujourdhui = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            LocalDate dateJour = dateDebutSemaineAffichee.plusDays(i);
            labels[i].setText(dateJour.format(dtf).toUpperCase());

            // Vérification : Futur
            if (dateJour.isAfter(aujourdhui)) {
                boutonsHeures[i].setDisable(true);
                boutonsHeures[i].setStyle(STYLE_INACTIF);
            } else {
                boutonsHeures[i].setDisable(false);
                boutonsHeures[i].setStyle(STYLE_ACTIF);
            }
        }

        DateTimeFormatter sf = DateTimeFormatter.ofPattern("dd MMMM", Locale.FRENCH);
        labelSemaine.setText("Semaine du " + dateDebutSemaineAffichee.format(sf) + " au " + dateDebutSemaineAffichee.plusDays(6).format(sf));

        showEvent();
    }

    // ==========================================
    // GESTION DES HEURES DE TRAVAIL
    // ==========================================

    @FXML
    public void ouvrirPopupHeures(ActionEvent event) {
        Node source = (Node) event.getSource();
        int dayIndex = Integer.parseInt(source.getUserData().toString());
        LocalDate dateCible = dateDebutSemaineAffichee.plusDays(dayIndex);

        // --- DÉTECTION DU MODE LECTURE SEULE ---
        boolean isReadOnly = dateCible.isBefore(LocalDate.now().minusDays(7));

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isReadOnly ? "Consultation des heures" : "Saisie des heures");
        dialog.setHeaderText("Heures du " + dateCible.format(DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH)));

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dp.getStylesheets().add(getClass().getResource("/style/popup.css").toExternalForm());

        // 1. récupération BDD
        HeuresTravail hr = null;
        try {
            hr = heuresTravailService.getHeuresParDate(utilisateur.getId(), dateCible);
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
                    heuresTravailService.sauvegarderHeures(utilisateur.getId(), dateCible, tDebutM, tFinM, tDebutA, tFinA, tTotal);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // --- CORRECTION : Tranches de 15 minutes ---
    private ComboBox<String> creerComboBoxTemps(String moment) {
        ComboBox<String> cb = new ComboBox<>();
        if (moment == "matin"){
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

    private Node creerBlocEvenement(Planning event, boolean afficherBoutons) {
        VBox box = new VBox(2);
        box.getStyleClass().add("event-block");
        box.setPadding(new Insets(4));
        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        String color = (event.getCouleur() != null && !event.getCouleur().isEmpty()) ? event.getCouleur() : "#ffcc00";
        box.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");

        Label lblType = new Label(event.getType());
        lblType.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(lblType, spacer);
        header.setAlignment(Pos.CENTER_LEFT);

        if (afficherBoutons) {
            Button btnEdit = new Button("✎");
            btnEdit.setStyle("-fx-background-color: rgba(255, 255, 255, 0.4); -fx-text-fill: white; -fx-padding: 1 5; -fx-font-size: 10px; -fx-cursor: hand; -fx-background-radius: 3;");
            btnEdit.setOnAction(e -> modifierEvenement(event));

            Button btnSuppr = new Button("X");
            btnSuppr.setStyle("-fx-background-color: rgba(255, 0, 0, 0.6); -fx-text-fill: white; -fx-padding: 1 5; -fx-font-size: 10px; -fx-cursor: hand; -fx-background-radius: 3;");
            btnSuppr.setOnAction(e -> supprimerEvenement(event));

            HBox actionsBox = new HBox(4, btnEdit, btnSuppr);
            header.getChildren().add(actionsBox);
        }

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        Label lblTime = new Label(event.getJourDebut().format(tf) + " - " + event.getJourFin().format(tf));
        lblTime.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");

        Label lblDesc = new Label(event.getDescription());
        lblDesc.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        lblDesc.setWrapText(true);

        box.getChildren().addAll(header, lblTime, lblDesc);
        return box;
    }

    // ==========================================
    // ACTIONS ET FORMULAIRE (AJOUT / MODIF PLANNING)
    // ==========================================

    @FXML
    public void ouvrirPopupAjout(ActionEvent event) {
        afficherFormulaire(null);
    }

    private void modifierEvenement(Planning event) {
        afficherFormulaire(event);
    }

    private void afficherFormulaire(Planning eventToEdit) {
        boolean isEdit = (eventToEdit != null);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier l'événement" : "Ajouter un événement");

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().add(ButtonType.OK);
        dp.getStylesheets().add(getClass().getResource("/style/popup.css").toExternalForm());

        Button okButton = (Button) dp.lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("button-ok");
        okButton.setText(isEdit ? "Enregistrer" : "Ajouter");

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(15); grid.setPrefWidth(450);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Cours", "Réunion", "Vacances", "Autre");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        ColorPicker colorPicker = new ColorPicker();

        TextField descField = new TextField();
        DatePicker dDP = new DatePicker();
        TextField hDF = new TextField(); hDF.setPrefWidth(80);
        DatePicker dFP = new DatePicker();
        TextField hFF = new TextField(); hFF.setPrefWidth(80);

        if (isEdit) {
            typeBox.setValue(eventToEdit.getType());
            descField.setText(eventToEdit.getDescription());
            dDP.setValue(eventToEdit.getJourDebut().toLocalDate());
            hDF.setText(eventToEdit.getJourDebut().format(DateTimeFormatter.ofPattern("HH:mm")));
            dFP.setValue(eventToEdit.getJourFin().toLocalDate());
            hFF.setText(eventToEdit.getJourFin().format(DateTimeFormatter.ofPattern("HH:mm")));

            if (eventToEdit.getCouleur() != null) {
                colorPicker.setValue(Color.web(eventToEdit.getCouleur()));
            }
        } else {
            typeBox.setValue("Réunion");
            colorPicker.setValue(Color.web("#7298E0"));
            dDP.setValue(LocalDate.now());
            hDF.setText("08:00");
            dFP.setValue(LocalDate.now());
            hFF.setText("10:00");
        }

        ajouterLigneForm(grid, "TYPE :", typeBox, 0);
        ajouterLigneForm(grid, "DESCRIPTION :", descField, 1);
        ajouterLigneForm(grid, "DÉBUT :", new HBox(10, dDP, hDF), 2);
        ajouterLigneForm(grid, "FIN :", new HBox(10, dFP, hFF), 3);
        ajouterLigneForm(grid, "COULEUR :", colorPicker, 4);

        dp.setContent(grid);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    LocalDateTime start = LocalDateTime.of(dDP.getValue(), LocalTime.parse(hDF.getText()));
                    LocalDateTime end = LocalDateTime.of(dFP.getValue(), LocalTime.parse(hFF.getText()));

                    String hexColor = "#" + colorPicker.getValue().toString().substring(2, 8);

                    traiterSauvegardeEvenement(start, end, typeBox.getValue(), descField.getText(), hexColor, isEdit ? eventToEdit.getId() : -1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void ajouterLigneForm(GridPane g, String label, Node field, int row) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("label-style");
        g.add(lbl, 0, row);
        g.add(field, 1, row);
    }

    private void traiterSauvegardeEvenement(LocalDateTime start, LocalDateTime end, String type, String desc, String couleur, int idToDelete) throws SQLException {
        if (idToDelete != -1) {
            planningService.supprimerPlanning(idToDelete);
        }

        PlanningDAO dao = new PlanningDAO();
        Planning newEvent = new Planning(0, start, end, type, desc, couleur, utilisateur);
        dao.createPlanning(newEvent);

        events = initEvent();
        rafraichirCalendrier();
    }

    private void supprimerEvenement(Planning event) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer cet événement ?", ButtonType.YES, ButtonType.NO);
        appliquerStyleAlert(a);

        a.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    PlanningDAO dao = new PlanningDAO();
                    dao.deletePlanning(event.getId());
                    events = initEvent();
                    rafraichirCalendrier();
                } catch (SQLException e) { e.printStackTrace(); }
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
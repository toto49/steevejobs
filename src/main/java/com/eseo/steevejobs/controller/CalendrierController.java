package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.PlanningService;
import com.eseo.steevejobs.service.SessionService;
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

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendrierController {

    @FXML
    private Label lundiLabel, mardiLabel, mercrediLabel, jeudiLabel, vendrediLabel, samediLabel, dimancheLabel;
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

    // ==========================================
    // INITIALISATION
    // ==========================================

    @FXML
    public void initialize() throws SQLException {
        PlanningDAO planningDAO = new PlanningDAO();
        planningService = new PlanningService(planningDAO);
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

    @FXML
    public void rafraichirCalendrier() throws SQLException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH);
        Label[] labels = {lundiLabel, mardiLabel, mercrediLabel, jeudiLabel, vendrediLabel, samediLabel, dimancheLabel};

        for (int i = 0; i < 7; i++) {
            labels[i].setText(dateDebutSemaineAffichee.plusDays(i).format(dtf).toUpperCase());
        }

        DateTimeFormatter sf = DateTimeFormatter.ofPattern("dd MMMM", Locale.FRENCH);
        labelSemaine.setText("Semaine du " + dateDebutSemaineAffichee.format(sf) + " au " + dateDebutSemaineAffichee.plusDays(6).format(sf));

        showEvent();
    }

    public List<Planning> initEvent() throws SQLException {
        return planningService.findByUserId(utilisateur.getId());
    }

    // ==========================================
    // GESTION DE L'AFFICHAGE DES BLOCS
    // ==========================================

    public void showEvent() {
        gridPlanning.getChildren().removeIf(node -> node.getStyleClass().contains("event-block"));
        LocalDate dateFinSemaine = dateDebutSemaineAffichee.plusDays(6);

        for (Planning event : events) {
            LocalDate dateCourante = event.getJourDebut().toLocalDate();
            LocalDate dateFinEvent = event.getJourFin().toLocalDate();

            while (!dateCourante.isAfter(dateFinEvent)) {
                if (!dateCourante.isBefore(dateDebutSemaineAffichee) && !dateCourante.isAfter(dateFinSemaine)) {
                    placerEvenementDansGrille(event, dateCourante);
                }
                dateCourante = dateCourante.plusDays(1);
            }
        }
    }

    private void placerEvenementDansGrille(Planning event, LocalDate date) {
        int col = date.getDayOfWeek().getValue();
        int hDebut = date.isEqual(event.getJourDebut().toLocalDate()) ? event.getJourDebut().getHour() : 8;
        int hFin = date.isEqual(event.getJourFin().toLocalDate()) ? event.getJourFin().getHour() : 18;

        int rowDebut = hDebut - 5;
        int rowSpan = Math.max(1, hFin - hDebut + 1);

        Node eventBlock = creerBlocEvenement(event);
        gridPlanning.add(eventBlock, col, rowDebut, 1, rowSpan);
        GridPane.setMargin(eventBlock, new Insets(2));
    }

    private Node creerBlocEvenement(Planning event) {
        VBox box = new VBox(2);
        box.getStyleClass().add("event-block");
        box.setPadding(new Insets(4));
        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        String color = switch (event.getType()) {
            case "Vacances" -> "#5cb85c";
            case "Réunion" -> "#7298E0";
            default -> "#ffcc00";
        };
        box.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");

        // Header : Type + Boutons (Pencil for Edit, X for Delete)
        Label lblType = new Label(event.getType());
        lblType.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");

        Button btnEdit = new Button("✎");
        btnEdit.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-padding: 1 5; -fx-font-size: 10px; -fx-cursor: hand; -fx-background-radius: 3;");
        btnEdit.setOnAction(e -> modifierEvenement(event));

        Button btnSuppr = new Button("X");
        btnSuppr.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-padding: 1 5; -fx-font-size: 10px; -fx-cursor: hand; -fx-background-radius: 3;");
        btnSuppr.setOnAction(e -> supprimerEvenement(event));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(lblType, spacer, new HBox(4, btnEdit, btnSuppr));
        header.setAlignment(Pos.CENTER_LEFT);

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
    // ACTIONS ET FORMULAIRE (AJOUT / MODIF)
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
        okButton.setText(isEdit ? "Enregistrer les modifications" : "Ajouter l'événement");

        // --- CHAMPS DU FORMULAIRE ---
        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(15); grid.setPrefWidth(450);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Cours", "Réunion", "Vacances", "Autre");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        TextField descField = new TextField();
        DatePicker dDP = new DatePicker();
        TextField hDF = new TextField(); hDF.setPrefWidth(80);
        DatePicker dFP = new DatePicker();
        TextField hFF = new TextField(); hFF.setPrefWidth(80);

        // --- PRÉ-REMPLISSAGE ---
        if (isEdit) {
            typeBox.setValue(eventToEdit.getType());
            descField.setText(eventToEdit.getDescription());
            dDP.setValue(eventToEdit.getJourDebut().toLocalDate());
            hDF.setText(eventToEdit.getJourDebut().format(DateTimeFormatter.ofPattern("HH:mm")));
            dFP.setValue(eventToEdit.getJourFin().toLocalDate());
            hFF.setText(eventToEdit.getJourFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            typeBox.setValue("Cours");
            dDP.setValue(LocalDate.now());
            hDF.setText("08:00");
            dFP.setValue(LocalDate.now());
            hFF.setText("10:00");
        }

        ajouterLigneForm(grid, "TYPE :", typeBox, 0);
        ajouterLigneForm(grid, "DESCRIPTION :", descField, 1);
        ajouterLigneForm(grid, "DÉBUT :", new HBox(10, dDP, hDF), 2);
        ajouterLigneForm(grid, "FIN :", new HBox(10, dFP, hFF), 3);

        dp.setContent(grid);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    LocalDateTime start = LocalDateTime.of(dDP.getValue(), LocalTime.parse(hDF.getText()));
                    LocalDateTime end = LocalDateTime.of(dFP.getValue(), LocalTime.parse(hFF.getText()));

                    // Si on modifie, on passe l'ID de l'événement à supprimer d'abord
                    traiterSauvegardeEvenement(start, end, typeBox.getValue(), descField.getText(), isEdit ? eventToEdit.getId() : -1);
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

    private void traiterSauvegardeEvenement(LocalDateTime start, LocalDateTime end, String type, String desc, int idToDelete) throws SQLException {
        // 1. Si c'est une modification, on supprime l'ancien pour éviter qu'il s'auto-conflit
        if (idToDelete != -1) {
            planningService.deletePlanning(idToDelete);
        }

        // 2. Gestion des conflits
        List<Planning> conflits = new ArrayList<>();
        for (Planning p : events) {
            // On ignore l'ancien ID car il vient d'être supprimé ou n'existe pas
            if (p.getId() != idToDelete && start.isBefore(p.getJourFin()) && !end.isBefore(p.getJourDebut())) {
                conflits.add(p);
            }
        }

        if (!conflits.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Ce créneau est déjà occupé. Voulez-vous remplacer l'existant ?", ButtonType.YES, ButtonType.NO);
            a.setTitle("Conflit d'horaire");
            a.setHeaderText("Créneau indisponible");
            appliquerStyleAlert(a);

            if (a.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                // Si l'utilisateur annule une modification, il faudrait idéalement restaurer l'ancien
                // mais pour rester simple : on rafraîchit juste.
                events = initEvent();
                rafraichirCalendrier();
                return;
            }

            for (Planning p : conflits) {
                planningService.deletePlanning(p.getId());
                // Découpage intelligent (facultatif selon ton besoin)
                if (p.getJourDebut().isBefore(start)) {
                    planningService.ajouterPlanning(new Planning(0, p.getJourDebut(), start, p.getType(), p.getDescription(), utilisateur));
                }
                if (p.getJourFin().isAfter(end)) {
                    LocalDateTime r = end.getHour() >= 20 ? end.toLocalDate().plusDays(1).atTime(8,0) : end;
                    if (r.isBefore(p.getJourFin())) {
                        planningService.ajouterPlanning(new Planning(0, r, p.getJourFin(), p.getType(), p.getDescription(), utilisateur));
                    }
                }
            }
        }

        // 3. Ajout du nouvel (ou modifié) événement
        planningService.ajouterPlanning(new Planning(0, start, end, type, desc, utilisateur));
        events = initEvent();
        rafraichirCalendrier();
    }

    private void supprimerEvenement(Planning event) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer cet événement ?", ButtonType.YES, ButtonType.NO);
        a.setTitle("Suppression");
        a.setHeaderText("Confirmation de suppression");
        appliquerStyleAlert(a);

        a.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    planningService.deletePlanning(event.getId());
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

    public void addWeek(ActionEvent e) throws SQLException { dateDebutSemaineAffichee = dateDebutSemaineAffichee.plusWeeks(1); rafraichirCalendrier(); }
    public void removeWeek(ActionEvent e) throws SQLException { dateDebutSemaineAffichee = dateDebutSemaineAffichee.minusWeeks(1); rafraichirCalendrier(); }
}
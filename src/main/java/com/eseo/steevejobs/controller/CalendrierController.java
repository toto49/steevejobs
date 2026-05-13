package com.eseo.steevejobs.controller;
import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.PlanningService;
import com.eseo.steevejobs.service.SessionService;
import com.sun.javafx.sg.prism.NGGroup;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CalendrierController {

    @FXML
    private Label lundiLabel;
    @FXML
    private Label mardiLabel;
    @FXML
    private Label mercrediLabel;
    @FXML
    private Label jeudiLabel;
    @FXML
    private Label vendrediLabel;
    @FXML
    private Label samediLabel;
    @FXML
    private Label dimancheLabel;
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

    @FXML
    public void initialize() throws SQLException {
        // Initialisation des services
        PlanningDAO planningDAO = new PlanningDAO();
        planningService = new PlanningService(planningDAO);
        utilisateur = SessionService.getUtilisateurConnecte();
        // Récupération des rdv
        events = initEvent();
        // Initialisation de base (Aujourd'hui)
        dateDebutSemaineAffichee = LocalDate.now().with(DayOfWeek.MONDAY);
        rafraichirCalendrier();

        // On écoute le changement de date du DatePicker
        datePickerSemaine.setOnAction(event -> {
            LocalDate dateChoisie = datePickerSemaine.getValue();
            if (dateChoisie != null) {
                dateDebutSemaineAffichee = dateChoisie.with(DayOfWeek.MONDAY);
                try {
                    rafraichirCalendrier();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @FXML
    public void rafraichirCalendrier() throws SQLException {

        DateTimeFormatter formatter_jour = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH);
        DateTimeFormatter formatter_semaine = DateTimeFormatter.ofPattern("dd MMMM", Locale.FRENCH);

        lundiLabel.setText(dateDebutSemaineAffichee.format(formatter_jour).toUpperCase());
        mardiLabel.setText(dateDebutSemaineAffichee.plusDays(1).format(formatter_jour).toUpperCase());
        mercrediLabel.setText(dateDebutSemaineAffichee.plusDays(2).format(formatter_jour).toUpperCase());
        jeudiLabel.setText(dateDebutSemaineAffichee.plusDays(3).format(formatter_jour).toUpperCase());
        vendrediLabel.setText(dateDebutSemaineAffichee.plusDays(4).format(formatter_jour).toUpperCase());
        samediLabel.setText(dateDebutSemaineAffichee.plusDays(5).format(formatter_jour).toUpperCase());
        dimancheLabel.setText(dateDebutSemaineAffichee.plusDays(6).format(formatter_jour).toUpperCase());

        datePickerSemaine.setAccessibleText("Semaine du " + dateDebutSemaineAffichee.format(formatter_jour));

        labelSemaine.setText("Semaine du " + dateDebutSemaineAffichee.format(formatter_semaine) + " au " + dateDebutSemaineAffichee.plusDays(6).format(formatter_semaine));

        showEvent();
    }

    public List<Planning> initEvent() throws SQLException {
        return planningService.findByUserId(utilisateur.getId());
    }

    public void showEvent() {
        // Nettoyage des anciens événements
        gridPlanning.getChildren().removeIf(node -> node.getStyleClass().contains("event-block"));

        LocalDate dateFinSemaine = dateDebutSemaineAffichee.plusDays(6);

        for (Planning event : events) {
            LocalDate dateDebutEvent = event.getJourDebut().toLocalDate();
            LocalDate dateFinEvent = event.getJourFin().toLocalDate();

            // On boucle sur chaque jour que dure l'événement
            LocalDate dateCourante = dateDebutEvent;

            while (!dateCourante.isAfter(dateFinEvent)) {

                // On affiche le bloc uniquement si le jour courant est dans la semaine affichée
                if (!dateCourante.isBefore(dateDebutSemaineAffichee) && !dateCourante.isAfter(dateFinSemaine)) {

                    int col = dateCourante.getDayOfWeek().getValue(); // Lundi = 1 ... Dimanche = 7

                    // --- Gestion de l'heure de début ---
                    int heureDebut;
                    if (dateCourante.isEqual(dateDebutEvent)) {
                        heureDebut = event.getJourDebut().getHour(); // C'est le 1er jour : on prend la vraie heure
                    } else {
                        heureDebut = 7; // Jour intermédiaire/fin : on force à 7h
                    }

                    // --- Gestion de l'heure de fin ---
                    int heureFin;
                    if (dateCourante.isEqual(dateFinEvent)) {
                        heureFin = event.getJourFin().getHour(); // C'est le dernier jour : on prend la vraie heure
                    } else {
                        heureFin = 19; // Jour intermédiaire/début : on force à 19h
                    }

                    // --- Calcul des lignes (6h00 = ligne 1) ---
                    int rowDebut = heureDebut - 5;
                    int rowSpan = heureFin - heureDebut + 1;

                    if (rowSpan <= 0) rowSpan = 1; // Sécurité visuelle minimum

                    // --- Création du composant ---
                    Label eventBlock = new Label(event.getType());
                    eventBlock.getStyleClass().add("event-block");
                    eventBlock.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                    // --- Modification du style en fonction du type ---
                    if (event.getType().equals("Vacances")) {
                        eventBlock.setStyle("-fx-background-color: #5cb85c; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: center; -fx-background-radius: 5;");
                    }else if (event.getType().equals("Réunion")){
                        eventBlock.setStyle("-fx-background-color: #7298E0; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: center; -fx-background-radius: 5;");
                    }else{
                        eventBlock.setStyle("-fx-background-color: #ffcc00; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: center; -fx-background-radius: 5;");
                    }

                    // Ajout au GridPane (colspan = 1 car un bloc = un jour)
                    gridPlanning.add(eventBlock, col, rowDebut, 1, rowSpan);
                    GridPane.setMargin(eventBlock, new Insets(2));
                }

                // On passe au jour suivant pour la boucle
                dateCourante = dateCourante.plusDays(1);
            }
        }
    }

    public void addWeek(ActionEvent event) throws SQLException {
        dateDebutSemaineAffichee = dateDebutSemaineAffichee.plusWeeks(1);
        rafraichirCalendrier();

    }

    public void removeWeek(ActionEvent event) throws SQLException {
        dateDebutSemaineAffichee = dateDebutSemaineAffichee.minusWeeks(1);
        rafraichirCalendrier();
    }

    @FXML
    public void ouvrirPopupAjout(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un événement");
        dialog.setHeaderText("Détails du nouvel événement");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Cours", "Réunion", "Vacances", "Autre");
        typeBox.setValue("Cours");

        // CORRECTION : DatePicker utilise LocalDate.now()
        DatePicker dateDebutPicker = new DatePicker(LocalDate.now());
        TextField heureDebutField = new TextField("08:00"); // Ajout du champ d'heure

        DatePicker dateFinPicker = new DatePicker(LocalDate.now());
        TextField heureFinField = new TextField("10:00"); // Ajout du champ d'heure

        grid.add(new Label("Type :"), 0, 0);
        grid.add(typeBox, 1, 0);

        grid.add(new Label("Début :"), 0, 1);
        grid.add(dateDebutPicker, 1, 1);
        grid.add(heureDebutField, 2, 1);

        grid.add(new Label("Fin :"), 0, 2);
        grid.add(dateFinPicker, 1, 2);
        grid.add(heureFinField, 2, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String type = typeBox.getValue();

                    // Combinaison de la date (DatePicker) et de l'heure (TextField)
                    LocalTime timeDebut = LocalTime.parse(heureDebutField.getText());
                    LocalDateTime dateDebut = LocalDateTime.of(dateDebutPicker.getValue(), timeDebut);

                    LocalTime timeFin = LocalTime.parse(heureFinField.getText());
                    LocalDateTime dateFin = LocalDateTime.of(dateFinPicker.getValue(), timeFin);

                    Planning evenement = new Planning(0, dateDebut, dateFin, type, utilisateur);
                    planningService.ajouterPlanning(evenement);

                    // On rafraîchit le calendrier avec les nouvelles données
                    events = planningService.findByUserId(utilisateur.getId());
                    rafraichirCalendrier();

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        });
    }

}

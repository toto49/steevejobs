package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.TicketService;
import com.eseo.steevejobs.service.TicketServiceImpl;
import com.eseo.steevejobs.service.UserService;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class TicketsListController implements ParametrizedController {

    private final TicketService ticketService = new TicketServiceImpl();
    private UserService userService;
    private SessionService sessionService;
    private User currentUser;

    @FXML
    private VBox ticketsContainer;

    private List<Ticket> tousLesTicketsBDD;

    @FXML
    public void initialize() {
        try {
            this.userService = new UserService();
            this.sessionService = new SessionService();
            this.currentUser = SessionService.getUtilisateurConnecte();

            // On charge TOUS les tickets de la BDD une seule fois
            this.tousLesTicketsBDD = ticketService.getAllTickets();

            // Par défaut (si on n'est pas passé par initData), on affiche "Mes Tickets"
            afficherMesTickets();

        } catch (RuntimeException e) {
            System.err.println("Erreur lors du chargement : " + e.getMessage());
        }
    }

    private void afficherMesTickets() {
        if (tousLesTicketsBDD == null) return;

        List<Ticket> mesTickets = tousLesTicketsBDD.stream()
                .filter(t -> t.getAuteur() != null && t.getAuteur().getId() == currentUser.getId())
                .collect(Collectors.toList());

        remplirLeContainer(mesTickets);
    }

    @Override
    public void initData(String parametreService) {
        if (tousLesTicketsBDD == null) return;

        List<Ticket> ticketsFiltres = tousLesTicketsBDD.stream()
                .filter(t -> (t.getService() != null && t.getService().equalsIgnoreCase(parametreService))
                        || (t.getAuteur() != null && t.getAuteur().getId() == currentUser.getId()))
                .collect(Collectors.toList());

        remplirLeContainer(ticketsFiltres);
    }

    private void remplirLeContainer(List<Ticket> liste) {
        ticketsContainer.getChildren().clear();
        for (Ticket ticket : liste) {
            String dateAffichee = ticketService.formatTicketDate(ticket.getDateOuverture());
            addTicketCard(
                    String.valueOf(ticket.getId()),
                    ticket.getSujet() != null ? ticket.getSujet() : "Sans sujet",
                    ticket.getStatut().name(),
                    dateAffichee
            );
        }
    }

    private void addTicketCard(String id, String subject, String status, String date) {
        HBox card = new HBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: black; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 15;");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setSpacing(20);
        card.setOnMouseClicked(event -> handleOpenTicket(id));

        VBox infoBox = new VBox(5);
        Label idLabel = new Label("TICKET N°" + id);
        idLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: black; -fx-font-weight: bold;");
        Label subjectLabel = new Label(subject);
        subjectLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");
        infoBox.getChildren().addAll(idLabel, subjectLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox metaBox = new VBox(5);
        metaBox.setAlignment(Pos.CENTER_RIGHT);
        Label statusLabel = new Label("Statut: " + status);
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " +
                (status.matches("EN_ATTENTE|EN_COURS|OPEN|IN PROGRESS") ? "green;" : "red;"));

        Label dateLabel = new Label(date);
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
        metaBox.getChildren().addAll(statusLabel, dateLabel);

        card.getChildren().addAll(infoBox, spacer, metaBox);
        ticketsContainer.getChildren().add(card);
    }

    @FXML
    public void handleCreateTicket(ActionEvent event) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Créer un nouveau ticket");
        popupStage.setResizable(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        Label titreLabel = new Label("NOUVEAU TICKET");
        titreLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #5882D6;");
        VBox sujetBox = new VBox(5);
        Label sujetTitre = new Label("Sujet");
        sujetTitre.setStyle("-fx-font-weight: bold;");
        TextField sujetField = new TextField();
        sujetField.setPromptText("Ex: Mon écran ne s'allume plus");
        sujetField.setPrefHeight(35);
        sujetBox.getChildren().addAll(sujetTitre, sujetField);


        VBox serviceBox = new VBox(5);
        Label serviceTitre = new Label("Service concerné");
        serviceTitre.setStyle("-fx-font-weight: bold;");
        ComboBox<String> serviceComboBox = new ComboBox<>();
        serviceComboBox.setItems(FXCollections.observableArrayList(
                "Service Informatique", "Ressources Humaines", "Maintenance", "Comptabilité", "Autre"
        ));
        serviceComboBox.setPromptText("Sélectionnez un service...");
        serviceComboBox.setPrefHeight(35);
        serviceComboBox.setMaxWidth(Double.MAX_VALUE);
        serviceBox.getChildren().addAll(serviceTitre, serviceComboBox);


        VBox descBox = new VBox(5);
        Label descTitre = new Label("Description détaillée");
        descTitre.setStyle("-fx-font-weight: bold;");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Détaillez votre problème...");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefHeight(150);
        VBox.setVgrow(descBox, Priority.ALWAYS);
        descBox.getChildren().addAll(descTitre, descriptionArea);


        HBox boutonsBox = new HBox(15);
        boutonsBox.setAlignment(Pos.CENTER_RIGHT);
        boutonsBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnAnnuler = new Button("ANNULER");
        btnAnnuler.setStyle("-fx-background-color: transparent; -fx-text-fill: #5882D6; -fx-border-color: #5882D6; -fx-border-radius: 5; -fx-cursor: hand; -fx-padding: 8 20;");

        Button btnCreer = new Button("CRÉER");
        btnCreer.setStyle("-fx-background-color: #5882D6; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 20;");

        boutonsBox.getChildren().addAll(btnAnnuler, btnCreer);

        root.getChildren().addAll(titreLabel, sujetBox, serviceBox, descBox, boutonsBox);


        btnAnnuler.setOnAction(e -> popupStage.close());

        btnCreer.setOnAction(e -> {
            String sujet = sujetField.getText().trim();
            String service = serviceComboBox.getValue();
            String description = descriptionArea.getText().trim();

            if (sujet.isEmpty() || service == null || description.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs.");
                alert.setHeaderText(null);
                alert.showAndWait();
                return;
            }

            try {
                Ticket nouveauTicket = new Ticket();
                nouveauTicket.setSujet(sujet);
                nouveauTicket.setService(service);
                nouveauTicket.setDescription(description);
                nouveauTicket.setAuteur(SessionService.getUtilisateurConnecte());

                ticketService.creerTicket(nouveauTicket);

                popupStage.close();
                initialize();

            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de la création du ticket : " + ex.getMessage());
                alert.showAndWait();
            }
        });


        Scene scene = new Scene(root, 500, 550);
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    private void handleOpenTicket(String ticketId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/ticket-view.fxml"));
            Parent view = loader.load();
            TicketController controller = loader.getController();
            controller.initData(Integer.parseInt(ticketId));

            if (MenuController.getInstance() != null) {
                MenuController.getInstance().setCenterView(view);
                MenuController.getInstance().changerTitre("Ticket N°" + ticketId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
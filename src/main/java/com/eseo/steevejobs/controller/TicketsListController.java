package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.*;
import javafx.application.Platform;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TicketsListController implements ParametrizedController {

    private final TicketService ticketService = new TicketServiceImpl();
    private UserService userService;
    private SessionService sessionService;
    private User currentUser;

    @FXML
    private VBox ticketsContainer;
    @FXML
    private Label titlepageticket;
    private List<Ticket> tousLesTicketsBDD;
    private static TicketsListController activeInstance;
    private String filtreActuel = null;

    public static TicketsListController getActiveInstance() {
        return activeInstance;
    }

    @FXML
    public void initialize() {
        try {
            activeInstance = this;
            this.userService = new UserService();
            this.sessionService = new SessionService();
            this.currentUser = SessionService.getUtilisateurConnecte();

            this.tousLesTicketsBDD = ticketService.getAllTickets();

            afficherMesTickets();

        } catch (RuntimeException e) {
            System.err.println("Erreur lors du chargement : " + e.getMessage());
            e.printStackTrace(); // <-- AJOUTE CETTE LIGNE
        }
    }

    private void afficherMesTickets() {
        System.out.println("🟢 DEBUG : La méthode afficherMesTickets() a été déclenchée !");
        if (tousLesTicketsBDD == null) return;
        this.filtreActuel = null;

        List<Ticket> mesTickets = tousLesTicketsBDD.stream()
                .filter(t -> t.getAuteur() != null && t.getAuteur().getId() == currentUser.getId())
                .collect(Collectors.toList());

        System.out.println("🟢 DEBUG : Nombre de tickets trouvés pour l'auteur : " + mesTickets.size());
        remplirLeContainer(mesTickets);
    }

    @Override
    public void initData(String parametreService) {
        titlepageticket.setText("ticket " + parametreService);
        if (tousLesTicketsBDD == null) return;
        this.filtreActuel = parametreService;

        List<Ticket> ticketsFiltres = tousLesTicketsBDD.stream()
                .filter(t -> t.getService() != null
                        && t.getService().equalsIgnoreCase(parametreService)
                        && t.getAuteur().getId() != currentUser.getId())
                .collect(Collectors.toList());

        remplirLeContainer(ticketsFiltres);
    }


    public void rafraichirAffichage() {
        Platform.runLater(() -> {
            try {
                this.tousLesTicketsBDD = ticketService.getAllTickets();

                if (filtreActuel != null) {
                    initData(filtreActuel);
                } else {
                    afficherMesTickets();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void remplirLeContainer(List<Ticket> liste) {
        liste.sort((t1, t2) -> {
            boolean nonLu1 = false;
            boolean nonLu2 = false;

            if (WebSocketService.getInstance() != null) {
                nonLu1 = WebSocketService.getInstance().isTicketNonLu(t1.getId());
                nonLu2 = WebSocketService.getInstance().isTicketNonLu(t2.getId());
            }

            if (nonLu1 && !nonLu2) return -1;
            if (!nonLu1 && nonLu2) return 1;


            LocalDateTime date1 = t1.getDateOuverture();
            LocalDateTime date2 = t2.getDateOuverture();


            if (t1.getDateDerniereActivite() != null) date1 = t1.getDateDerniereActivite();
            if (t2.getDateDerniereActivite() != null) date2 = t2.getDateDerniereActivite();

            return date2.compareTo(date1);
        });


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
        card.setAlignment(Pos.CENTER_LEFT);
        card.setSpacing(20);
        card.setOnMouseClicked(event -> handleOpenTicket(id));

        boolean isNonLu = false;
        if (WebSocketService.getInstance() != null) {
            isNonLu = WebSocketService.getInstance().isTicketNonLu(Integer.parseInt(id));
        }

        if (isNonLu) {
            card.setStyle("-fx-background-color: #FFE5E5; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #E74C3C; -fx-border-radius: 10; -fx-border-width: 2; -fx-padding: 15;");
        } else {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: black; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 15;");
        }

        VBox infoBox = new VBox(5);
        Label idLabel = new Label("TICKET N°" + id + (isNonLu ? " (NOUVEAU MESSAGE)" : ""));

        if (isNonLu) {
            idLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #E74C3C; -fx-font-weight: bold;");
        } else {
            idLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: black; -fx-font-weight: bold;");
        }

        Label subjectLabel = new Label(subject);
        subjectLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");
        if (isNonLu) subjectLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-font-weight: bold;");

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
        sujetField.getStyleClass().add("champform");
        sujetBox.getChildren().addAll(sujetTitre, sujetField);

        VBox serviceBox = new VBox(5);
        Label serviceTitre = new Label("Service concerné");
        serviceTitre.setStyle("-fx-font-weight: bold;");
        ComboBox<String> serviceComboBox = new ComboBox<>();
        serviceComboBox.setItems(FXCollections.observableArrayList(
                "ADMIN", "RH"
        ));
        serviceComboBox.setPromptText("Sélectionnez un service...");
        serviceComboBox.setPrefHeight(35);
        serviceComboBox.setMaxWidth(Double.MAX_VALUE);
        serviceComboBox.getStyleClass().add("champform");
        serviceBox.getChildren().addAll(serviceTitre, serviceComboBox);

        VBox descBox = new VBox(5);
        Label descTitre = new Label("Description détaillée");
        descTitre.setStyle("-fx-font-weight: bold;");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Détaillez votre problème...");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefHeight(150);
        descriptionArea.getStyleClass().add("champform");
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
                if (WebSocketService.getInstance() != null) {
                    List<Integer> rhIds = userService.getIdsByRole("RH");
                    List<Integer> adminIds = userService.getIdsByRole("ADMIN");
                    java.util.Set<Integer> staffIds = new java.util.HashSet<>();
                    if (rhIds != null) staffIds.addAll(rhIds);
                    if (adminIds != null) staffIds.addAll(adminIds);
                    staffIds.remove(SessionService.getUtilisateurConnecte().getId());

                    WebSocketService.getInstance().envoyerNotificationGroupée(
                            new java.util.ArrayList<>(staffIds),
                            0,
                            "TECH"
                    );
                }

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
            activeInstance = null;

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
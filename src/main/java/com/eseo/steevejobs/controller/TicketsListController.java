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
import javafx.scene.Node;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    @FXML
    private Button btnFiltreEnCours;
    @FXML
    private Button btnFiltreArchives;
    private boolean modeArchivesActif = false;
    private boolean isFetching = false;

    public static TicketsListController getActiveInstance() {
        return activeInstance;
    }

    @FXML
    public void initialize() {
        activeInstance = this;
        this.userService = new UserService();
        this.sessionService = new SessionService();
        this.currentUser = SessionService.getUtilisateurConnecte();
        chargerTicketsBDDAsync(this::rafraichirAffichageLocal);
    }

    @Override
    public void initData(String parametreService) {
        this.filtreActuel = parametreService;
        if (titlepageticket != null) {
            titlepageticket.setText("TICKETS " + parametreService.toUpperCase());
        }

        if (tousLesTicketsBDD != null) {
            rafraichirAffichageLocal();
        } else {
            chargerTicketsBDDAsync(this::rafraichirAffichageLocal);
        }
    }

    public void afficherMesTickets() {
        this.filtreActuel = null;
        if (titlepageticket != null) {
            titlepageticket.setText("MES TICKETS");
        }

        if (tousLesTicketsBDD != null) {
            rafraichirAffichageLocal();
        } else {
            chargerTicketsBDDAsync(this::rafraichirAffichageLocal);
        }
    }

    private void chargerTicketsBDDAsync(Runnable actionApresChargement) {
        if (isFetching) return;
        isFetching = true;

        CompletableFuture.supplyAsync(() -> {
            return ticketService.getAllTickets();
        }).thenAcceptAsync(tickets -> {
            this.tousLesTicketsBDD = tickets;
            this.isFetching = false;
            if (actionApresChargement != null) {
                actionApresChargement.run();
            }
        }, Platform::runLater).exceptionally(ex -> {
            this.isFetching = false;
            ex.printStackTrace();
            return null;
        });
    }

    private void rafraichirAffichageLocal() {
        if (tousLesTicketsBDD == null) return;
        activeInstance = this;

        List<Ticket> listeAFicher;

        if (filtreActuel != null && !filtreActuel.isEmpty()) {
            listeAFicher = tousLesTicketsBDD.stream()
                    .filter(t -> t.getService() != null
                            && t.getService().equalsIgnoreCase(filtreActuel)
                            && t.getAuteur() != null
                            && t.getAuteur().getId() != currentUser.getId())
                    .filter(this::correspondAuModeActuel)
                    .collect(Collectors.toList());
        } else {
            // MODE MES TICKETS
            listeAFicher = tousLesTicketsBDD.stream()
                    .filter(t -> t.getAuteur() != null && t.getAuteur().getId() == currentUser.getId())
                    .filter(this::correspondAuModeActuel)
                    .collect(Collectors.toList());
        }

        remplirLeContainer(listeAFicher);
    }

    public void rafraichirAffichage() {
        chargerTicketsBDDAsync(this::rafraichirAffichageLocal);
    }

    private void remplirLeContainer(List<Ticket> liste) {
        liste.sort((t1, t2) -> {
            boolean jeSuisAuteur1 = (t1.getAuteur() != null && t1.getAuteur().getId() == currentUser.getId());
            boolean nonLu1 = jeSuisAuteur1 ? t1.isNonLuAuteur() : t1.isNonLuAdmin();

            boolean jeSuisAuteur2 = (t2.getAuteur() != null && t2.getAuteur().getId() == currentUser.getId());
            boolean nonLu2 = jeSuisAuteur2 ? t2.isNonLuAuteur() : t2.isNonLuAdmin();

            if (nonLu1 && !nonLu2) return -1;
            if (!nonLu1 && nonLu2) return 1;

            LocalDateTime date1 = (t1.getDateDerniereActivite() != null) ? t1.getDateDerniereActivite() : t1.getDateOuverture();
            LocalDateTime date2 = (t2.getDateDerniereActivite() != null) ? t2.getDateDerniereActivite() : t2.getDateOuverture();

            return date2.compareTo(date1);
        });

        List<Node> cartesVisuelles = new ArrayList<>();
        for (Ticket ticket : liste) {
            cartesVisuelles.add(creerTicketCard(ticket));
        }

        ticketsContainer.getChildren().setAll(cartesVisuelles);
    }

    private HBox creerTicketCard(Ticket ticket) {
        String id = String.valueOf(ticket.getId());
        HBox card = new HBox();
        card.setAlignment(Pos.CENTER_LEFT);
        card.setSpacing(20);
        card.setOnMouseClicked(event -> handleOpenTicket(id));

        boolean jeSuisAuteur = (ticket.getAuteur() != null && ticket.getAuteur().getId() == currentUser.getId());
        boolean isNonLu = jeSuisAuteur ? ticket.isNonLuAuteur() : ticket.isNonLuAdmin();

        if (isNonLu) {
            card.setStyle("-fx-background-color: #FFE5E5; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #E74C3C; -fx-border-radius: 10; -fx-border-width: 2; -fx-padding: 15;");
        } else {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #DDDDDD; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 15;");
        }

        VBox infoBox = new VBox(5);
        Label idLabel = new Label("TICKET N°" + id + (isNonLu ? " (NOUVEAU MESSAGE)" : ""));
        idLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + (isNonLu ? "#E74C3C;" : "black;"));

        Label subjectLabel = new Label(ticket.getSujet() != null ? ticket.getSujet() : "Sans sujet");
        subjectLabel.setStyle("-fx-font-size: 16px; " + (isNonLu ? "-fx-font-weight: bold; -fx-text-fill: black;" : "-fx-text-fill: #333333;"));

        infoBox.getChildren().addAll(idLabel, subjectLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox metaBox = new VBox(5);
        metaBox.setAlignment(Pos.CENTER_RIGHT);
        String statusStr = ticket.getStatut().name();
        Label statusLabel = new Label("Statut: " + statusStr);
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " +
                (statusStr.matches("EN_ATTENTE|EN_COURS|OPEN|IN_PROGRESS") ? "green;" : "red;"));

        Label dateLabel = new Label(ticketService.formatTicketDate(ticket.getDateOuverture()));
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
        metaBox.getChildren().addAll(statusLabel, dateLabel);

        card.getChildren().addAll(infoBox, spacer, metaBox);
        return card;
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
        sujetTitre.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        TextField sujetField = new TextField();
        sujetField.setPromptText("Ex: Mon écran ne s'allume plus");
        sujetField.setPrefHeight(35);
        sujetField.getStyleClass().add("champform");
        sujetBox.getChildren().addAll(sujetTitre, sujetField);

        VBox serviceBox = new VBox(5);
        Label serviceTitre = new Label("Service concerné");
        serviceTitre.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        ComboBox<String> serviceComboBox = new ComboBox<>();
        serviceComboBox.setItems(FXCollections.observableArrayList("ADMIN", "RH"));
        serviceComboBox.setPromptText("Sélectionnez un service...");
        serviceComboBox.setMaxWidth(Double.MAX_VALUE);
        serviceComboBox.getStyleClass().add("menu-burger");
        serviceBox.getChildren().addAll(serviceTitre, serviceComboBox);

        VBox descBox = new VBox(5);
        Label descTitre = new Label("Description détaillée");
        descTitre.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Détaillez votre problème...");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefHeight(150);
        descriptionArea.getStyleClass().add("textarea-form");
        VBox.setVgrow(descBox, Priority.ALWAYS);
        descBox.getChildren().addAll(descTitre, descriptionArea);

        HBox boutonsBox = new HBox(15);
        boutonsBox.setAlignment(Pos.CENTER_RIGHT);
        Button btnAnnuler = new Button("ANNULER");
        btnAnnuler.setStyle("-fx-background-color: transparent; -fx-text-fill: #5882D6; -fx-border-color: #5882D6; -fx-border-radius: 5; -fx-cursor: hand;");
        Button btnCreer = new Button("CRÉER");
        btnCreer.setStyle("-fx-background-color: #5882D6; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");

        boutonsBox.getChildren().addAll(btnAnnuler, btnCreer);
        root.getChildren().addAll(titreLabel, sujetBox, serviceBox, descBox, boutonsBox);

        btnAnnuler.setOnAction(e -> popupStage.close());

        btnCreer.setOnAction(e -> {
            String sujet = sujetField.getText().trim();
            String service = serviceComboBox.getValue();
            String description = descriptionArea.getText().trim();

            if (sujet.isEmpty() || service == null || description.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs.").showAndWait();
                return;
            }

            try {
                Ticket nouveauTicket = new Ticket();
                nouveauTicket.setSujet(sujet);
                nouveauTicket.setService(service);
                nouveauTicket.setDescription(description);
                nouveauTicket.setAuteur(currentUser);

                ticketService.creerTicket(nouveauTicket);
                ticketService.marquerTicketNonLu(nouveauTicket.getId(), true);

                popupStage.close();

                if (WebSocketService.getInstance() != null) {
                    java.util.Set<Integer> staffIds = new java.util.HashSet<>();

                    if ("RH".equalsIgnoreCase(service)) {
                        List<Integer> rhIds = userService.getIdsByRole("RH");
                        if (rhIds != null) staffIds.addAll(rhIds);
                    } else if ("ADMIN".equalsIgnoreCase(service)) {
                        List<Integer> adminIds = userService.getIdsByRole("ADMIN");
                        if (adminIds != null) staffIds.addAll(adminIds);
                    }

                    staffIds.remove(currentUser.getId());

                    WebSocketService.getInstance().envoyerNotificationGroupée(
                            new java.util.ArrayList<>(staffIds),
                            nouveauTicket.getId(),
                            "TECH"
                    );
                }

                rafraichirAffichage();

            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erreur lors de la création du ticket.").showAndWait();
            }
        });

        Scene scene = new Scene(root, 500, 550);


        try {
            String css = getClass().getResource("/style/style.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (NullPointerException e) {
            System.err.println("Fichier CSS introuvable ! Vérifie le chemin d'accès.");
        }

        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    private void handleOpenTicket(String ticketId) {
        try {
            activeInstance = null; // On libère l'instance pour éviter les refreshs en arrière-plan
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/ticket-view.fxml"));
            Parent view = loader.load();
            TicketController controller = loader.getController();
            controller.initData(Integer.parseInt(ticketId));

            if (MenuController.getInstance() != null) {
                MenuController.getInstance().setCenterView(view);
                MenuController.getInstance().changerTitre("TICKET N°" + ticketId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFiltreEnCours(ActionEvent event) {
        modeArchivesActif = false;
        mettreAJourStyleBoutons();
        rafraichirAffichageLocal();
    }

    @FXML
    public void handleFiltreArchives(ActionEvent event) {
        modeArchivesActif = true;
        mettreAJourStyleBoutons();
        rafraichirAffichageLocal();
    }

    private void mettreAJourStyleBoutons() {
        String styleActif = "-fx-background-color: #5882D6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;";
        String styleInactif = "-fx-background-color: white; -fx-text-fill: #5882D6; -fx-border-color: #5882D6; -fx-border-radius: 5; -fx-font-weight: bold; -fx-cursor: hand;";

        if (modeArchivesActif) {
            btnFiltreArchives.setStyle(styleActif);
            btnFiltreEnCours.setStyle(styleInactif);
        } else {
            btnFiltreEnCours.setStyle(styleActif);
            btnFiltreArchives.setStyle(styleInactif);
        }
    }

    private boolean correspondAuModeActuel(Ticket t) {
        String statut = t.getStatut().name().toUpperCase();
        boolean estFerme = statut.equals("FERME") || statut.equals("RESOLU");
        return modeArchivesActif == estFerme;
    }
}
package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.*;
import com.eseo.steevejobs.util.TestRuntime;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Contrôleur FXML de la liste des tickets (mes tickets ou filtre par service RH/ADMIN).
 * Liaisons FXML : {@code ticketsContainer}, filtres en cours/archives, bouton création.
 * Implémente {@link ParametrizedController} pour le paramètre de service.
 */
public class TicketsListController implements ParametrizedController {

    /** Service de gestion des tickets. */
    private final TicketService ticketService = new TicketServiceImpl();
    /** Service de gestion des utilisateurs. */
    private UserService userService;
    /** Service de session utilisateur. */
    private SessionService sessionService;
    /** Utilisateur connecté consultant la liste. */
    private User currentUser;

    /** Conteneur des cartes de tickets affichées. */
    @FXML
    private VBox ticketsContainer;
    /** Label affichant le titre de la page tickets. */
    @FXML
    private Label titlepageticket;

    /** Liste complète des tickets chargés depuis la base. */
    private List<Ticket> tousLesTicketsBDD;
    /** Instance de la liste tickets actuellement affichée. */
    private static TicketsListController activeInstance;
    /** Filtre de service appliqué à la liste, ou null pour mes tickets. */
    private String filtreActuel = null;

    /** Bouton de filtre sur les tickets en cours. */
    @FXML
    private Button btnFiltreEnCours;
    /** Bouton de filtre sur les tickets archivés. */
    @FXML
    private Button btnFiltreArchives;
    /** Indique si le filtre archives est actif. */
    private boolean modeArchivesActif = false;
    /** Indique si un chargement de tickets est en cours. */
    private boolean isFetching = false;

    /**
     * Retourne l'instance active de la liste tickets.
     *
     * @return contrôleur courant ou {@code null}
     */
    public static TicketsListController getActiveInstance() {
        return activeInstance;
    }

    /**
     * Initialise les services et charge les tickets de manière asynchrone.
     */
    @FXML
    public void initialize() {
        activeInstance = this;
        this.userService = new UserService();
        this.sessionService = new SessionService();
        this.currentUser = SessionService.getUtilisateurConnecte();
        chargerTicketsBDDAsync(this::rafraichirAffichageLocal);
    }

    /**
     * Applique le filtre par service (ex. {@code ADMIN}, {@code RH}) pour la vue staff.
     *
     * @param parametreService code du service cible
     */
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

    /**
     * Affiche les tickets créés par l'utilisateur connecté (navigation menu « Tickets »).
     */
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

    /**
     * Charge la liste des tickets en base de manière asynchrone.
     *
     * @param actionApresChargement callback exécuté après le chargement, ou {@code null}
     */
    private void chargerTicketsBDDAsync(Runnable actionApresChargement) {
        if (TestRuntime.isEnabled()) {
            this.tousLesTicketsBDD = List.of();
            return;
        }
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

    /**
     * Filtre et affiche les tickets selon le mode courant (mes tickets, service ou archives).
     */
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

    /**
     * Recharge les tickets depuis la base et rafraîchit l'affichage (callback WebSocket).
     */
    public void rafraichirAffichage() {
        chargerTicketsBDDAsync(this::rafraichirAffichageLocal);
    }

    /**
     * Trie les tickets et remplit le conteneur avec les cartes visuelles.
     *
     * @param liste tickets à afficher
     */
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

    /**
     * Construit une carte cliquable représentant un ticket dans la liste.
     *
     * @param ticket ticket source
     * @return conteneur graphique de la carte
     */
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

    /**
     * Ouvre la popup de création d'un nouveau ticket.
     * Liaison FXML : bouton « Créer un ticket ».
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    public void handleCreateTicket(ActionEvent event) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Créer un nouveau ticket");

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        // Champs
        TextField sujetField = new TextField();
        sujetField.setPromptText("Ex: Mon écran ne s'allume plus");
        sujetField.getStyleClass().add("champform");
        sujetField.setPrefHeight(34);

        ComboBox<String> serviceComboBox = new ComboBox<>();
        serviceComboBox.setItems(FXCollections.observableArrayList("ADMIN", "RH"));
        serviceComboBox.setPromptText("Sélectionnez un service...");
        serviceComboBox.setMaxWidth(Double.MAX_VALUE);
        serviceComboBox.getStyleClass().add("menu-burger");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Détaillez votre problème...");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(6);
        descriptionArea.getStyleClass().add("textarea-form");

        Label errSujet = new Label("");
        errSujet.getStyleClass().addAll("label-style", "label-erreur");
        Label errService = new Label("");
        errService.getStyleClass().addAll("label-style", "label-erreur");
        Label errDesc = new Label("");
        errDesc.getStyleClass().addAll("label-style", "label-erreur");

        // GridPane 2 colonnes
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setMinWidth(120);
        colLabel.setPrefWidth(160);
        colLabel.setHalignment(HPos.LEFT);

        ColumnConstraints colField = new ColumnConstraints();
        colField.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(colLabel, colField);

        int row = 0;
        grid.add(new Label("Sujet *"), 0, row);
        grid.add(sujetField, 1, row++);
        grid.add(errSujet, 1, row++);

        grid.add(new Label("Service *"), 0, row);
        grid.add(serviceComboBox, 1, row++);
        grid.add(errService, 1, row++);

        grid.add(new Label("Description *"), 0, row);
        grid.add(descriptionArea, 1, row++);
        grid.add(errDesc, 1, row++);

        GridPane.setHgrow(sujetField, Priority.ALWAYS);
        GridPane.setHgrow(serviceComboBox, Priority.ALWAYS);
        GridPane.setHgrow(descriptionArea, Priority.ALWAYS);

        // Contenu
        VBox contentBox = new VBox(12);
        Label titreLabel = new Label("NOUVEAU TICKET");
        titreLabel.getStyleClass().add("popup-header-title");
        titreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #5882D6;");
        contentBox.getChildren().addAll(titreLabel, grid);
        contentBox.getStyleClass().add("popup-contenu");
        contentBox.setPadding(new Insets(8));

        dp.setContent(contentBox);

        // Applique popup.css au DialogPane
        appliquerStyleDialog(dp);

        // Récupère boutons
        Button btnCancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);

        if (btnOk != null) {
            btnOk.setText("CRÉER");
            btnOk.getStyleClass().add("button-primary");
            btnOk.addEventFilter(ActionEvent.ACTION, ev -> {
                // reset erreurs
                errSujet.setText(""); errService.setText(""); errDesc.setText("");
                boolean valide = true;

                if (sujetField.getText().trim().isEmpty()) {
                    errSujet.setText("Le sujet est obligatoire."); valide = false;
                }
                if (serviceComboBox.getValue() == null) {
                    errService.setText("Le service est obligatoire."); valide = false;
                }
                if (descriptionArea.getText().trim().isEmpty()) {
                    errDesc.setText("La description est obligatoire."); valide = false;
                }

                if (!valide) {
                    ev.consume();
                    return;
                }

                try {
                    Ticket nouveauTicket = new Ticket();
                    nouveauTicket.setSujet(sujetField.getText().trim());
                    nouveauTicket.setService(serviceComboBox.getValue());
                    nouveauTicket.setDescription(descriptionArea.getText().trim());
                    nouveauTicket.setAuteur(currentUser);

                    ticketService.creerTicket(nouveauTicket);
                    ticketService.marquerTicketNonLu(nouveauTicket.getId(), true);

                    // notifications websocket
                    if (WebSocketService.getInstance() != null) {
                        java.util.Set<Integer> staffIds = new java.util.HashSet<>();

                        if ("RH".equalsIgnoreCase(nouveauTicket.getService())) {
                            List<Integer> rhIds = userService.getIdsByRole("RH");
                            if (rhIds != null) staffIds.addAll(rhIds);
                        } else if ("ADMIN".equalsIgnoreCase(nouveauTicket.getService())) {
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

                    dialog.close();
                    rafraichirAffichage();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    errDesc.setText("Erreur lors de la création.");
                    ev.consume();
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setText("ANNULER");
            btnCancel.getStyleClass().add("button-annuler");
        }


        dp.setPrefWidth(720);
        dp.setMinWidth(520);
        dp.setMinHeight(Region.USE_PREF_SIZE);
        dp.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dp.setMaxHeight(700);

        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    /**
     * Ouvre la vue détail d'un ticket dans la zone centrale du menu.
     *
     * @param ticketId identifiant du ticket à ouvrir
     */
    private void handleOpenTicket(String ticketId) {
        try {
            activeInstance = null;
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

    /**
     * Active le filtre « en cours » (tickets non fermés).
     * Liaison FXML : {@code btnFiltreEnCours}.
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    public void handleFiltreEnCours(ActionEvent event) {
        modeArchivesActif = false;
        mettreAJourStyleBoutons();
        rafraichirAffichageLocal();
    }

    /**
     * Active le filtre « archives » (tickets fermés ou résolus).
     * Liaison FXML : {@code btnFiltreArchives}.
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    public void handleFiltreArchives(ActionEvent event) {
        modeArchivesActif = true;
        mettreAJourStyleBoutons();
        rafraichirAffichageLocal();
    }

    /**
     * Met en surbrillance le bouton de filtre actif (en cours ou archives).
     */
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

    /**
     * Indique si un ticket correspond au filtre en cours ou archives.
     *
     * @param t ticket évalué
     * @return {@code true} si le ticket doit apparaître dans le mode actif
     */
    private boolean correspondAuModeActuel(Ticket t) {
        String statut = t.getStatut().name().toUpperCase();
        boolean estFerme = statut.equals("FERME") || statut.equals("RESOLU");
        return modeArchivesActif == estFerme;
    }

    /**
     * Applique la feuille de style popup aux boutons d'une boîte de dialogue.
     *
     * @param dp panneau de dialogue cible
     */
    private void appliquerStyleDialog(DialogPane dp) {
        java.net.URL popupUrl = getClass().getResource("/style/popup.css");
        if (popupUrl != null) dp.getStylesheets().add(popupUrl.toExternalForm());

        Button btnOk = (Button) dp.lookupButton(ButtonType.OK);
        if (btnOk != null) btnOk.getStyleClass().add("button-ok");

        Button btnCancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (btnCancel != null) btnCancel.getStyleClass().add("button-cancel");
    }
}

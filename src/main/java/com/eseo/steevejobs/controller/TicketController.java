package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.*;
import com.eseo.steevejobs.util.TestRuntime;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Contrôleur FXML du détail d'un ticket (fil de messages).
 * Liaisons FXML : {@code chatMessagesContainer}, {@code messageInput}, en-tête et statut.
 */
public class TicketController {

    /** Service de gestion des tickets. */
    private final TicketService ticketService = new TicketServiceImpl();
    /** Service de gestion des utilisateurs. */
    private final UserService userService = new UserService();

    /** Instance du contrôleur de détail ticket actuellement affichée. */
    private static TicketController activeInstance;

    /** Label affichant le numéro du ticket. */
    @FXML
    private Label ticketTitleLabel;
    /** Label affichant l'objet du ticket. */
    @FXML
    private Label ticketObjectLabel;
    /** Conteneur des bulles de messages du fil de discussion. */
    @FXML
    private VBox chatMessagesContainer;
    /** Champ de saisie d'un nouveau message. */
    @FXML
    private TextField messageInput;
    /** Label affichant le service du ticket. */
    @FXML
    private Label serviceLabel;
    /** Label affichant le statut du ticket. */
    @FXML
    private Label statusLabel;
    /** Label affichant la date de création du ticket. */
    @FXML
    private Label dateLabel;
    /** Bouton de fermeture ou réouverture du ticket. */
    @FXML
    private Button actionButton;
    /** Panneau défilant contenant les messages. */
    @FXML
    private ScrollPane messageScrollPane;
    /** Label affichant la description du ticket. */
    @FXML
    private Label descriptionLabel;

    /** Ticket actuellement affiché dans le chat. */
    private Ticket currentTicket;
    /** Identifiant du ticket affiché, ou -1 si aucun. */
    private int viewingTicketId = -1;
    /** Utilisateur connecté consultant le ticket. */
    private User currentUser;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM à HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy\nHH:mm:ss");

    /**
     * Retourne l'instance active du chat ticket.
     *
     * @return contrôleur ouvert ou {@code null}
     */
    public static TicketController getActiveInstance() {
        return activeInstance;
    }

    /**
     * Identifiant du ticket actuellement affiché.
     *
     * @return identifiant ticket ou {@code -1} si aucun chat actif
     */
    public int getCurrentTicketId() {
        return viewingTicketId;
    }

    /**
     * Initialise l'utilisateur courant et le défilement automatique du fil de messages.
     */
    @FXML
    public void initialize() {
        this.currentUser = SessionService.getUtilisateurConnecte();
        chatMessagesContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                messageScrollPane.layout();
                messageScrollPane.setVvalue(1.0);
            });
        });
    }

    /**
     * Libère la session chat active (appelé lors d'un changement de page menu).
     */
    public static void fermerChat() {
        if (activeInstance != null) {
            activeInstance.viewingTicketId = -1;
            activeInstance.currentTicket = null;
        }
        activeInstance = null;
    }

    /**
     * Réinitialise l'état interne du chat actif sans fermer la vue.
     */
    private void libererSessionChat() {
        viewingTicketId = -1;
        currentTicket = null;
        activeInstance = null;
    }

    /**
     * Recharge le fil de messages sans indicateur de chargement (callback WebSocket).
     */
    public void refreshChatSilently() {
        if (TestRuntime.isEnabled() || viewingTicketId <= 0) return;
        int ticketId = viewingTicketId;
        CompletableFuture.supplyAsync(() -> {
            currentTicket = ticketService.getTicketById(ticketId);
            return ticketService.getMessagesDuTicket(ticketId);
        }).thenAcceptAsync(messages -> {
            List<Node> bulles = messages.stream()
                    .map(this::creerMessageBubble)
                    .collect(Collectors.toList());

            chatMessagesContainer.getChildren().setAll(bulles);
            updateStatusUI();
            messageScrollPane.setVvalue(1.0);
        }, Platform::runLater).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    /**
     * Marque le ticket comme non lu et envoie une notification WebSocket aux destinataires concernés.
     */
    private void notifierMiseAJourTicket() {
        try {
            if (WebSocketService.getInstance() != null) {
                int monId = currentUser.getId();
                int idAuteurTicket = currentTicket.getAuteur().getId();

                if (monId == idAuteurTicket) {
                    ticketService.marquerTicketNonLu(currentTicket.getId(), true);
                    java.util.Set<Integer> staffIds = new java.util.HashSet<>();
                    String serviceDuTicket = currentTicket.getService();

                    if ("RH".equalsIgnoreCase(serviceDuTicket)) {
                        List<Integer> rhIds = userService.getIdsByRole("RH");
                        if (rhIds != null) staffIds.addAll(rhIds);
                    } else if ("ADMIN".equalsIgnoreCase(serviceDuTicket)) {
                        List<Integer> adminIds = userService.getIdsByRole("ADMIN");
                        if (adminIds != null) staffIds.addAll(adminIds);
                    } else {
                        List<Integer> rhIds = userService.getIdsByRole("RH");
                        List<Integer> adminIds = userService.getIdsByRole("ADMIN");
                        if (rhIds != null) staffIds.addAll(rhIds);
                        if (adminIds != null) staffIds.addAll(adminIds);
                    }

                    staffIds.remove(monId);
                    WebSocketService.getInstance().envoyerNotificationGroupée(
                            new java.util.ArrayList<>(staffIds),
                            currentTicket.getId(),
                            "TECH"
                    );
                } else {
                    ticketService.marquerTicketNonLu(currentTicket.getId(), false);
                    WebSocketService.getInstance().envoyerNotification(
                            idAuteurTicket,
                            currentTicket.getId(),
                            "AUTEUR"
                    );
                }
            }
        } catch (Exception wsException) {
            System.err.println("❌ Erreur WS : " + wsException.getMessage());
        }
    }

    /**
     * Envoie le message saisi et notifie les destinataires via WebSocket.
     * Liaison FXML : bouton d'envoi ou action Entrée sur {@code messageInput}.
     */
    @FXML
    public void handleSendMessage() {
        String texte = messageInput.getText().trim();

        if (texte.isEmpty() || currentTicket == null) return;
        messageInput.setDisable(true);
        messageInput.clear();

        Message nouveauMessage = new Message();
        nouveauMessage.setContenu(texte);
        nouveauMessage.setAuteur(currentUser);
        nouveauMessage.setTicket(currentTicket);
        nouveauMessage.setDateEnvoi(LocalDateTime.now());

        chatMessagesContainer.getChildren().add(creerMessageBubble(nouveauMessage));

        CompletableFuture.runAsync(() -> {
            ticketService.ajouterMessage(currentTicket.getId(), nouveauMessage);
            currentTicket = ticketService.getTicketById(currentTicket.getId());
            notifierMiseAJourTicket();
        }).thenRunAsync(() -> {

            messageInput.setDisable(false);
            messageInput.requestFocus();
            updateStatusUI();
        }, Platform::runLater).exceptionally(ex -> {
            ex.printStackTrace();
            Platform.runLater(() -> {
                messageInput.setDisable(false);
                messageInput.setText(texte);
                showAlert("Erreur réseau", "Le message n'a pas pu être envoyé.");
            });
            return null;
        });
    }

    /**
     * Construit une bulle de message alignée selon l'auteur.
     *
     * @param message message à afficher
     * @return conteneur graphique du message
     */
    private HBox creerMessageBubble(Message message) {
        HBox messageWrapper = new HBox();
        messageWrapper.setPadding(new Insets(5, 0, 5, 0));

        VBox contentBox = new VBox(5);
        contentBox.setMaxWidth(400);

        Label messageLabel = new Label(message.getContenu());
        messageLabel.setWrapText(true);
        messageLabel.setPadding(new Insets(10, 15, 10, 15));

        String auteurNom = "";
        boolean isMyMessage = (message.getAuteur().getId() == currentUser.getId());

        if (!isMyMessage) {
            auteurNom = message.getAuteur().getPrenom() + " " + message.getAuteur().getNom() + " - ";
        }
        Label infoLabel = new Label(auteurNom + message.getDateEnvoi().format(TIME_FORMATTER));
        infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");

        if (isMyMessage) {
            messageWrapper.setAlignment(Pos.CENTER_RIGHT);
            contentBox.setAlignment(Pos.TOP_RIGHT);
            messageLabel.setStyle("-fx-background-color: #BBD2FA; -fx-text-fill: black; -fx-background-radius: 10; -fx-font-size: 14px;");
        } else {
            messageWrapper.setAlignment(Pos.CENTER_LEFT);
            contentBox.setAlignment(Pos.TOP_LEFT);
            messageLabel.setStyle("-fx-background-color: #6A8BCC; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-size: 14px;");
        }

        contentBox.getChildren().addAll(messageLabel, infoLabel);
        messageWrapper.getChildren().add(contentBox);

        return messageWrapper;
    }

    /**
     * Bascule le statut du ticket entre ouvert et fermé.
     * Liaison FXML : {@code actionButton}.
     *
     * @param actionEvent événement du bouton (non utilisé)
     */
    @FXML
    public void handleToggleTicketStatus(ActionEvent actionEvent) {
        if (currentTicket == null) return;

        StatutTicket nouveauStatut = (currentTicket.getStatut() == StatutTicket.EN_ATTENTE || currentTicket.getStatut() == StatutTicket.EN_COURS)
                ? StatutTicket.valueOf("FERME")
                : StatutTicket.EN_ATTENTE;

        actionButton.setDisable(true); // Sécurité anti-spam

        CompletableFuture.runAsync(() -> {
            currentTicket = ticketService.changerStatut(currentTicket.getId(), nouveauStatut);
            notifierMiseAJourTicket();
        }).thenRunAsync(() -> {
            actionButton.setDisable(false);
            updateStatusUI();
        }, Platform::runLater).exceptionally(ex -> {
            ex.printStackTrace();
            Platform.runLater(() -> {
                actionButton.setDisable(false);
                showAlert("Erreur", "Impossible de changer le statut du ticket.");
            });
            return null;
        });
    }

    /**
     * Met à jour les labels de statut et l'état du champ de saisie selon le ticket courant.
     */
    private void updateStatusUI() {
        if (currentTicket == null) return;

        String statutActuel = currentTicket.getStatut().name();

        if (statutActuel.equals("FERME") || statutActuel.equals("RESOLU") || statutActuel.equals("CLOSED")) {
            statusLabel.setText("STATUS :\nFERMÉ");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: red; -fx-font-weight: bold;");
            actionButton.setText("RÉOUVRIR\nLE TICKET");
            actionButton.setStyle("-fx-background-color: #5882D6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");

            messageInput.setDisable(true);
            messageInput.setPromptText("Ce ticket est fermé.");
        } else {
            statusLabel.setText("STATUS :\n" + statutActuel.replace("_", " "));
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: green; -fx-font-weight: bold;");
            actionButton.setText("FERMER\nLE TICKET");
            actionButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");

            messageInput.setDisable(false);
            messageInput.setPromptText("Ecrivez votre message...");
        }
    }

    /**
     * Retourne à la liste des tickets (mes tickets ou filtre service selon le rôle).
     * Liaison FXML : bouton retour.
     *
     * @param actionEvent événement du bouton (non utilisé)
     */
    @FXML
    public void handleRetour(ActionEvent actionEvent) {
        libererSessionChat();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/ticketsList-view.fxml"));
            Parent view = loader.load();
            TicketsListController controller = loader.getController();

            boolean jeSuisAuteur = (currentTicket.getAuteur().getId() == currentUser.getId());

            if (jeSuisAuteur) {
                controller.afficherMesTickets();
            } else {
                controller.initData(currentTicket.getService());
            }

            if (MenuController.getInstance() != null) {
                MenuController.getInstance().setCenterView(view);
                MenuController.getInstance().changerTitre("Tickets");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Affiche une alerte d'avertissement stylisée.
     *
     * @param title titre de la fenêtre
     * @param content message affiché
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        appliquerStyleDialog(alert.getDialogPane());
        alert.showAndWait();
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

    /**
     * Charge un ticket par identifiant et affiche le fil de messages.
     * Implémentation de {@link ParametrizedController#initData(String)} avec conversion entière.
     *
     * @param ticketId identifiant du ticket à ouvrir
     */
    public void initData(int ticketId) {
        activeInstance = this;
        viewingTicketId = ticketId;
        if (TestRuntime.isEnabled()) {
            return;
        }
        chatMessagesContainer.getChildren().clear();
        messageInput.clear();
        messageInput.setDisable(false);
        messageInput.setPromptText("Ecrivez votre message...");

        ProgressIndicator loader = new ProgressIndicator();
        loader.setMaxSize(50, 50);
        VBox loaderContainer = new VBox(loader);
        loaderContainer.setAlignment(Pos.CENTER);
        loaderContainer.prefHeightProperty().bind(messageScrollPane.heightProperty());
        chatMessagesContainer.getChildren().add(loaderContainer);

        CompletableFuture.supplyAsync(() -> {
            currentTicket = ticketService.getTicketById(ticketId);
            return ticketService.getMessagesDuTicket(ticketId);
        }).thenAcceptAsync(messages -> {
            if (currentTicket == null) {
                showAlert("Erreur", "Le ticket demandé est introuvable.");
                return;
            }

            if (WebSocketService.getInstance() != null) {
                WebSocketService.getInstance().marquerCommeLu(ticketId);
                boolean jeSuisAdmin = (currentUser.getId() != currentTicket.getAuteur().getId());
                ticketService.marquerTicketLu(ticketId, jeSuisAdmin);

                int restants = jeSuisAdmin
                        ? ticketService.getNombreTicketsNonLusAdmin(currentUser.getRole(), currentUser.getId())
                        : ticketService.getNombreTicketsNonLusAuteur(currentUser.getId());

                if (jeSuisAdmin) {
                    HomeController.notificationsTech = restants;
                    if (MenuController.getInstance() != null)
                        MenuController.getInstance().allumerBadge("TECH", restants);
                } else {
                    HomeController.notificationsAuteur = restants;
                    if (MenuController.getInstance() != null)
                        MenuController.getInstance().allumerBadge("AUTEUR", restants);
                }
            }

            // MàJ Textes Header
            ticketTitleLabel.setText("TICKET N°" + currentTicket.getId());
            ticketObjectLabel.setText("OBJET : " + (currentTicket.getSujet() != null ? currentTicket.getSujet().toUpperCase() : "SANS SUJET"));
            serviceLabel.setText("SERVICE\n" + currentTicket.getService().toUpperCase());
            dateLabel.setText("DATE DE\nCRÉATION :\n" + currentTicket.getDateOuverture().format(DATE_FORMATTER));
            descriptionLabel.setText("DESCRIPTION : \n\n" + (currentTicket.getDescription() != null ? currentTicket.getDescription().toUpperCase() : "SANS DESCRIPTION"));

            updateStatusUI();

            // ⭐ Batch Rendering des messages initiaux
            List<Node> bulles = messages.stream()
                    .map(this::creerMessageBubble)
                    .collect(Collectors.toList());

            chatMessagesContainer.getChildren().setAll(bulles);
            messageScrollPane.setVvalue(1.0);

        }, Platform::runLater).exceptionally(ex -> {
            Platform.runLater(() -> {
                chatMessagesContainer.getChildren().clear();
                Label errorLabel = new Label("Erreur de connexion au serveur.");
                errorLabel.setStyle("-fx-text-fill: red;");
                chatMessagesContainer.getChildren().add(errorLabel);
            });
            ex.printStackTrace();
            return null;
        });
    }
}
package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.*;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TicketController {

    private final TicketService ticketService = new TicketServiceImpl();
    private final UserService userService = new UserService();

    private static TicketController activeInstance;

    @FXML
    private Label ticketTitleLabel;
    @FXML
    private Label ticketObjectLabel;
    @FXML
    private VBox chatMessagesContainer;
    @FXML
    private TextField messageInput;
    @FXML
    private Label serviceLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Button actionButton;
    @FXML
    private ScrollPane messageScrollPane;

    private Ticket currentTicket;
    private User currentUser;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM à HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy\nHH:mm:ss");

    public static TicketController getActiveInstance() {
        return activeInstance;
    }

    public int getCurrentTicketId() {
        return currentTicket != null ? currentTicket.getId() : -1;
    }

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

    public static void fermerChat() {
        activeInstance = null;
    }

    public void refreshChatSilently() {
        if (currentTicket == null) return;

        int ticketId = currentTicket.getId();

        Task<List<Message>> loadTask = new Task<List<Message>>() {
            @Override
            protected List<Message> call() throws Exception {
                currentTicket = ticketService.getTicketById(ticketId);
                return ticketService.getMessagesDuTicket(ticketId);
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Message> messages = loadTask.getValue();
            chatMessagesContainer.getChildren().clear();

            for (Message msg : messages) {
                addMessageBubble(msg);
            }
            Platform.runLater(() -> {
                messageScrollPane.setVvalue(1.0);
                updateStatusUI();
            });
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void notifierMiseAJourTicket() {
        try {
            if (WebSocketService.getInstance() != null) {
                String roleMoi = currentUser.getRole().toUpperCase();
                int monId = currentUser.getId();
                int idAuteurTicket = currentTicket.getAuteur().getId();

                if (roleMoi.equals("ADMIN") || roleMoi.equals("RH")) {
                    if (monId != idAuteurTicket) {
                        WebSocketService.getInstance().envoyerNotification(
                                idAuteurTicket,
                                currentTicket.getId(),
                                "AUTEUR"
                        );
                    }
                } else {
                    List<Integer> rhIds = userService.getIdsByRole("RH");
                    List<Integer> adminIds = userService.getIdsByRole("ADMIN");

                    java.util.Set<Integer> staffIds = new java.util.HashSet<>();
                    if (rhIds != null) staffIds.addAll(rhIds);
                    if (adminIds != null) staffIds.addAll(adminIds);

                    staffIds.remove(monId);

                    WebSocketService.getInstance().envoyerNotificationGroupée(
                            new java.util.ArrayList<>(staffIds),
                            currentTicket.getId(),
                            "TECH"
                    );
                }
            }
        } catch (Exception wsException) {
            wsException.printStackTrace();
        }
    }

    @FXML
    public void handleSendMessage() {
        String texte = messageInput.getText().trim();

        if (texte.isEmpty() || currentTicket == null) {
            return;
        }

        Message nouveauMessage = new Message();
        nouveauMessage.setContenu(texte);
        nouveauMessage.setAuteur(currentUser);
        nouveauMessage.setTicket(currentTicket);
        nouveauMessage.setDateEnvoi(LocalDateTime.now());

        addMessageBubble(nouveauMessage);
        messageInput.clear();

        Task<Void> sendTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ticketService.ajouterMessage(currentTicket.getId(), nouveauMessage);

                currentTicket = ticketService.getTicketById(currentTicket.getId());
                notifierMiseAJourTicket();

                return null;
            }
        };

        sendTask.setOnSucceeded(e -> Platform.runLater(this::updateStatusUI));

        sendTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            Platform.runLater(() -> showAlert("Erreur réseau", "Le message n'a pas pu être envoyé."));
        });

        Thread thread = new Thread(sendTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void addMessageBubble(Message message) {
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
        chatMessagesContainer.getChildren().add(messageWrapper);
    }

    @FXML
    public void handleToggleTicketStatus(ActionEvent actionEvent) {
        if (currentTicket == null) return;

        StatutTicket nouveauStatut = (currentTicket.getStatut() == StatutTicket.EN_ATTENTE || currentTicket.getStatut() == StatutTicket.EN_COURS)
                ? StatutTicket.valueOf("FERME")
                : StatutTicket.EN_ATTENTE;

        Task<Void> updateStatusTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                currentTicket = ticketService.changerStatut(currentTicket.getId(), nouveauStatut);
                notifierMiseAJourTicket();

                return null;
            }
        };

        updateStatusTask.setOnSucceeded(e -> Platform.runLater(this::updateStatusUI));

        updateStatusTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            Platform.runLater(() -> showAlert("Erreur", "Impossible de changer le statut du ticket."));
        });

        new Thread(updateStatusTask).start();
    }

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
            messageInput.setPromptText("Écrivez ici...");
        }
    }

    @FXML
    public void handleRetour(ActionEvent actionEvent) {
        activeInstance = null;

        if (MenuController.getInstance() != null) {
            MenuController.getInstance().chargerPage("ticketsList");
            MenuController.getInstance().changerTitre("Tickets");
        } else {
            System.err.println("Impossible de retourner en arrière.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void initData(int ticketId) {
        activeInstance = this;
        if (WebSocketService.getInstance() != null) {
            WebSocketService.getInstance().marquerCommeLu(ticketId);
            MenuController.getInstance().effacerBadgeticket();
        }
        chatMessagesContainer.getChildren().clear();

        ProgressIndicator loader = new ProgressIndicator();
        loader.setMaxSize(50, 50);

        VBox loaderContainer = new VBox(loader);
        loaderContainer.setAlignment(Pos.CENTER);
        loaderContainer.prefHeightProperty().bind(messageScrollPane.heightProperty());

        chatMessagesContainer.getChildren().add(loaderContainer);

        Task<List<Message>> loadMessagesTask = new Task<List<Message>>() {
            @Override
            protected List<Message> call() throws Exception {
                currentTicket = ticketService.getTicketById(ticketId);
                return ticketService.getMessagesDuTicket(ticketId);
            }
        };

        loadMessagesTask.setOnSucceeded(event -> {
            List<Message> messages = loadMessagesTask.getValue();

            if (currentTicket == null) {
                showAlert("Erreur", "Le ticket demandé est introuvable.");
                return;
            }
            ticketTitleLabel.setText("TICKET N°" + currentTicket.getId());
            ticketObjectLabel.setText("OBJET : " + (currentTicket.getSujet() != null ? currentTicket.getSujet().toUpperCase() : "SANS SUJET"));
            serviceLabel.setText("SERVICE\n" + currentTicket.getService().toUpperCase());
            dateLabel.setText("DATE DE\nCRÉATION :\n" + currentTicket.getDateOuverture().format(DATE_FORMATTER));

            updateStatusUI();
            chatMessagesContainer.getChildren().clear();

            for (Message msg : messages) {
                addMessageBubble(msg);
            }

            Platform.runLater(() -> messageScrollPane.setVvalue(1.0));
        });

        loadMessagesTask.setOnFailed(event -> {
            chatMessagesContainer.getChildren().clear();
            Label errorLabel = new Label("Erreur de connexion au serveur.");
            errorLabel.setStyle("-fx-text-fill: red;");
            chatMessagesContainer.getChildren().add(errorLabel);
            event.getSource().getException().printStackTrace();
        });

        Thread thread = new Thread(loadMessagesTask);
        thread.setDaemon(true);
        thread.start();
    }
}
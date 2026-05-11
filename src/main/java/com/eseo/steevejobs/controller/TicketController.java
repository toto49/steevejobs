package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Enum.StatutTicket;
import com.eseo.steevejobs.model.Message;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.TicketService;
import com.eseo.steevejobs.service.TicketServiceImpl;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TicketController {

    private final TicketService ticketService = new TicketServiceImpl();

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

    public void initData(int ticketId) {
        try {

            this.currentTicket = ticketService.getTicketById(ticketId);

            if (currentTicket == null) {
                showAlert("Erreur", "Le ticket demandé est introuvable.");
                return;
            }

            ticketTitleLabel.setText("TICKET N°" + currentTicket.getId());
            ticketObjectLabel.setText("OBJET : " + (currentTicket.getSujet() != null ? currentTicket.getSujet().toUpperCase() : "SANS SUJET"));

            serviceLabel.setText("SERVICE\n" + currentTicket.getService().toUpperCase());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy\nHH:mm:ss");
            dateLabel.setText("DATE DE\nCRÉATION :\n" + currentTicket.getDateOuverture().format(formatter));

            updateStatusUI();


            chargerMessages();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les données du ticket.");
        }
    }

    private void chargerMessages() {
        chatMessagesContainer.getChildren().clear();

        try {
            List<Message> messages = ticketService.getMessagesDuTicket(currentTicket.getId());

            for (Message msg : messages) {
                addMessageBubble(msg);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des messages : " + e.getMessage());
        }
    }

    private void addMessageBubble(Message message) {
        HBox messageWrapper = new HBox();
        messageWrapper.setPadding(new Insets(5, 0, 5, 0));

        VBox contentBox = new VBox(5);
        contentBox.setMaxWidth(400);

        Label messageLabel = new Label(message.getContenu());
        messageLabel.setWrapText(true);
        messageLabel.setPadding(new Insets(10, 15, 10, 15));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM à HH:mm");

        String auteurNom = message.getAuteur().getPrenom() + " " + message.getAuteur().getNom() + " - ";
        if (message.getAuteur().getId() == currentUser.getId()) {
            auteurNom = "";
        }
        Label infoLabel = new Label(auteurNom + message.getDateEnvoi().format(timeFormatter));
        infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");


        boolean isMyMessage = (message.getAuteur().getId() == currentUser.getId());

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
    public void handleSendMessage() {
        String texte = messageInput.getText().trim();

        if (texte.isEmpty() || currentTicket == null) {
            return;
        }


        try {
            Message nouveauMessage = new Message();
            nouveauMessage.setContenu(texte);
            nouveauMessage.setAuteur(currentUser);
            nouveauMessage.setTicket(currentTicket);

            ticketService.ajouterMessage(currentTicket.getId(), nouveauMessage);

            messageInput.clear();

            chargerMessages();

            this.currentTicket = ticketService.getTicketById(currentTicket.getId());
            updateStatusUI();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'envoyer le message.");
        }
    }

    @FXML
    public void handleToggleTicketStatus(ActionEvent actionEvent) {
        if (currentTicket == null) return;

        try {
            StatutTicket nouveauStatut;

            if (currentTicket.getStatut() == StatutTicket.EN_ATTENTE || currentTicket.getStatut() == StatutTicket.EN_COURS) {
                nouveauStatut = StatutTicket.valueOf("FERME");
            } else {
                nouveauStatut = StatutTicket.EN_ATTENTE;
            }


            this.currentTicket = ticketService.changerStatut(currentTicket.getId(), nouveauStatut);


            updateStatusUI();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de changer le statut du ticket.");
        }
    }

    private void updateStatusUI() {
        if (currentTicket == null) return;

        String statutActuel = currentTicket.getStatut().name();

        if (statutActuel.equals("FERME") || statutActuel.equals("RESOLU") || statutActuel.equals("CLOSED")) {
            statusLabel.setText("STATUS :\nFERMÉ");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: red; -fx-font-weight: bold;");
            actionButton.setText("RÉOUVRIR\nLE TICKET");
            actionButton.setStyle("-fx-background-color: #5882D6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");

            messageInput.setDisable(true); // Bloque l'envoi de message si le ticket est fermé
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
        if (MenuController.getInstance() != null) {
            MenuController.getInstance().chargerPage("ticketsList");
            MenuController.getInstance().changerTitre("Tickets");
        } else {
            System.err.println("Impossible de retourner en arrière : MenuController non initialisé.");
        }
    }


    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
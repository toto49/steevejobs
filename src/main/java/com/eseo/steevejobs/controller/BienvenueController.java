package com.eseo.steevejobs.controller;


import com.eseo.steevejobs.service.ConnexionService;
import com.eseo.steevejobs.service.MailService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class BienvenueController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    public void onLoginClick(ActionEvent actionEvent) {
    }

    public void mdpOublieClicked(ActionEvent actionEvent) {
        Label message = new Label("Entrée votre adresse mail de récupération !");
        message.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-text-fill: black;");

        TextField textField = new TextField();
        textField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: black; -fx-font-size: 14px; -fx-border-radius: 15");

        Button button = new Button("Envoyer");
        button.setStyle("-fx-font-size: 14px; -fx-background-color: green; -fx-text-fill: white;");

        HBox buttonBox = new HBox(button);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(message, textField, buttonBox);
        layout.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-radius: 10; -fx-background-radius: 10;");
        layout.setSpacing(10);
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Mot de passe oublié");
        popup.setScene(new Scene(layout));
        popup.setResizable(false);
        popup.show();
        button.setOnAction(e -> {
            String email = textField.getText();
            if (email.isEmpty()) {
                System.out.println("Aucun email entré");
                return;
            }


            new Thread(() -> {
                String token = ConnexionService.generateRandomMdp(12);
                MailService.EnvoyerMail(
                        email,
                        "Réinitialisation du mot de passe",
                        "Voici votre nouveau mot de passe : " + token + "\nPensez à bien changer votre mot de passe une fois connecté"
                );
            }).start();

            popup.close();
        });

    }


}


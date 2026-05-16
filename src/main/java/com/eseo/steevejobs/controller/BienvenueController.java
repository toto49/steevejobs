package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.HelloApplication;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class BienvenueController {

    @FXML
    private Label welcomeText;

    private final SessionService prefService = new SessionService();
    private final UserService userService = new UserService();

    @FXML
    private Text errror_connexion;
    @FXML
    private TextField mail_connexion;
    @FXML
    private PasswordField mdp_connexion;
    @FXML
    private CheckBox save_connexion;

    @FXML
    public void initialize() {
        if (prefService.hasEmailSauvegarde()) {
            mail_connexion.setText(prefService.recupererEmail());
            save_connexion.setSelected(true);
        }
        mdp_connexion.setOnAction(event -> onLoginClick(null));
    }

    @FXML
    public void mdpOublieClicked(ActionEvent actionEvent) {
        Label message = new Label("Entrez votre adresse mail de récupération :");
        message.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-text-fill: black;");

        TextField textField = new TextField();
        textField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: black; -fx-font-size: 14px; -fx-border-radius: 15;");
        Label popupMessageLabel = new Label("");
        popupMessageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");

        Button button = new Button("Envoyer");
        button.setStyle("-fx-font-size: 14px; -fx-background-color: green; -fx-text-fill: white;");

        HBox buttonBox = new HBox(button);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(message, textField, popupMessageLabel, buttonBox);
        layout.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-radius: 10; -fx-background-radius: 10;");
        layout.setSpacing(10);

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Mot de passe oublié");
        popup.setScene(new Scene(layout));
        popup.setResizable(false);
        popup.show();

        button.setOnAction(e -> {
            String email = textField.getText().trim();
            if (email.isEmpty()) {
                popupMessageLabel.setText("Veuillez entrer une adresse email.");
                popupMessageLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            button.setDisable(true);
            popupMessageLabel.setText("Envoi en cours...");
            popupMessageLabel.setStyle("-fx-text-fill: blue;");

            new Thread(() -> {
                try {
                    if (!userService.checkEmailExists(email)) {
                        throw new Exception("Cet email n'existe pas dans notre système.");
                    }

                    String token = ConnexionService.generateRandomMdp(12);
                    String hashedToken = userService.hashPassword(token);

                    User user = userService.getUserByEmail(email);
                    userService.updateUserPassword(user.getId(), hashedToken);

                    MailService.EnvoyerMail(
                            email,
                            "Réinitialisation du mot de passe",
                            "Voici votre nouveau mot de passe : " + token + "\nPensez à bien changer votre mot de passe une fois connecté."
                    );

                    Platform.runLater(() -> {
                        errror_connexion.setText("Email envoyé avec succès.");
                        errror_connexion.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        popup.close();
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        popupMessageLabel.setText(ex.getMessage() != null ? ex.getMessage() : "Erreur lors de l'envoi.");
                        popupMessageLabel.setStyle("-fx-text-fill: red;");
                        button.setDisable(false);
                    });
                }
            }).start();
        });
    }

    @FXML
    protected void onLoginClick(ActionEvent actionEvent) {
        String mail = mail_connexion.getText();
        String password = mdp_connexion.getText();
        boolean save = save_connexion.isSelected();

        if (mail == null || mail.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errror_connexion.setText("Veuillez remplir tous les champs.");
            errror_connexion.setStyle("-fx-fill: red; -fx-font-weight: bold;");
            return;
        }

        try {
            String passwordHash = userService.hashPassword(password);
            User connectedUser = userService.authenticate(mail, passwordHash);

            if (connectedUser == null) {
                errror_connexion.setText("Erreur : identifiants incorrects.");
                errror_connexion.setStyle("-fx-fill: red; -fx-font-weight: bold;");
            } else {
                errror_connexion.setText("");
                SessionService.setUtilisateurConnecte(connectedUser);
                String token = JwtService.genererToken(connectedUser.getId());
                SessionService.setTokenJWT(token);
                com.eseo.steevejobs.service.WebSocketService.getInstance().connecter();

                if (save) {
                    prefService.sauvegarderEmail(mail);
                } else {
                    prefService.effacerEmail();
                }

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/menu-view.fxml"));
                    Parent root = loader.load();
                    HelloApplication.changerPageGlobale(root, "Menu Principal");

                } catch (IOException e) {
                    e.printStackTrace();
                    errror_connexion.setText("Erreur : Impossible de charger la page suivante.");
                    errror_connexion.setStyle("-fx-fill: red; -fx-font-weight: bold;");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            errror_connexion.setText("Erreur de connexion à la base de données.");
            errror_connexion.setStyle("-fx-fill: red; -fx-font-weight: bold;");
        }
    }


}
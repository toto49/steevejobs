package com.eseo.steevejobs.controller;


import com.eseo.steevejobs.service.ConnexionService;
import com.eseo.steevejobs.service.MailService;
import com.eseo.steevejobs.service.SessionService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class BienvenueController {
    @FXML
    private Label welcomeText;
    private final SessionService prefService = new SessionService();
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
                    // TODO: Étape A - Appeler service pour vérifier si l'email existe bien en base de données.
                    // boolean emailExiste = UserService.verifierEmail(email);
                    // if (!emailExiste) { throw new Exception("Cet email n'existe pas."); }

                    String token = ConnexionService.generateRandomMdp(12);

                    // TODO: Étape B - Mettre à jour le mot de passe de l'utilisateur dans la base de données
                    // UserService.modifierMotDePasse(email, token);

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
                        popupMessageLabel.setText("Erreur lors de l'envoi. Veuillez réessayer.");
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

        // 3. Appel au service d'authentification
        // TODO: Appeler service de connexion ici.
        // boolean connexionOK = UserService.authentifier(mail, password);
        boolean connexionOK = false;

        if (!connexionOK) {
            errror_connexion.setText("Erreur : identifiants incorrects.");
            errror_connexion.setStyle("-fx-fill: red; -fx-font-weight: bold;");
        } else {
            errror_connexion.setText("");

            if (save) {

                prefService.sauvegarderEmail(mail);
            } else {
                prefService.effacerEmail();
            }


            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("view/menu-view.fxml"));
                Parent root = loader.load();

                // TODO: Si besoin de passer des données au nouveau contrôleur, faire ici :
                // MenuController controller = loader.getController();
                // controller.initData(monUtilisateurConnecte);


                Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                errror_connexion.setText("Erreur interne lors de la redirection.");
                errror_connexion.setStyle("-fx-fill: red; -fx-font-weight: bold;");
            }
        }
    }

}


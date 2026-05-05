package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.MailService;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.SystemNotificationService;
import com.eseo.steevejobs.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.prefs.Preferences;

public class ParametresController {

    @FXML
    private PasswordField ancienMdpField;
    @FXML
    private PasswordField nouveauMdpField;
    @FXML
    private PasswordField confirmerMdpField;
    @FXML
    private TextField mailField;
    @FXML
    private TextField telField;
    @FXML
    private TextField nomField;
    @FXML
    private TextField prenomField;

    @FXML
    private CheckBox pushNotificationsToggle;
    @FXML
    private CheckBox ConnexionCheck;

    private Preferences prefs;
    private UserService userService;
    private SessionService sessionService;
    private User currentUser;

    @FXML
    public void initialize() {
        this.userService = new UserService();
        this.sessionService = new SessionService();
        User utilisateur = SessionService.getUtilisateurConnecte();

        if (utilisateur != null) {
            setUtilisateurConnecte(utilisateur);
        } else {
            System.err.println("Aucun utilisateur n'est connecté en mémoire !");
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun utilisateur connecté.");
        }
        prefs = Preferences.userNodeForPackage(getClass());
        boolean isPushSaved = prefs.getBoolean("push_enabled", false);
        pushNotificationsToggle.setSelected(isPushSaved);
        boolean isConnexionSaved = sessionService.hasEmailSauvegarde();
        ConnexionCheck.setSelected(isConnexionSaved);
    }

    private void setUtilisateurConnecte(User user) {
        this.currentUser = user;
        if (user != null) {
            mailField.setText(user.getEmail() != null ? user.getEmail() : "");
            nomField.setText(user.getNom() != null ? user.getNom() : "");
        }
    }

    @FXML
    void toggleConnexionSave(ActionEvent event) {
        if (currentUser == null) return;

        if (ConnexionCheck.isSelected()) {
            sessionService.sauvegarderEmail(currentUser.getEmail());
        } else {
            sessionService.effacerEmail();
        }
    }

    @FXML
    void enregistrerInformations(ActionEvent event) {
        if (currentUser == null) return;

        try {
            String nouvelEmail = mailField.getText().trim();

            if (nouvelEmail.isEmpty() || nomField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Attention", "L'email et le nom sont obligatoires.");
                return;
            }

            currentUser.setEmail(nouvelEmail);
            currentUser.setNom(nomField.getText().trim());
            userService.updateUser(currentUser);
            if (ConnexionCheck.isSelected()) {
                sessionService.sauvegarderEmail(nouvelEmail);
            }

            String contenuemail =
                    "Bonjour,\n\n" +
                            "Vos informations personnelles ont été modifiées.\n" +
                            "Si vous n’êtes pas à l’origine de ce changement, veuillez contacter immédiatement le service informatique.\n\n" +
                            "Cordialement,\n" +
                            "Le support technique";

            MailService.EnvoyerMail(nouvelEmail, "changement de vos informations", contenuemail);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Vos informations ont été mises à jour avec succès.");
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur serveur", "Impossible de mettre à jour vos informations.");
        }
    }

    @FXML
    void validerSecurite(ActionEvent event) {
        if (currentUser == null) return;

        String ancienMdp = ancienMdpField.getText();
        String nouveauMdp = nouveauMdpField.getText();
        String confirmationMdp = confirmerMdpField.getText();

        if (ancienMdp.isEmpty() || nouveauMdp.isEmpty() || confirmationMdp.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir tous les champs de sécurité.");
            return;
        }

        if (!nouveauMdp.equals(confirmationMdp)) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le nouveau mot de passe et la confirmation ne correspondent pas.");
            return;
        }
        String hashedAncien = userService.hashPassword(ancienMdp);
        if (!hashedAncien.equals(currentUser.getPasswordHash())) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "L'ancien mot de passe est incorrect.");
            return;
        }

        try {
            String contenuemail =
                    "Bonjour,\n\n" +
                            "Vos informations de connexion ont été modifiées.\n" +
                            "Si vous n’êtes pas à l’origine de ce changement, veuillez contacter immédiatement le service informatique.\n\n" +
                            "Cordialement,\n" +
                            "Le support technique";

            MailService.EnvoyerMail(currentUser.getEmail(), "changement de vos informations de connexion", contenuemail);
            String hashedNouveau = userService.hashPassword(nouveauMdp);
            userService.updateUserPassword(currentUser.getId(), hashedNouveau);
            currentUser.setPasswordHash(hashedNouveau);
            ancienMdpField.clear();
            nouveauMdpField.clear();
            confirmerMdpField.clear();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre mot de passe a été modifié.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur serveur", "Impossible de mettre à jour le mot de passe.");
        }
    }

    @FXML
    void testpush(ActionEvent event) {
        boolean isPushEnabled = pushNotificationsToggle.isSelected();
        prefs.putBoolean("push_enabled", isPushEnabled);
        SystemNotificationService.send("test", "coucou");
    }

    @FXML
    void handleRetour(ActionEvent event) {
        System.out.println("Clic sur le bouton Retour.");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
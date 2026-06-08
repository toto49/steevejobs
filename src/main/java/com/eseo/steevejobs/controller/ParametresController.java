package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.HelloApplication;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.MailService;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.SystemNotificationService;
import com.eseo.steevejobs.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;
import java.util.prefs.Preferences;

/**
 * Contrôleur FXML de la page paramètres utilisateur.
 * Liaisons FXML : champs profil, mots de passe, cases notifications et connexion mémorisée.
 * Navigation déconnexion vers {@code bienvenue-view.fxml}.
 */
public class ParametresController {

    /** Champ de saisie de l'ancien mot de passe. */
    @FXML
    private PasswordField ancienMdpField;
    /** Champ de saisie du nouveau mot de passe. */
    @FXML
    private PasswordField nouveauMdpField;
    /** Champ de confirmation du nouveau mot de passe. */
    @FXML
    private PasswordField confirmerMdpField;
    /** Champ de saisie de l'adresse e-mail. */
    @FXML
    private TextField mailField;
    /** Champ de saisie du numéro de téléphone. */
    @FXML
    private TextField telField;
    /** Champ de saisie du nom. */
    @FXML
    private TextField nomField;
    /** Champ de saisie du prénom. */
    @FXML
    private TextField prenomField;
    @FXML
    private TextField adresseField;

    /** Case à cocher d'activation des notifications push. */
    @FXML
    private CheckBox pushNotificationsToggle;
    /** Case à cocher de mémorisation de l'e-mail de connexion. */
    @FXML
    private CheckBox ConnexionCheck;

    /** Préférences utilisateur locales. */
    private Preferences prefs;
    /** Service de gestion des utilisateurs. */
    private UserService userService;
    /** Service de session utilisateur. */
    private SessionService sessionService;
    /** Utilisateur dont le profil est affiché. */
    private User currentUser;

    /**
     * Charge l'utilisateur en session, préremplit les champs et restaure les préférences locales.
     */
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

    /**
     * Préremplit les champs du formulaire avec les données de l'utilisateur connecté.
     *
     * @param user utilisateur dont le profil est affiché
     */
    private void setUtilisateurConnecte(User user) {
        this.currentUser = user;
        if (user != null) {
            mailField.setText(user.getEmail() != null ? user.getEmail() : "");
            nomField.setText(user.getNom() != null ? user.getNom() : "");
            prenomField.setText(user.getPrenom() != null ? user.getPrenom() : "");
            telField.setText(user.getTel() != null ? user.getTel() : "");
            adresseField.setText(user.getAdresse() != null ? user.getAdresse() : "");
        }
    }

    /**
     * Active ou désactive la mémorisation de l'email de connexion.
     * Liaison FXML : {@code ConnexionCheck}.
     *
     * @param event événement de la case à cocher (non utilisé)
     */
    @FXML
    void toggleConnexionSave(ActionEvent event) {
        if (currentUser == null) return;

        if (ConnexionCheck.isSelected()) {
            sessionService.sauvegarderEmail(currentUser.getEmail());
        } else {
            sessionService.effacerEmail();
        }
    }

    /**
     * Active ou désactive les notifications push directement lors du clic sur la case à cocher.
     * Liaison FXML : {@code pushNotificationsToggle}.
     *
     * @param event événement de la case à cocher (non utilisé)
     */
    @FXML
    void togglePushNotification(ActionEvent event) {
        boolean isPushEnabled = pushNotificationsToggle.isSelected();

        // Sauvegarde immédiate dans les préférences locales de la machine
        prefs.putBoolean("push_enabled", isPushEnabled);

        if (isPushEnabled) {
            // Envoi instantané du test système pour confirmer l'activation à l'utilisateur
            SystemNotificationService.send("SteeveJobs - Notifications", "Les notifications push sont désormais activées !");
        } else {
            // Optionnel : Une petite alerte discrète ou un log pour confirmer la désactivation
            SystemNotificationService.send("SteeveJobs - Notifications", "Les notifications push ne sont désormais plus activées !");

        }
    }
    /**
     * Enregistre les informations personnelles modifiées et envoie un email de confirmation.
     * Liaison FXML : bouton d'enregistrement du profil.
     *
     * @param event événement du bouton (non utilisé)
     */
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
            currentUser.setPrenom(prenomField.getText().trim());
            currentUser.setTel(telField.getText().trim());
            currentUser.setAdresse(adresseField.getText().trim());
            userService.updateUser(currentUser);
            SessionService.setUtilisateurConnecte(currentUser);

            if (ConnexionCheck.isSelected()) {
                sessionService.sauvegarderEmail(nouvelEmail);
            }

            String contenuemail =
                    "Bonjour,\n\n" +
                            "Vos informations personnelles ont été modifiées.\n" +
                            "Si vous n’êtes pas à l’origine de ce changement, veuillez contacter immédiatement le service informatique.\n\n" +
                            "Cordialement,\n" +
                            "Le support technique";

            MailService.EnvoyerMail(nouvelEmail, "Changement de vos informations", contenuemail);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Vos informations ont été mises à jour avec succès.");

        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", e.getMessage());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur serveur", "Impossible de mettre à jour vos informations.");
        }
    }

    /**
     * Valide et enregistre le nouveau mot de passe après vérification de l'ancien.
     * Liaison FXML : bouton de validation sécurité.
     *
     * @param event événement du bouton (non utilisé)
     */
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
        if (!BCrypt.checkpw(ancienMdp, currentUser.getPasswordHash())) {
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

            MailService.EnvoyerMail(currentUser.getEmail(), "Changement de vos informations de connexion", contenuemail);
            userService.updateUserPassword(currentUser.getId(), nouveauMdp);
            String nouveauHash = userService.hashPassword(nouveauMdp);
            currentUser.setPasswordHash(nouveauHash);

            SessionService.setUtilisateurConnecte(currentUser);
            ancienMdpField.clear();
            nouveauMdpField.clear();
            confirmerMdpField.clear();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre mot de passe a été modifié.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur serveur", "Impossible de mettre à jour le mot de passe.");
        }
    }

    /**
     * Enregistre la préférence de notifications push et envoie un message de test si activé.
     * Liaison FXML : {@code pushNotificationsToggle}.
     *
     * @param event événement de la case à cocher (non utilisé)
     */
    @FXML
    void testpush(ActionEvent event) {
        boolean isPushEnabled = pushNotificationsToggle.isSelected();
        prefs.putBoolean("push_enabled", isPushEnabled);
        if (isPushEnabled) {
            SystemNotificationService.send("SteeveJobs - Test", "Les notifications push sont bien activées !");
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Notifications désactivées", "Vous avez désactivé les notifications push, aucun test n'a été envoyé.");
        }
    }

    /**
     * Gère le clic sur le bouton retour (journalisation uniquement).
     * Liaison FXML : bouton retour.
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    void handleRetour(ActionEvent event) {
        System.out.println("Clic sur le bouton Retour.");
    }

    /**
     * Affiche une boîte de dialogue modale.
     *
     * @param type type d'alerte
     * @param title titre de la fenêtre
     * @param content message affiché
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Réinitialise la session et navigue vers l'écran de connexion.
     * Liaison FXML : bouton de déconnexion.
     *
     * @param actionEvent événement du bouton (non utilisé)
     */
    @FXML
    public void resetsession(ActionEvent actionEvent) {
        try {
            SessionService.setUtilisateurConnecte(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/bienvenue-view.fxml"));
            Parent loginRoot = loader.load();

            HelloApplication.changerPageGlobale(loginRoot, "Connexion");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erreur lors du chargement de la page de connexion.");
        }
    }
}
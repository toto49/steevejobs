package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.WebSocketService;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.json.JSONObject;

import java.awt.*;
import java.net.URI;

public class VisioController {

    private static VisioController activeInstance;
    private static Dotenv dotenv;
    private boolean attenteTokenVisio = false;

    @FXML
    private TextField txtRoomName;
    @FXML
    private Button btnRejoindre;
    @FXML
    private Label lblStatut;

    public static VisioController getActiveInstance() {
        return activeInstance;
    }

    private static Dotenv getDotenv() {
        if (dotenv == null) dotenv = Dotenv.load();
        return dotenv;
    }

    @FXML
    public void initialize() {
        activeInstance = this;
        this.attenteTokenVisio = false;
        if (lblStatut != null) {
            lblStatut.setText("");
        }
    }

    public void couperController() {
        this.attenteTokenVisio = false;
        activeInstance = null;
    }

    @FXML
    private void demanderConnexion() {
        String room = txtRoomName.getText().trim();
        if (room.isEmpty()) {
            lblStatut.setText("⚠️ Veuillez renseigner un identifiant de salon.");
            return;
        }

        this.attenteTokenVisio = true;
        User currentUser = SessionService.getUtilisateurConnecte();
        String nomUtilisateur = (currentUser != null) ? currentUser.getNom() : "Tom_Boudaud";

        JSONObject requete = new JSONObject();
        requete.put("type", "REQUEST_VISIO_TOKEN");
        requete.put("roomName", room);
        requete.put("identity", nomUtilisateur);

        WebSocketService.getInstance().envoyerMessageBrut(requete.toString());
        lblStatut.setStyle("-fx-text-fill: #3498db;");
        lblStatut.setText("⏳ Négociation du jeton avec le serveur NAS...");
    }

    public void recevoirTokenEtLancer(String tokenJWT) {
        if (!attenteTokenVisio) return;
        this.attenteTokenVisio = false;

        Platform.runLater(() -> {
            try {
                String urlFrontNas = getDotenv().get("URL_FRONT_VISIO");
                String urlLiveKit = getDotenv().get("LIVEKIT_SERVER_URL");

                String urlComplete = String.format("%s?url=%s&token=%s", urlFrontNas, urlLiveKit, tokenJWT);
                System.out.println("🚀 Lancement de la visio hébergée sur le NAS : " + urlComplete);

                if (lblStatut != null) {
                    lblStatut.setStyle("-fx-text-fill: #2ecc71;");
                    lblStatut.setText("✅ Redirection vers le serveur de visio...");
                }

                String arch = System.getProperty("os.arch").toLowerCase();
                if (arch.contains("arm") || arch.contains("aarch64")) {
                    String commandeEdgeApp = "cmd /c start msedge --app=\"" + urlComplete + "\"";
                    Runtime.getRuntime().exec(commandeEdgeApp);
                } else {
                    Desktop.getDesktop().browse(new URI(urlComplete));
                }

            } catch (Exception e) {
                if (lblStatut != null) {
                    lblStatut.setStyle("-fx-text-fill: #e74c3c;");
                    lblStatut.setText("❌ Erreur : " + e.getMessage());
                }
            }
        });
    }
}
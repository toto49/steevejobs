package com.eseo.steevejobs.controller;

import fi.iki.elonen.NanoHTTPD;
import javafx.application.Platform;
import javafx.fxml.FXML;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class VisioController {

    private static VisioController activeInstance;
    private MiniServeurVisio serveurWeb;

    public static VisioController getActiveInstance() {
        return activeInstance;
    }

    @FXML
    public void initialize() {
        activeInstance = this;
    }

    @FXML
    private void demanderConnexion() {
        JSONObject requete = new JSONObject();
        requete.put("type", "REQUEST_VISIO_TOKEN");
        requete.put("roomName", "Salle_De_Crise");
        requete.put("identity", "Tom_Boudaud");
        System.out.println("⏳ Demande de token envoyée...");
    }

    public void recevoirTokenEtLancer(String tokenJWT) {
        Platform.runLater(() -> {
            try {
                if (serveurWeb == null) {
                    serveurWeb = new MiniServeurVisio(9999);
                    serveurWeb.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                }

                String urlLiveKit = "ws://82.65.149.31:7880";

                String urlComplete = String.format("http://localhost:9999/?url=%s&token=%s", urlLiveKit, tokenJWT);
                System.out.println("🚀 Lancement de la visio via serveur embarqué : " + urlComplete);

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(urlComplete));
                } else {
                    Runtime.getRuntime().exec("cmd /c start " + urlComplete);
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur lors du lancement du serveur/navigateur : " + e.getMessage());
            }
        });
    }

    private class MiniServeurVisio extends NanoHTTPD {
        public MiniServeurVisio(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            try {
                InputStream is = getClass().getResourceAsStream("/visio.html");
                if (is == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Fichier visio.html introuvable dans les ressources JavaFX");
                }

                String htmlContenu = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, htmlContenu);
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Erreur serveur : " + e.getMessage());
            }
        }
    }
}
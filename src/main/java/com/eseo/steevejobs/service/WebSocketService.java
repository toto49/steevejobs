package com.eseo.steevejobs.service;

import com.eseo.steevejobs.controller.MenuController;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WebSocketService {

    private static WebSocketService instance;

    private WebSocketClient wsClient;
    private boolean isConnected = false;

    private WebSocketService() {
    }

    public static WebSocketService getInstance() {
        if (instance == null) {
            instance = new WebSocketService();
        }
        return instance;
    }

    public void connecter() {
        if (isConnected) return;

        try {
            Dotenv dotenv = Dotenv.load();
            String ip = dotenv.get("WS_SERVER_IP");
            String port = dotenv.get("WS_SERVER_PORT");
            URI uri = new URI("ws://" + ip + ":" + port);

            wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    isConnected = true;
                    System.out.println("✅ WebSocketService connecté au NAS (" + ip + ")");

                    if (SessionService.getUtilisateurConnecte() != null) {
                        send("REGISTER:" + SessionService.getUtilisateurConnecte().getId());
                    }
                }

                @Override
                public void onMessage(String message) {
                    if (message.startsWith("UPDATE_TICKET:")) {

                        Platform.runLater(() -> {
                            if (MenuController.getInstance() != null) {
                                MenuController.getInstance().allumerBadge();
                            }
                        });
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    System.out.println("❌ WebSocketService déconnecté.");
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("Erreur WebSocket : " + ex.getMessage());
                }
            };
            wsClient.connect();

        } catch (Exception e) {
            System.err.println("Impossible d'initialiser le WebSocketService : " + e.getMessage());
        }
    }

    public void envoyerNotification(int idAuteurCible, int idTicket) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send("NOTIFY:" + idAuteurCible + ":" + idTicket);
        }
    }

    public void deconnecter() {
        if (wsClient != null) {
            wsClient.close();
        }
    }
}
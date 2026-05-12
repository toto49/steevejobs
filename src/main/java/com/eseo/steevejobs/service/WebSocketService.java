package com.eseo.steevejobs.service;

import com.eseo.steevejobs.controller.MenuController;
import com.eseo.steevejobs.controller.TicketController;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.List;

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
                        String payload = message.split(":")[1];
                        String idTicketStr = payload.split("_")[0];
                        String typeCible = payload.contains("_") ? payload.split("_")[1] : "AUTEUR";
                        int idTicket = Integer.parseInt(idTicketStr);

                        Platform.runLater(() -> {

                            TicketController chatActif = TicketController.getActiveInstance();

                            if (chatActif != null && chatActif.getCurrentTicketId() == idTicket) {

                                chatActif.refreshChatSilently();


                            } else {

                                if (MenuController.getInstance() != null) {
                                    MenuController.getInstance().allumerBadge(typeCible);
                                }

                                if ("AUTEUR".equals(typeCible)) {
                                    SystemNotificationService.send(
                                            "SteeveJobs - Support",
                                            "Vous avez une nouvelle réponse sur le ticket #" + idTicket
                                    );
                                }
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

    public void envoyerNotification(int idAuteurCible, int idTicket, String typeCible) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send("NOTIFY:" + idAuteurCible + ":" + idTicket + "_" + typeCible);
        }
    }

    public void deconnecter() {
        if (wsClient != null) {
            wsClient.close();
        }
    }

    public void envoyerNotificationGroupée(List<Integer> idsCibles, int idTicket, String typeCible) {
        if (wsClient != null && wsClient.isOpen() && !idsCibles.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < idsCibles.size(); i++) {
                sb.append(idsCibles.get(i));
                if (i < idsCibles.size() - 1) sb.append(",");
            }
            wsClient.send("NOTIFY:" + sb + ":" + idTicket + "_" + typeCible);
        }
    }
}
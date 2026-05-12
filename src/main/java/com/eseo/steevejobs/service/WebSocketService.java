package com.eseo.steevejobs.service;

import com.eseo.steevejobs.controller.MenuController;
import com.eseo.steevejobs.controller.TicketController;
import com.eseo.steevejobs.controller.TicketsListController;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebSocketService {

    private static WebSocketService instance;
    private WebSocketClient wsClient;
    private boolean isConnected = false;
    private final Set<Integer> ticketsNonLus = new HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private URI lastUri;

    private WebSocketService() {
        scheduler.scheduleAtFixedRate(this::checkConnection, 5, 5, TimeUnit.SECONDS);
    }

    public static WebSocketService getInstance() {
        if (instance == null) {
            instance = new WebSocketService();
        }
        return instance;
    }

    public void connecter() {
        if (isConnected && wsClient != null && wsClient.isOpen()) return;

        try {
            Dotenv dotenv = Dotenv.load();
            String ip = dotenv.get("WS_SERVER_IP");
            String port = dotenv.get("WS_SERVER_PORT");
            lastUri = new URI("ws://" + ip + ":" + port);

            wsClient = new WebSocketClient(lastUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    isConnected = true;
                    System.out.println("✅ WebSocketService connecté au NAS (" + ip + ":" + port + ")");
                    if (SessionService.getUtilisateurConnecte() != null) {
                        send("REGISTER:" + SessionService.getUtilisateurConnecte().getId());
                    }
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("📥 MESSAGE REÇU DU NAS : " + message);
                    if (message.startsWith("UPDATE_TICKET:")) {
                        traiterMessageUpdate(message);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    System.out.println("❌ WebSocketService déconnecté. Tentative de reconnexion prévue... (" + reason + ")");
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("⚠️ Erreur WebSocket : " + ex.getMessage());
                    isConnected = false;
                }
            };
            wsClient.connect();

        } catch (Exception e) {
            System.err.println("🛑 Erreur d'initialisation WebSocket : " + e.getMessage());
        }
    }

    private void checkConnection() {
        if (!isConnected || wsClient == null || !wsClient.isOpen()) {
            if (lastUri != null) {
                System.out.println("🔄 Tentative de reconnexion automatique...");
                connecter();
            }
        }
    }

    private void traiterMessageUpdate(String message) {
        String payload = message.split(":")[1];
        String idTicketStr = payload.split("_")[0];
        String typeCible = payload.contains("_") ? payload.split("_")[1] : "AUTEUR";
        int idTicket = Integer.parseInt(idTicketStr);

        Platform.runLater(() -> {
            TicketController chatActif = TicketController.getActiveInstance();
            if (chatActif != null && chatActif.getCurrentTicketId() == idTicket) {
                chatActif.refreshChatSilently();
            } else {
                ajouterTicketNonLu(idTicket);

                TicketsListController listeActive = TicketsListController.getActiveInstance();
                if (listeActive != null) {
                    listeActive.rafraichirAffichage();
                }

                if (MenuController.getInstance() != null) {
                    MenuController.getInstance().allumerBadge(typeCible);
                }

                if ("AUTEUR".equals(typeCible)) {
                    SystemNotificationService.send("SteeveJobs - Support", "Nouvelle réponse sur le ticket #" + idTicket);
                }
            }
        });
    }

    public void deconnecter(Runnable onClosed) {

        scheduler.shutdown();

        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
        if (onClosed != null) onClosed.run();
    }

    public void envoyerNotification(int idAuteurCible, int idTicket, String typeCible) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send("NOTIFY:" + idAuteurCible + ":" + idTicket + "_" + typeCible);
        } else {
            System.err.println("📤 Échec envoi : WebSocket déconnecté");
        }
    }

    public void envoyerNotificationGroupée(List<Integer> idsCibles, int idTicket, String typeCible) {
        if (wsClient != null && wsClient.isOpen() && !idsCibles.isEmpty()) {
            String ids = String.join(",", idsCibles.stream().map(String::valueOf).toArray(String[]::new));
            wsClient.send("NOTIFY:" + ids + ":" + idTicket + "_" + typeCible);
        }
    }

    public void ajouterTicketNonLu(int idTicket) {
        ticketsNonLus.add(idTicket);
    }

    public void marquerCommeLu(int idTicket) {
        ticketsNonLus.remove(idTicket);
    }

    public boolean isTicketNonLu(int idTicket) {
        return ticketsNonLus.contains(idTicket);
    }
}
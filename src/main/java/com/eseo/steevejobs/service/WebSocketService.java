package com.eseo.steevejobs.service;

import com.eseo.steevejobs.controller.HomeController;
import com.eseo.steevejobs.controller.MenuController;
import com.eseo.steevejobs.controller.TicketController;
import com.eseo.steevejobs.controller.TicketsListController;
import com.eseo.steevejobs.model.User;
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
                    System.out.println("❌ WebSocketService déconnecté. (" + reason + ")");
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
                connecter();
            }
        }
    }

    private void traiterMessageUpdate(String message) {
        try {
            String payload = message.split(":")[1];
            String[] parts = payload.split("_");

            int idTicket = Integer.parseInt(parts[0]);
            String typeCible = parts.length > 1 ? parts[1] : "AUTEUR";
            int idSender = parts.length > 2 ? Integer.parseInt(parts[2]) : -1;

            User currentUser = SessionService.getUtilisateurConnecte();
            if (currentUser != null && currentUser.getId() == idSender) {
                System.out.println("🤫 Auto-notification bloquée : C'est moi qui ai déclenché cette action.");
                return;
            }

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

                    HomeController.ajouterNotification(typeCible);

                    if ("AUTEUR".equals(typeCible)) {
                        SystemNotificationService.send("SteeveJobs - Support", "Nouvelle réponse sur le ticket #" + idTicket);
                    } else if ("TECH".equals(typeCible)) {
                        SystemNotificationService.send("SteeveJobs - Admin", "Nouveau message à traiter ! (#" + idTicket + ")");
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du message WS : " + e.getMessage());
        }
    }

    public void deconnecter(Runnable onClosed) {
        scheduler.shutdown();
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
        if (onClosed != null) onClosed.run();
    }

    public void envoyerNotification(int idAuteurCible, int idTicket, String typeCible) {
        System.out.println("🚀 [DEBUG WS] Tentative d'envoi simple -> Cible ID: " + idAuteurCible + " | Type: " + typeCible);
        if (wsClient != null && wsClient.isOpen()) {
            int myId = SessionService.getUtilisateurConnecte() != null ? SessionService.getUtilisateurConnecte().getId() : -1;
            String msg = "NOTIFY:" + idAuteurCible + ":" + idTicket + "_" + typeCible + "_" + myId;
            System.out.println("📤 ENVOI AU NAS : " + msg);
            wsClient.send(msg);
        } else {
            System.err.println("❌ [DEBUG WS] Envoi annulé : WebSocket déconnecté !");
        }
    }

    public void envoyerNotificationGroupée(List<Integer> idsCibles, int idTicket, String typeCible) {
        System.out.println("🚀 [DEBUG WS] Tentative d'envoi groupé -> Type: " + typeCible);
        System.out.println("🚀 [DEBUG WS] Liste des destinataires trouvés : " + idsCibles);

        if (wsClient != null && wsClient.isOpen()) {
            if (!idsCibles.isEmpty()) {
                int myId = SessionService.getUtilisateurConnecte() != null ? SessionService.getUtilisateurConnecte().getId() : -1;
                String ids = String.join(",", idsCibles.stream().map(String::valueOf).toArray(String[]::new));
                String msg = "NOTIFY:" + ids + ":" + idTicket + "_" + typeCible + "_" + myId;
                System.out.println("📤 ENVOI GROUPÉ AU NAS : " + msg);
                wsClient.send(msg);
            } else {
                System.err.println("❌ [DEBUG WS] Envoi annulé : La liste des destinataires est VIDE ! (Ta requête BDD n'a trouvé aucun Admin/RH)");
            }
        } else {
            System.err.println("❌ [DEBUG WS] Envoi annulé : WebSocket déconnecté !");
        }
    }

    // --- Gestion des tickets non lus ---
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
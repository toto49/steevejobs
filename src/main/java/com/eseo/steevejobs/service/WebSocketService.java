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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WebSocketService {

    private static volatile WebSocketService instance;
    private WebSocketClient wsClient;
    private boolean isConnected = false;
    private final Set<Integer> ticketsNonLus = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private URI lastUri;

    private WebSocketService() {
        scheduler.scheduleAtFixedRate(this::checkConnection, 5, 5, TimeUnit.SECONDS);
    }
    public static WebSocketService getInstance() {
        if (instance == null) {
            synchronized (WebSocketService.class) {
                if (instance == null) {
                    instance = new WebSocketService();
                }
            }
        }
        return instance;
    }

    public synchronized void connecter() {
        if (isConnected && wsClient != null && wsClient.isOpen()) return;

        try {
            Dotenv dotenv = Dotenv.load();
            String ip = dotenv.get("WS_SERVER_IP");
            String port = dotenv.get("WS_SERVER_PORT");

            if (ip == null || port == null) return;

            lastUri = new URI("ws://" + ip + ":" + port);

            wsClient = new WebSocketClient(lastUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    isConnected = true;
                    try {
                        User user = SessionService.getUtilisateurConnecte();
                        if (user != null) {
                            send("REGISTER:" + user.getId());
                        }
                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void onMessage(String message) {
                    if (message != null && message.startsWith("UPDATE_TICKET:")) {
                        traiterMessageUpdate(message);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                }

                @Override
                public void onError(Exception ex) {
                    isConnected = false;
                }
            };
            wsClient.connect();

        } catch (Exception e) {
            isConnected = false;
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
            String[] messageParts = message.split(":");
            if (messageParts.length < 2) return;

            String payload = messageParts[1];
            String[] parts = payload.split("_");
            if (parts.length == 0) return;

            int idTicket = Integer.parseInt(parts[0]);
            String typeCible = parts.length > 1 ? parts[1] : "AUTEUR";
            int idSender = parts.length > 2 ? Integer.parseInt(parts[2]) : -1;

            User currentUser = SessionService.getUtilisateurConnecte();
            if (currentUser != null && currentUser.getId() == idSender) {
                return;
            }

            Platform.runLater(() -> {
                try {
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
                            int nombre = "TECH".equals(typeCible) ? HomeController.notificationsTech : HomeController.notificationsAuteur;
                            MenuController.getInstance().allumerBadge(typeCible, nombre);
                        }

                        HomeController.ajouterNotification(typeCible);

                        if ("AUTEUR".equals(typeCible)) {
                            SystemNotificationService.send("SteeveJobs - Support", "Nouvelle réponse sur le ticket #" + idTicket);
                        } else if ("TECH".equals(typeCible)) {
                            SystemNotificationService.send("SteeveJobs - Admin", "Nouveau message à traiter ! (#" + idTicket + ")");
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
        } catch (Exception ignored) {
        }
    }

    public void deconnecter(Runnable onClosed) {
        try {
            scheduler.shutdownNow();
            if (wsClient != null && wsClient.isOpen()) {
                wsClient.closeBlocking();
            }
        } catch (Exception ignored) {
        }

        if (onClosed != null) {
            onClosed.run();
        }
    }

    public void envoyerNotification(int idAuteurCible, int idTicket, String typeCible) {
        try {
            if (wsClient != null && wsClient.isOpen()) {
                User user = SessionService.getUtilisateurConnecte();
                int myId = user != null ? user.getId() : -1;
                String msg = "NOTIFY:" + idAuteurCible + ":" + idTicket + "_" + typeCible + "_" + myId;
                wsClient.send(msg);
            }
        } catch (Exception ignored) {
        }
    }

    public void envoyerNotificationGroupée(List<Integer> idsCibles, int idTicket, String typeCible) {
        try {
            if (wsClient != null && wsClient.isOpen() && idsCibles != null && !idsCibles.isEmpty()) {
                User user = SessionService.getUtilisateurConnecte();
                int myId = user != null ? user.getId() : -1;

                String ids = idsCibles.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));

                String msg = "NOTIFY:" + ids + ":" + idTicket + "_" + typeCible + "_" + myId;
                wsClient.send(msg);
            }
        } catch (Exception ignored) {
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
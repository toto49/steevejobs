package com.eseo.steevejobs.service;

import com.eseo.steevejobs.controller.*;
import com.eseo.steevejobs.model.User;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebSocketService {

    private static volatile WebSocketService instance;
    private WebSocketClient wsClient;

    private static volatile Dotenv dotenvInstance;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);

    private final Set<Integer> ticketsNonLus = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean serviceActive = new AtomicBoolean(true);

    private URI lastUri;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "WS-Client-Scheduler");
        t.setDaemon(true);
        return t;
    });
    private final Set<String> pendingTypesToUpdate = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean debounceScheduled = new AtomicBoolean(false);
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    private WebSocketService() {
        scheduler.scheduleAtFixedRate(this::checkConnection, 2, 5, TimeUnit.SECONDS);
    }

    public static WebSocketService getInstance() {
        if (instance == null) {
            synchronized (WebSocketService.class) {
                if (instance == null) instance = new WebSocketService();
            }
        }
        return instance;
    }

    private Dotenv getDotenv() {
        if (dotenvInstance == null) {
            synchronized (WebSocketService.class) {
                try {
                    dotenvInstance = Dotenv.load();
                } catch (Exception e) {
                    System.err.println("❌ ERREUR CRITIQUE : Impossible de charger le fichier .env du client JavaFX !");
                    throw e;
                }
            }
        }
        return dotenvInstance;
    }

    public void envoyerMessageBrut(String message) {
        try {
            if (wsClient != null && wsClient.isOpen() && isConnected.get()) {
                wsClient.send(message);
            } else {
                System.err.println("⚠️ Impossible d'envoyer le message : Le WebSocket n'est pas connecté au serveur.");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur d'envoi de message brut : " + e.getMessage());
        }
    }

    public void connecter() {
        if (isConnected.get() || !isConnecting.compareAndSet(false, true)) return;

        try {
            serviceActive.set(true);
            String ip = getDotenv().get("WS_SERVER_IP");
            String port = getDotenv().get("WS_SERVER_PORT");

            if (ip == null || port == null) {
                System.err.println("❌ ERREUR WS : WS_SERVER_IP ou WS_SERVER_PORT manquant dans le .env !");
                isConnecting.set(false);
                return;
            }

            lastUri = new URI("ws://" + ip + ":" + port);

            wsClient = new WebSocketClient(lastUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    isConnected.set(true);
                    isConnecting.set(false);

                    try {
                        String tokenJWT = SessionService.getTokenJWT();

                        if (tokenJWT != null) {
                            JSONObject reg = new JSONObject();
                            reg.put("type", "REGISTER");
                            reg.put("token", tokenJWT);
                            send(reg.toString());
                        } else {
                            System.err.println("❌ ERREUR : Aucun Token JWT trouvé dans la SessionService !");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ WS_ERR (Register): " + e.getMessage());
                    }
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject json = new JSONObject(message);
                        String type = json.optString("type");
                        if ("UPDATE_TICKET".equals(type)) {
                            traiterMessageUpdate(json);
                        } else if ("VISIO_TOKEN_RESPONSE".equals(type)) {
                            String status = json.optString("status", "SUCCESS");

                            if ("SUCCESS".equals(status)) {
                                String token = json.getString("token");
                                String roomName = json.optString("roomName", "");
                                if (VisioController.getActiveInstance() != null) {
                                    VisioController.getActiveInstance().recevoirTokenEtLancer(token, roomName);
                                }
                            } else {
                                String messageErreur = json.optString("message", "❌ Accès au salon refusé.");
                                System.err.println("🛑 [WS] Accès Visio refusé par le serveur : " + messageErreur);
                                if (VisioController.getActiveInstance() != null) {
                                    VisioController.getActiveInstance().recevoirErreurVisio(messageErreur);
                                }
                            }
                        } else if ("PLANIF_RESPONSE".equals(type)) {
                            String status = json.optString("status");
                            System.out.println("📊 [WS] Retour planification reçu : " + status);

                            if (VisioController.getActiveInstance() != null) {
                                if ("SUCCESS".equals(status)) {
                                    VisioController.getActiveInstance().recevoirErreurVisio("✅ Réunion planifiée avec succès !");
                                    VisioController.getActiveInstance().rafraichirListeReunions(); // Recharge le tableau
                                } else {
                                    String messageErreur = json.optString("message",
                                            "❌ Échec de la planification en BDD.");
                                    VisioController.getActiveInstance().recevoirErreurVisio(messageErreur);
                                }
                            }
                        } else if ("MY_VISIOS_RESPONSE".equals(type)) {
                            JSONArray reunions = json.optJSONArray("reunions");

                            if (VisioController.getActiveInstance() != null && reunions != null) {
                                VisioController.getActiveInstance().recevoirListeReunions(reunions);
                            }
                        } else if ("DELETE_VISIO_RESPONSE".equals(type)) {
                            String status = json.optString("status");
                            message = json.optString("message", "Suppression impossible.");
                            if (VisioController.getActiveInstance() != null) {
                                if ("SUCCESS".equals(status)) {
                                    VisioController.getActiveInstance().recevoirSuppressionSalon("✅ " + message);
                                } else {
                                    VisioController.getActiveInstance().recevoirSuppressionSalon("❌ " + message);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("❌ WS_ERR (Parse Msg): " + e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("🛑 WS Déconnecté. Raison : " + reason);
                    isConnected.set(false);
                    isConnecting.set(false);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("❌ WS_ERR (Erreur de réseau): Le client n'arrive pas à atteindre l'IP");
                    isConnected.set(false);
                    isConnecting.set(false);
                }
            };
            wsClient.connect();

        } catch (Exception e) {
            System.err.println("❌ WS_ERR (Catch Connect): " + e.getMessage());
            isConnected.set(false);
            isConnecting.set(false);
        }
    }

    private void checkConnection() {
        if (!serviceActive.get()) return;
        try {
            boolean open = (wsClient != null && wsClient.isOpen());
            if (!open && lastUri != null) connecter();
        } catch (Exception ignored) {
        }
    }

    private void traiterMessageUpdate(JSONObject messageJson) {
        try {
            String eventId = messageJson.optString("eventId");
            if (eventId != null && !eventId.isEmpty() && !processedEvents.add(eventId)) return;
            if (processedEvents.size() > 500) processedEvents.clear();

            JSONObject payload = messageJson.getJSONObject("payload");
            int idTicket = payload.getInt("ticketId");
            String typeCible = payload.optString("targetType", "AUTEUR");
            int idSender = payload.optInt("senderId", -1);

            User currentUser = SessionService.getUtilisateurConnecte();
            if (currentUser != null && currentUser.getId() == idSender) return;

            ajouterTicketNonLu(idTicket);
            pendingTypesToUpdate.add(typeCible);

            if (debounceScheduled.compareAndSet(false, true)) {
                Platform.runLater(() -> {
                    PauseTransition pause = new PauseTransition(Duration.millis(300));
                    pause.setOnFinished(e -> appliquerMisesAJourUI(idTicket));
                    pause.play();
                });
            }
        } catch (Exception e) {
            System.err.println("❌ WS_ERR (Process JSON): " + e.getMessage());
        }
    }

    private void appliquerMisesAJourUI(int dernierIdTicket) {
        debounceScheduled.set(false);
        try {
            TicketController chatActif = TicketController.getActiveInstance();
            if (chatActif != null && chatActif.getCurrentTicketId() == dernierIdTicket) {
                chatActif.refreshChatSilently();
            } else {
                TicketsListController listeActive = TicketsListController.getActiveInstance();
                if (listeActive != null) listeActive.rafraichirAffichage();
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(ParametresController.class);
                boolean pushEnabled = prefs.getBoolean("push_enabled", false);

                for (String typeCible : pendingTypesToUpdate) {
                    HomeController.ajouterNotification(typeCible);

                    if (MenuController.getInstance() != null) {
                        int nombre = "TECH".equals(typeCible) ? HomeController.notificationsTech : HomeController.notificationsAuteur;
                        MenuController.getInstance().allumerBadge(typeCible, nombre);
                    }

                    if (pushEnabled) {
                        if ("AUTEUR".equals(typeCible)) {
                            SystemNotificationService.send("SteeveJobs - Support", "Nouvelle réponse reçue");
                        } else if ("TECH".equals(typeCible)) {
                            SystemNotificationService.send("SteeveJobs - Admin", "Nouveau message à traiter !");
                        }
                    }
                }
                pendingTypesToUpdate.clear();
            }
        } catch (Exception ignored) {
        }
    }

    public void deconnecter(Runnable onClosed) {
        try {
            serviceActive.set(false);
            if (wsClient != null && wsClient.isOpen()) wsClient.closeBlocking();
        } catch (Exception ignored) {
        }
        if (onClosed != null) onClosed.run();
    }

    public void envoyerNotificationGroupée(List<Integer> idsCibles, int idTicket, String typeCible) {
        try {
            if (isConnected.get() && wsClient != null && wsClient.isOpen() && idsCibles != null && !idsCibles.isEmpty()) {
                User user = SessionService.getUtilisateurConnecte();
                int myId = user != null ? user.getId() : -1;

                JSONObject notifyMsg = new JSONObject();
                notifyMsg.put("type", "NOTIFY");

                JSONArray targets = new JSONArray(idsCibles);
                notifyMsg.put("targets", targets);

                JSONObject payload = new JSONObject();
                payload.put("ticketId", idTicket);
                payload.put("targetType", typeCible);
                payload.put("senderId", myId);

                notifyMsg.put("payload", payload);
                notifyMsg.put("eventId", UUID.randomUUID().toString());

                wsClient.send(notifyMsg.toString());
            }
        } catch (Exception ignored) {
        }
    }

    public void envoyerNotification(int idAuteurCible, int idTicket, String typeCible) {
        envoyerNotificationGroupée(List.of(idAuteurCible), idTicket, typeCible);
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
package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.util.TestRuntime;
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

/**
 * Client WebSocket singleton pour le client JavaFX (tickets, visio, notifications).
 * <p>
 * Effets de bord : connexion réseau persistante vers {@code WS_SERVER_IP}:{@code WS_SERVER_PORT}
 * (fichier .env) ; enregistrement JWT via {@link SessionService} ; envoi de messages
 * {@code NOTIFY} ; rafraîchissement UI via {@link WebSocketUiBridge} (debounce 300 ms) ;
 * notifications bureau optionnelles si préférence activée. Désactivé en mode test
 * ({@link com.eseo.steevejobs.util.TestRuntime}).
 * </p>
 */
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
    private volatile int pendingTicketId = -1;

    private final WebSocketUiBridge uiBridge = WebSocketUiBridge.getInstance();

    private WebSocketService() {
        if (!TestRuntime.isEnabled()) {
            scheduler.scheduleAtFixedRate(this::checkConnection, 2, 5, TimeUnit.SECONDS);
        }
    }

    /**
     * Retourne l'instance singleton du client WebSocket.
     *
     * @return instance partagée
     */
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

    /**
     * Envoie une charge JSON brute sur le socket si la connexion est ouverte.
     *
     * @param message payload texte (JSON attendu par le serveur)
     */
    public void envoyerMessageBrut(String message) {
        if (TestRuntime.isEnabled()) {
            return;
        }
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

    /**
     * Établit la connexion WebSocket et envoie le message {@code REGISTER} avec le JWT de session.
     * <p>
     * Planifie une reconnexion périodique tant que le service reste actif.
     * </p>
     */
    public void connecter() {
        if (TestRuntime.isEnabled()) {
            return;
        }
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
                                uiBridge.dispatchVisioTokenSuccess(token, roomName);
                            } else {
                                String messageErreur = json.optString("message", "❌ Accès au salon refusé.");
                                System.err.println("🛑 [WS] Accès Visio refusé par le serveur : " + messageErreur);
                                uiBridge.dispatchVisioMessage(messageErreur);
                            }
                        } else if ("PLANIF_RESPONSE".equals(type)) {
                            String status = json.optString("status");

                            if ("SUCCESS".equals(status)) {
                                uiBridge.dispatchVisioMessage("✅ Réunion planifiée avec succès !");
                                uiBridge.dispatchRefreshReunionsRequest();
                            } else {
                                String messageErreur = json.optString("message",
                                        "❌ Échec de la planification en BDD.");
                                uiBridge.dispatchVisioMessage(messageErreur);
                            }
                        } else if ("MY_VISIOS_RESPONSE".equals(type)) {
                            JSONArray reunions = json.optJSONArray("reunions");
                            uiBridge.dispatchReunionsList(reunions);
                        } else if ("DELETE_VISIO_RESPONSE".equals(type)) {
                            String status = json.optString("status");
                            String responseMessage = json.optString("message", "Suppression impossible.");
                            if ("SUCCESS".equals(status)) {
                                uiBridge.dispatchSalonDeleted("✅ " + responseMessage);
                            } else {
                                uiBridge.dispatchSalonDeleted("❌ " + responseMessage);
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
        if (TestRuntime.isEnabled() || !serviceActive.get()) return;
        try {
            boolean open = (wsClient != null && wsClient.isOpen());
            if (!open && lastUri != null) connecter();
        } catch (Exception ignored) {
        }
    }

    private void traiterMessageUpdate(JSONObject messageJson) {
        try {
            String eventId = messageJson.optString("eventId");
            if (eventId != null && !eventId.isEmpty() && !processedEvents.add(eventId)) {
                return;
            }
            if (processedEvents.size() > 500) processedEvents.clear();

            JSONObject payload = messageJson.getJSONObject("payload");
            int idTicket = payload.getInt("ticketId");
            String typeCible = payload.optString("targetType", "AUTEUR");
            int idSender = payload.optInt("senderId", -1);
            User currentUser = SessionService.getUtilisateurConnecte();
            if (currentUser != null && currentUser.getId() == idSender) {
                return;
            }

            ajouterTicketNonLu(idTicket);
            pendingTypesToUpdate.add(typeCible);
            pendingTicketId = idTicket;
            planifierMiseAJourUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void planifierMiseAJourUI() {
        if (debounceScheduled.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                PauseTransition pause = new PauseTransition(Duration.millis(300));
                pause.setOnFinished(e -> appliquerMisesAJourUI());
                pause.play();
            });
        }
    }

    private void appliquerMisesAJourUI() {
        debounceScheduled.set(false);
        int ticketId = pendingTicketId;
        try {
            if (uiBridge.tryRefreshChatIfActive(ticketId)) {
                pendingTypesToUpdate.clear();
            } else {
                uiBridge.dispatchRefreshTicketList();

                for (String typeCible : pendingTypesToUpdate) {
                    uiBridge.dispatchTicketNotification(typeCible, isPushEnabled());
                }
                pendingTypesToUpdate.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!pendingTypesToUpdate.isEmpty() || pendingTicketId != ticketId) {
            planifierMiseAJourUI();
        }
    }

    private boolean isPushEnabled() {
        return java.util.prefs.Preferences.userNodeForPackage(
                com.eseo.steevejobs.controller.ParametresController.class
        ).getBoolean("push_enabled", false);
    }

    /**
     * Ferme la connexion WebSocket et désactive les tentatives de reconnexion.
     *
     * @param onClosed callback exécuté après fermeture (peut être {@code null})
     */
    public void deconnecter(Runnable onClosed) {
        try {
            serviceActive.set(false);
            if (wsClient != null && wsClient.isOpen()) wsClient.closeBlocking();
        } catch (Exception ignored) {
        }
        if (onClosed != null) onClosed.run();
    }

    /**
     * Notifie plusieurs utilisateurs d'une mise à jour ticket via message {@code NOTIFY}.
     *
     * @param idsCibles  identifiants des destinataires
     * @param idTicket   identifiant du ticket concerné
     * @param typeCible  cible métier (ex. {@code AUTEUR}, {@code ADMIN})
     */
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

    /**
     * Notifie un seul utilisateur (wrapper sur {@link #envoyerNotificationGroupée}).
     *
     * @param idAuteurCible identifiant du destinataire
     * @param idTicket      identifiant du ticket
     * @param typeCible     type de cible pour l'UI
     */
    public void envoyerNotification(int idAuteurCible, int idTicket, String typeCible) {
        envoyerNotificationGroupée(List.of(idAuteurCible), idTicket, typeCible);
    }

    /**
     * Marque un ticket comme non lu en mémoire locale (badge UI).
     *
     * @param idTicket identifiant du ticket
     */
    public void ajouterTicketNonLu(int idTicket) {
        ticketsNonLus.add(idTicket);
    }

    /**
     * Retire un ticket de l'ensemble des non lus locaux.
     *
     * @param idTicket identifiant du ticket
     */
    public void marquerCommeLu(int idTicket) {
        ticketsNonLus.remove(idTicket);
    }

    /**
     * Indique si un ticket est marqué non lu côté client.
     *
     * @param idTicket identifiant du ticket
     * @return {@code true} si le ticket est dans l'ensemble des non lus
     */
    public boolean isTicketNonLu(int idTicket) {
        return ticketsNonLus.contains(idTicket);
    }
}

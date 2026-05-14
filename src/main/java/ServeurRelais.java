import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.cdimascio.dotenv.Dotenv;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServeurRelais extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(ServeurRelais.class);
    private static final int MAX_MSG_SIZE_BYTES = 8192;
    private static final int MAX_CONN_PER_USER = 5;
    private static final int MAX_MSGS_PER_SEC = 20;
    private final ConcurrentHashMap<String, Set<WebSocket>> connectedUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocket, Integer> rateLimiter = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "WS-Server-Scheduler");
        t.setDaemon(true);
        return t;
    });
    private final JWTVerifier jwtVerifier;

    public ServeurRelais(int port) {
        super(new InetSocketAddress(port));
        this.setConnectionLostTimeout(30);
        String secret = Dotenv.load().get("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("🚨 CRITIQUE : JWT_SECRET manquant dans le .env ! Arrêt du serveur.");
        }
        Algorithm jwtAlgorithm = Algorithm.HMAC256(secret);
        this.jwtVerifier = JWT.require(jwtAlgorithm)
                .withIssuer("steevejobs-api")
                .acceptLeeway(5)
                .build();
        scheduler.scheduleAtFixedRate(rateLimiter::clear, 1, 1, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        ServeurRelais server = new ServeurRelais(8887);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Extinction du serveur (Graceful Shutdown)...");
            try {
                server.scheduler.shutdownNow();
                server.stop(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("Nouvelle connexion entrante : IP={}", conn.getRemoteSocketAddress());

        scheduler.schedule(() -> {
            if (conn.isOpen() && conn.getAttachment() == null) {
                logger.warn("Auth timeout (10s) expiré pour IP={}. Fermeture.", conn.getRemoteSocketAddress());
                conn.close(1008, "Auth timeout");
            }
        }, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String userId = conn.getAttachment();
        if (userId != null) {
            connectedUsers.computeIfPresent(userId, (key, userSockets) -> {
                userSockets.remove(conn);
                logger.info("Déconnexion : UserID={}, Appareils restants={}", userId, userSockets.size());
                return userSockets.isEmpty() ? null : userSockets;
            });
        }
        rateLimiter.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        int sizeInBytes = message.getBytes(StandardCharsets.UTF_8).length;
        if (sizeInBytes > MAX_MSG_SIZE_BYTES) {
            logger.warn("Payload trop lourd ({} bytes). IP={}", sizeInBytes, conn.getRemoteSocketAddress());
            conn.close(1009, "Message Too Big");
            return;
        }
        int msgCount = rateLimiter.merge(conn, 1, Integer::sum);
        if (msgCount > MAX_MSGS_PER_SEC) {
            logger.warn("Rate limit dépassé pour IP={}", conn.getRemoteSocketAddress());
            conn.close(1008, "Rate Limit Exceeded");
            return;
        }

        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type");

            if ("REGISTER".equals(type)) {
                String token = json.optString("token");
                String userId = extraireUserIdDuToken(token);

                if (userId == null) {
                    logger.warn("Échec Auth : Token invalide/expiré depuis IP={}", conn.getRemoteSocketAddress());
                    conn.close(1008, "Unauthorized");
                    return;
                }
                if (conn.getAttachment() != null) {
                    logger.warn("Socket déjà authentifié. Tentative ignorée pour UserID={}", userId);
                    return;
                }

                conn.setAttachment(userId);

                connectedUsers.compute(userId, (k, userSockets) -> {
                    if (userSockets == null) userSockets = ConcurrentHashMap.newKeySet();

                    if (userSockets.size() >= MAX_CONN_PER_USER) {
                        logger.warn("Limite de {} appareils atteinte pour UserID={}", MAX_CONN_PER_USER, userId);
                        conn.close(1008, "Too Many Devices");
                        return userSockets;
                    }
                    userSockets.add(conn);
                    return userSockets;
                });

                logger.info("✅ Auth Réussie : UserID={} est en ligne.", userId);

            } else if ("NOTIFY".equals(type)) {
                String senderId = conn.getAttachment();
                if (senderId == null) {
                    conn.close(1008, "Not Authenticated");
                    return;
                }

                JSONObject payload = json.optJSONObject("payload");
                JSONArray targets = json.optJSONArray("targets");
                if (payload == null || targets == null || targets.length() > 100) {
                    logger.warn("Payload invalide ou broadcast abusif par UserID={}", senderId);
                    return;
                }

                String eventId = json.optString("eventId");
                if (eventId == null || eventId.trim().isEmpty()) {
                    eventId = UUID.randomUUID().toString();
                }

                JSONObject updateMsg = new JSONObject();
                updateMsg.put("type", "UPDATE_TICKET");
                updateMsg.put("eventId", eventId);
                updateMsg.put("payload", payload);
                String messageAEnvoyer = updateMsg.toString();
                for (int i = 0; i < targets.length(); i++) {
                    String targetId = String.valueOf(targets.getInt(i));
                    Set<WebSocket> targetSockets = connectedUsers.get(targetId);

                    if (targetSockets != null) {
                        List<WebSocket> socketsCopy = new ArrayList<>(targetSockets);

                        for (WebSocket targetConn : socketsCopy) {
                            if (!targetConn.isOpen()) {
                                targetSockets.remove(targetConn);
                                continue;
                            }
                            try {
                                targetConn.send(messageAEnvoyer);
                            } catch (Exception ex) {
                                logger.error("Erreur d'envoi à la socket UserID={}. Nettoyage.", targetId);
                                targetSockets.remove(targetConn);
                            }
                        }
                    }
                }
                logger.info("NOTIFY relayé par UserID={} vers {} cibles (EventID: {})", senderId, targets.length(), eventId);
            } else {
                logger.warn("Type de message inconnu reçu : {}", type);
            }

        } catch (Exception e) {
            logger.error("Erreur de parsing JSON : {} | Msg: {}", e.getMessage(), message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex.getMessage() != null && !ex.getMessage().contains("Connection reset by peer")) {
            logger.error("Erreur serveur interne : ", ex);
        }
    }

    @Override
    public void onStart() {
        logger.info("🚀 Serveur WS (Production) démarré sur le port {}", getPort());
    }

    private String extraireUserIdDuToken(String token) {
        if (token == null || token.trim().isEmpty()) return null;

        try {
            DecodedJWT jwt = jwtVerifier.verify(token);
            String userId = jwt.getSubject();
            if (userId == null && !jwt.getClaim("userId").isNull()) {
                userId = jwt.getClaim("userId").asString();
                if (userId == null) userId = String.valueOf(jwt.getClaim("userId").asInt());
            }
            return userId;

        } catch (JWTVerificationException exception) {
            logger.warn("❌ JWT refusé : {}", exception.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("❌ Erreur lecture JWT : ", e);
            return null;
        }
    }
}
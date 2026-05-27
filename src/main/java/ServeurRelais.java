import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.cdimascio.dotenv.Dotenv;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
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
    private final String livekitApiKey;
    private final String livekitApiSecret;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public ServeurRelais(int port) {
        super(new InetSocketAddress(port));
        this.setConnectionLostTimeout(30);

        Dotenv dotenv = Dotenv.load();
        String secret = dotenv.get("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("🚨 CRITIQUE : JWT_SECRET manquant dans le .env ! Arrêt du serveur.");
        }
        this.livekitApiKey = dotenv.get("API_KEY_VISIO");
        this.livekitApiSecret = dotenv.get("API_SECRET_VISIO");

        if (this.livekitApiKey == null || this.livekitApiKey.trim().isEmpty() ||
                this.livekitApiSecret == null || this.livekitApiSecret.trim().isEmpty()) {
            throw new IllegalStateException("🚨 CRITIQUE : API_KEY_VISIO ou API_SECRET_VISIO manquant dans le .env ! Arrêt du serveur.");
        }
        this.dbUrl = dotenv.get("DB_URL");
        this.dbUser = dotenv.get("DB_USER");
        this.dbPassword = dotenv.get("DB_PASSWORD");

        if (this.dbUrl == null || this.dbUser == null || this.dbPassword == null) {
            throw new IllegalStateException("🚨 CRITIQUE : Configuration de la base de données (DB_URL, DB_USER, DB_PASSWORD) manquante dans le .env !");
        }

        Algorithm jwtAlgorithm = Algorithm.HMAC256(secret);
        this.jwtVerifier = JWT.require(jwtAlgorithm)
                .withIssuer("steevejobs-api")
                .acceptLeeway(5)
                .build();

        scheduler.scheduleAtFixedRate(rateLimiter::clear, 1, 1, TimeUnit.SECONDS);
    }

    private Connection getDatabaseConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
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

            } else if ("REQUEST_VISIO_TOKEN".equals(type)) {
                String senderId = conn.getAttachment();
                if (senderId == null) {
                    conn.close(1008, "Not Authenticated");
                    return;
                }
                int intUserId = Integer.parseInt(senderId);

                String roomName = json.optString("roomName", "Reunion_Generale").trim();
                String identity = json.optString("identity", "Employe_" + senderId);

                logger.info("Vérification des droits et gestion d'accès visio pour UserID={} (Room: {})", senderId, roomName);

                try {
                    boolean existeEnBdd = false;
                    int codeAcces = 0;

                    try (Connection connDb = getDatabaseConnection();
                         PreparedStatement stmtCheck = connDb.prepareStatement("SELECT COUNT(*) FROM VISIO WHERE room_name = ? AND statut != 'TERMINE'")) {
                        stmtCheck.setString(1, roomName);
                        try (ResultSet rs = stmtCheck.executeQuery()) {
                            if (rs.next()) {
                                existeEnBdd = rs.getInt(1) > 0;
                            }
                        }
                    }

                    if (!existeEnBdd) {
                        logger.info("✨ Salon inconnu ou actif introuvable. Création automatique d'une session instantanée : {}", roomName);
                        try (Connection connDb = getDatabaseConnection();
                             PreparedStatement stmtInsert = connDb.prepareStatement("INSERT INTO VISIO (room_name, createur_id, statut, heure_debut) VALUES (?, ?, 'EN_COURS', CURRENT_TIMESTAMP)")) {
                            stmtInsert.setString(1, roomName);
                            stmtInsert.setInt(2, intUserId);
                            stmtInsert.executeUpdate();
                        }
                    }
                    String sqlAuth = "SELECT v.statut, v.heure_programmee, v.createur_id, " +
                            "(SELECT COUNT(*) FROM VISIO_INVITATIONS vi WHERE vi.visio_id = v.id AND vi.employe_id = ?) as est_invite " +
                            "FROM VISIO v WHERE v.room_name = ? AND v.statut != 'TERMINE'";

                    try (Connection connDb = getDatabaseConnection();
                         PreparedStatement stmtAuth = connDb.prepareStatement(sqlAuth)) {
                        stmtAuth.setInt(1, intUserId);
                        stmtAuth.setString(2, roomName);

                        try (ResultSet rs = stmtAuth.executeQuery()) {
                            if (rs.next()) {
                                String statut = rs.getString("statut");
                                int createurId = rs.getInt("createur_id");
                                int estInvite = rs.getInt("est_invite");

                                if (intUserId == createurId || estInvite > 0) {
                                    codeAcces = 1;

                                    if ("PROGRAMMEE".equals(statut)) {
                                        Timestamp tsProg = rs.getTimestamp("heure_programmee");
                                        if (tsProg != null) {
                                            LocalDateTime heureProgrammee = tsProg.toLocalDateTime();
                                            if (LocalDateTime.now().isBefore(heureProgrammee.minusMinutes(10))) {
                                                codeAcces = -1;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    JSONObject reponse = new JSONObject();
                    reponse.put("type", "VISIO_TOKEN_RESPONSE");

                    if (codeAcces == 1) {
                        try (Connection connDb = getDatabaseConnection();
                             PreparedStatement stmtOpen = connDb.prepareStatement("UPDATE VISIO SET statut = 'EN_COURS', heure_debut = CURRENT_TIMESTAMP WHERE room_name = ? AND statut = 'PROGRAMMEE'")) {
                            stmtOpen.setString(1, roomName);
                            stmtOpen.executeUpdate();
                        }


                        AccessToken tokenLiveKit = new AccessToken(livekitApiKey, livekitApiSecret);
                        tokenLiveKit.setIdentity(identity);
                        tokenLiveKit.setName(identity);
                        tokenLiveKit.addGrants(new RoomJoin(true), new RoomName(roomName));
                        tokenLiveKit.setTtl(3600);

                        reponse.put("status", "SUCCESS");
                        reponse.put("token", tokenLiveKit.toJwt());
                        reponse.put("roomName", roomName);
                        logger.info("🔒 Token LiveKit généré et signé avec succès pour la room {}", roomName);

                    } else if (codeAcces == -1) {
                        reponse.put("status", "ERROR");
                        reponse.put("message", "⚠️ La réunion n'a pas encore commencé. Revenez quelques minutes avant l'heure prévue.");
                        logger.warn("🛑 Tentative d'accès anticipée bloquée pour la room {}", roomName);
                    } else {
                        reponse.put("status", "ERROR");
                        reponse.put("message", "❌ Accès refusé : Vous ne figurez pas sur la liste des invités de cette réunion.");
                        logger.warn("🚫 Accès refusé pour l'utilisateur {} sur la room {}", intUserId, roomName);
                    }

                    conn.send(reponse.toString());

                } catch (Exception ex) {
                    logger.error("Erreur lors du traitement d'accès ou de jeton LiveKit : ", ex);
                }

            } else if ("PLANIFY_VISIO".equals(type)) {
                String senderId = conn.getAttachment();
                if (senderId == null) {
                    conn.close(1008, "Not Authenticated");
                    return;
                }
                int intUserId = Integer.parseInt(senderId);

                String planifRoom = json.getString("roomName").trim();
                LocalDateTime heureProg = LocalDateTime.parse(json.getString("heureProgrammee"));
                JSONArray invitesJson = json.optJSONArray("invites");

                boolean success = false;
                Connection connDb = null;

                try {
                    connDb = getDatabaseConnection();
                    connDb.setAutoCommit(false);

                    int newVisioId = 0;
                    String sqlVisio = "INSERT INTO VISIO (room_name, createur_id, statut, heure_programmee) VALUES (?, ?, 'PROGRAMMEE', ?)";

                    try (PreparedStatement stmtV = connDb.prepareStatement(sqlVisio, Statement.RETURN_GENERATED_KEYS)) {
                        stmtV.setString(1, planifRoom);
                        stmtV.setInt(2, intUserId);
                        stmtV.setTimestamp(3, Timestamp.valueOf(heureProg));
                        stmtV.executeUpdate();

                        try (ResultSet generatedKeys = stmtV.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                newVisioId = generatedKeys.getInt(1);
                            }
                        }
                    }

                    if (newVisioId > 0 && invitesJson != null && invitesJson.length() > 0) {
                        String sqlInvite = "INSERT INTO VISIO_INVITATIONS (visio_id, employe_id) VALUES (?, ?)";
                        try (PreparedStatement stmtI = connDb.prepareStatement(sqlInvite)) {
                            for (int i = 0; i < invitesJson.length(); i++) {
                                stmtI.setInt(1, newVisioId);
                                stmtI.setInt(2, invitesJson.getInt(i));
                                stmtI.addBatch();
                            }
                            stmtI.executeBatch();
                        }
                    }

                    connDb.commit();
                    success = true;
                    logger.info("📅 Nouvelle réunion planifiée en BDD : ID_Visio={}, Salle={}", newVisioId, planifRoom);

                } catch (Exception ex) {
                    if (connDb != null) {
                        try {
                            connDb.rollback();
                        } catch (SQLException ignored) {
                        }
                    }
                    logger.error("❌ Échec de la transaction de planification : ", ex);
                } finally {
                    if (connDb != null) {
                        try {
                            connDb.close();
                        } catch (SQLException ignored) {
                        }
                    }
                }

                JSONObject reponsePlanif = new JSONObject();
                reponsePlanif.put("type", "PLANIF_RESPONSE");
                reponsePlanif.put("status", success ? "SUCCESS" : "ERROR");
                conn.send(reponsePlanif.toString());

            } else if ("GET_MY_VISIOS".equals(type)) {
                String senderId = conn.getAttachment();
                if (senderId == null) {
                    conn.close(1008, "Not Authenticated");
                    return;
                }
                int intUserId = Integer.parseInt(senderId);

                JSONObject reponseListe = new JSONObject();
                reponseListe.put("type", "MY_VISIOS_RESPONSE");
                JSONArray jArray = new JSONArray();

                String sqlList = "SELECT DISTINCT v.* FROM VISIO v LEFT JOIN VISIO_INVITATIONS vi ON v.id = vi.visio_id " +
                        "WHERE (v.createur_id = ? OR vi.employe_id = ?) AND v.statut != 'TERMINE' ORDER BY v.heure_programmee ASC";

                try (Connection connDb = getDatabaseConnection();
                     PreparedStatement stmt = connDb.prepareStatement(sqlList)) {
                    stmt.setInt(1, intUserId);
                    stmt.setInt(2, intUserId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            JSONObject jVisio = new JSONObject();
                            jVisio.put("id", rs.getInt("id"));
                            jVisio.put("roomName", rs.getString("room_name"));
                            jVisio.put("statut", rs.getString("statut"));

                            Timestamp ts = rs.getTimestamp("heure_programmee");
                            jVisio.put("heureProgrammee", ts != null ? ts.toLocalDateTime().toString() : "");
                            jArray.put(jVisio);
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Erreur lors de la récupération de la liste des visios : ", ex);
                }

                reponseListe.put("reunions", jArray);
                conn.send(reponseListe.toString());

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
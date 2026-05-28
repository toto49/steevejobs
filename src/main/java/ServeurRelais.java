import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.cdimascio.dotenv.Dotenv;
import io.livekit.server.*;
import livekit.LivekitModels;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServeurRelais extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(ServeurRelais.class);
    private static final int MAX_MSG_SIZE_BYTES = 8192;
    private static final int MAX_CONN_PER_USER = 5;
    private static final int MAX_MSGS_PER_SEC = 20;
    private static final int ROOM_EMPTY_CHECK_DELAY_SEC = 3;

    private final ConcurrentHashMap<String, Set<WebSocket>> connectedUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocket, Integer> rateLimiter = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "WS-Server-Scheduler");
        t.setDaemon(true);
        return t;
    });

    private final JWTVerifier jwtVerifier;
    private final Algorithm jwtAlgorithm;
    private final String livekitApiKey;
    private final String livekitApiSecret;
    private final RoomServiceClient roomServiceClient;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public ServeurRelais(int port) {
        super(new InetSocketAddress(port));
        this.setConnectionLostTimeout(30);

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String secret = trimEnv(dotenv.get("JWT_SECRET"));
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("CRITIQUE : JWT_SECRET manquant dans le .env");
        }

        this.livekitApiKey = trimEnv(dotenv.get("API_KEY_VISIO"));
        this.livekitApiSecret = trimEnv(dotenv.get("API_SECRET_VISIO"));
        if (this.livekitApiKey == null || this.livekitApiKey.isEmpty()
                || this.livekitApiSecret == null || this.livekitApiSecret.isEmpty()) {
            throw new IllegalStateException("CRITIQUE : API_KEY_VISIO ou API_SECRET_VISIO manquant dans le .env");
        }

        this.dbUrl = trimEnv(dotenv.get("DB_URL"));
        this.dbUser = trimEnv(dotenv.get("DB_USER"));
        this.dbPassword = trimEnv(dotenv.get("DB_PASSWORD"));
        if (this.dbUrl == null || this.dbUser == null || this.dbPassword == null) {
            throw new IllegalStateException("CRITIQUE : DB_URL, DB_USER ou DB_PASSWORD manquant dans le .env");
        }

        this.jwtAlgorithm = Algorithm.HMAC256(secret);
        this.jwtVerifier = JWT.require(jwtAlgorithm)
                .withIssuer("steevejobs-api")
                .acceptLeeway(5)
                .build();

        String livekitWsUrl = trimEnv(dotenv.get("LIVEKIT_URL"));
        String resolvedLivekitHttpUrl = trimEnv(dotenv.get("LIVEKIT_HTTP_URL"));
        if (resolvedLivekitHttpUrl == null || resolvedLivekitHttpUrl.isEmpty()) {
            resolvedLivekitHttpUrl = livekitWsUrl != null
                    ? livekitWsUrl.replace("wss://", "https://").replace("ws://", "http://")
                    : "https://livekit.atomgame.fr";
        }
        this.roomServiceClient = RoomServiceClient.createClient(
                resolvedLivekitHttpUrl,
                this.livekitApiKey,
                this.livekitApiSecret
        );

        scheduler.scheduleAtFixedRate(rateLimiter::clear, 1, 1, TimeUnit.SECONDS);
    }

    private static String trimEnv(String value) {
        return value == null ? null : value.trim();
    }

    private static String sanitizeLiveKitIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            return "participant";
        }
        return identity.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    public static void main(String[] args) {
        int port = 8887;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        ServeurRelais server = new ServeurRelais(port);
        server.start();

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String httpPortValue = trimEnv(dotenv.get("KICK_HTTP_PORT"));
        int httpPort = httpPortValue != null && !httpPortValue.isEmpty() ? Integer.parseInt(httpPortValue) : 8889;
        server.startVisioHttpServer(httpPort);

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

    private void startVisioHttpServer(int port) {
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/api/visio/kick", this::handleKickHttp);
            httpServer.createContext("/api/visio/end-room", this::handleEndRoomHttp);
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
            logger.info("API visio HTTP demarree sur le port {}", port);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de demarrer l'API visio sur le port " + port, ex);
        }
    }

    private Connection getDatabaseConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("Nouvelle connexion entrante : IP={}", conn.getRemoteSocketAddress());

        scheduler.schedule(() -> {
            if (conn.isOpen() && conn.getAttachment() == null) {
                logger.warn("Auth timeout (10s) expire pour IP={}. Fermeture.", conn.getRemoteSocketAddress());
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
                logger.info("Deconnexion : UserID={}, Appareils restants={}", userId, userSockets.size());
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
            logger.warn("Rate limit depasse pour IP={}", conn.getRemoteSocketAddress());
            conn.close(1008, "Rate Limit Exceeded");
            return;
        }

        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type");

            if ("REGISTER".equals(type)) {
                handleRegister(conn, json);
            } else if ("REQUEST_VISIO_TOKEN".equals(type)) {
                handleRequestVisioToken(conn, json);
            } else if ("PLANIFY_VISIO".equals(type)) {
                handlePlanifyVisio(conn, json);
            } else if ("GET_MY_VISIOS".equals(type)) {
                handleGetMyVisios(conn);
            } else if ("DELETE_VISIO".equals(type)) {
                handleDeleteVisio(conn, json);
            } else if ("KICK_VISIO_PARTICIPANT".equals(type)) {
                handleKickVisioParticipant(conn, json);
            } else if ("NOTIFY".equals(type)) {
                handleNotify(conn, json);
            } else {
                logger.warn("Type de message inconnu recu : {}", type);
            }
        } catch (Exception e) {
            logger.error("Erreur de parsing JSON : {} | Msg: {}", e.getMessage(), message);
        }
    }

    private void handleRegister(WebSocket conn, JSONObject json) {
        String token = json.optString("token");
        String userId = extraireUserIdDuToken(token);

        if (userId == null) {
            logger.warn("Echec Auth : Token invalide/expire depuis IP={}", conn.getRemoteSocketAddress());
            conn.close(1008, "Unauthorized");
            return;
        }
        if (conn.getAttachment() != null) {
            logger.warn("Socket deja authentifiee. Tentative ignoree pour UserID={}", userId);
            return;
        }

        conn.setAttachment(userId);
        connectedUsers.compute(userId, (k, userSockets) -> {
            if (userSockets == null) {
                userSockets = ConcurrentHashMap.newKeySet();
            }

            if (userSockets.size() >= MAX_CONN_PER_USER) {
                logger.warn("Limite de {} appareils atteinte pour UserID={}", MAX_CONN_PER_USER, userId);
                conn.close(1008, "Too Many Devices");
                return userSockets;
            }

            userSockets.add(conn);
            return userSockets;
        });

        logger.info("Auth reussie : UserID={} est en ligne.", userId);
    }

    private void handleRequestVisioToken(WebSocket conn, JSONObject json) {
        String senderId = conn.getAttachment();
        if (senderId == null) {
            conn.close(1008, "Not Authenticated");
            return;
        }

        int intUserId = Integer.parseInt(senderId);
        String roomName = json.optString("roomName", "Reunion_Generale").trim();
        String identity = json.optString("identity", senderId);
        String displayName = json.optString("displayName", "Employe_" + senderId).trim();
        if (displayName.isBlank()) {
            displayName = "Employe_" + senderId;
        }

        logger.info("Verification des droits visio pour UserID={} (Room: {})", senderId, roomName);

        try {
            int codeAcces = 0;
            boolean existeEnBdd = false;
            int roomCreateurId = intUserId;

            try (Connection connDb = getDatabaseConnection();
                 PreparedStatement stmtCheck = connDb.prepareStatement(
                         "SELECT COUNT(*) FROM VISIO WHERE room_name = ? AND statut != 'TERMINE'")) {
                stmtCheck.setString(1, roomName);
                try (ResultSet rs = stmtCheck.executeQuery()) {
                    if (rs.next()) {
                        existeEnBdd = rs.getInt(1) > 0;
                    }
                }
            }

            if (!existeEnBdd) {
                logger.info("Creation automatique d'une session instantanee : {}", roomName);
                try (Connection connDb = getDatabaseConnection();
                     PreparedStatement stmtInsert = connDb.prepareStatement(
                             "INSERT INTO VISIO (room_name, createur_id, statut, heure_debut) VALUES (?, ?, 'EN_COURS', CURRENT_TIMESTAMP)")) {
                    stmtInsert.setString(1, roomName);
                    stmtInsert.setInt(2, intUserId);
                    stmtInsert.executeUpdate();
                }
                codeAcces = 1;
            } else {
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
                            roomCreateurId = createurId;
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
            }

            JSONObject reponse = new JSONObject();
            reponse.put("type", "VISIO_TOKEN_RESPONSE");

            if (codeAcces == 1) {
                try (Connection connDb = getDatabaseConnection();
                     PreparedStatement stmtOpen = connDb.prepareStatement(
                             "UPDATE VISIO SET statut = 'EN_COURS', heure_debut = CURRENT_TIMESTAMP WHERE room_name = ? AND statut = 'PROGRAMMEE'")) {
                    stmtOpen.setString(1, roomName);
                    stmtOpen.executeUpdate();
                }

                String safeIdentity = sanitizeLiveKitIdentity(identity);
                AccessToken tokenLiveKit = new AccessToken(livekitApiKey, livekitApiSecret);
                tokenLiveKit.setIdentity(safeIdentity);
                tokenLiveKit.setName(displayName);
                tokenLiveKit.addGrants(
                        new RoomJoin(true),
                        new RoomName(roomName),
                        new CanPublish(true),
                        new CanSubscribe(true)
                );
                tokenLiveKit.setTtl(3600);

                reponse.put("status", "SUCCESS");
                reponse.put("token", tokenLiveKit.toJwt());
                reponse.put("roomName", roomName);
                reponse.put("createurId", roomCreateurId);
                reponse.put("endToken", genererEndToken(intUserId, roomName));
                if (intUserId == roomCreateurId) {
                    reponse.put("kickToken", genererKickToken(intUserId, roomName, roomCreateurId));
                }
                logger.info("Token LiveKit genere pour {} dans la room {}", safeIdentity, roomName);
            } else if (codeAcces == -1) {
                reponse.put("status", "ERROR");
                reponse.put("message", "Cette reunion n'a pas encore commence. Revenez quelques minutes avant l'heure prevue.");
                logger.warn("Acces anticipe bloque pour la room {}", roomName);
            } else {
                reponse.put("status", "ERROR");
                reponse.put("message", "Acces refuse : vous ne figurez pas sur la liste des invites de cette reunion.");
                logger.warn("Acces refuse pour l'utilisateur {} sur la room {}", intUserId, roomName);
            }

            conn.send(reponse.toString());
        } catch (Exception ex) {
            logger.error("Erreur lors du traitement du token LiveKit : ", ex);
            JSONObject reponse = new JSONObject();
            reponse.put("type", "VISIO_TOKEN_RESPONSE");
            reponse.put("status", "ERROR");
            reponse.put("message", "Erreur serveur lors de la generation du token LiveKit.");
            conn.send(reponse.toString());
        }
    }

    private void handlePlanifyVisio(WebSocket conn, JSONObject json) {
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
            logger.info("Nouvelle reunion planifiee : ID_Visio={}, Salle={}", newVisioId, planifRoom);
        } catch (Exception ex) {
            if (connDb != null) {
                try {
                    connDb.rollback();
                } catch (SQLException ignored) {
                }
            }
            logger.error("Echec de la transaction de planification : ", ex);
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
    }

    private void handleGetMyVisios(WebSocket conn) {
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
                "WHERE (v.createur_id = ? OR vi.employe_id = ?) ORDER BY v.heure_programmee DESC";

        try (Connection connDb = getDatabaseConnection();
             PreparedStatement stmt = connDb.prepareStatement(sqlList)) {
            stmt.setInt(1, intUserId);
            stmt.setInt(2, intUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject jVisio = new JSONObject();
                    jVisio.put("id", rs.getInt("id"));
                    jVisio.put("roomName", rs.getString("room_name"));
                    jVisio.put("createurId", rs.getInt("createur_id"));
                    jVisio.put("statut", rs.getString("statut"));

                    Timestamp ts = rs.getTimestamp("heure_programmee");
                    jVisio.put("heureProgrammee", ts != null ? ts.toLocalDateTime().toString() : "");
                    jArray.put(jVisio);
                }
            }
        } catch (Exception ex) {
            logger.error("Erreur lors de la recuperation de la liste des visios : ", ex);
        }

        reponseListe.put("reunions", jArray);
        conn.send(reponseListe.toString());
    }

    private void handleDeleteVisio(WebSocket conn, JSONObject json) {
        String senderId = conn.getAttachment();
        if (senderId == null) {
            conn.close(1008, "Not Authenticated");
            return;
        }

        int intUserId = Integer.parseInt(senderId);
        String roomName = json.optString("roomName", "").trim();

        JSONObject reponse = new JSONObject();
        reponse.put("type", "DELETE_VISIO_RESPONSE");

        if (roomName.isEmpty()) {
            reponse.put("status", "ERROR");
            reponse.put("message", "Nom de salon manquant.");
            conn.send(reponse.toString());
            return;
        }

        try (Connection connDb = getDatabaseConnection()) {
            int createurId;
            int visioId;

            try (PreparedStatement stmtFind = connDb.prepareStatement(
                    "SELECT id, createur_id FROM VISIO WHERE room_name = ?")) {
                stmtFind.setString(1, roomName);
                try (ResultSet rs = stmtFind.executeQuery()) {
                    if (!rs.next()) {
                        reponse.put("status", "ERROR");
                        reponse.put("message", "Salon introuvable.");
                        conn.send(reponse.toString());
                        return;
                    }
                    visioId = rs.getInt("id");
                    createurId = rs.getInt("createur_id");
                }
            }

            if (createurId != intUserId) {
                reponse.put("status", "ERROR");
                reponse.put("message", "Seul le createur peut supprimer ce salon.");
                conn.send(reponse.toString());
                return;
            }

            try (PreparedStatement stmtInv = connDb.prepareStatement(
                    "DELETE FROM VISIO_INVITATIONS WHERE visio_id = ?")) {
                stmtInv.setInt(1, visioId);
                stmtInv.executeUpdate();
            }

            try (PreparedStatement stmtDelete = connDb.prepareStatement(
                    "UPDATE VISIO SET statut = 'TERMINE', heure_fin = CURRENT_TIMESTAMP WHERE id = ?")) {
                stmtDelete.setInt(1, visioId);
                stmtDelete.executeUpdate();
            }

            reponse.put("status", "SUCCESS");
            reponse.put("message", "Salon supprime avec succes.");
            notifierChangementStatutVisio(roomName);
        } catch (Exception ex) {
            logger.error("Erreur lors de la suppression du salon {} : ", roomName, ex);
            reponse.put("status", "ERROR");
            reponse.put("message", "Erreur lors de la suppression du salon.");
        }

        conn.send(reponse.toString());
    }

    private void handleKickVisioParticipant(WebSocket conn, JSONObject json) {
        JSONObject reponse = new JSONObject();
        reponse.put("type", "KICK_VISIO_RESPONSE");

        try {
            JSONObject result = executerKickParticipant(
                    json.optString("kickToken", ""),
                    json.optString("roomName", "").trim(),
                    json.optString("targetIdentity", "").trim(),
                    conn.getAttachment()
            );
            reponse.put("status", result.getString("status"));
            reponse.put("message", result.getString("message"));
        } catch (Exception ex) {
            logger.error("Erreur lors de l'expulsion visio via WS : ", ex);
            reponse.put("status", "ERROR");
            reponse.put("message", "Impossible d'expulser ce participant.");
        }

        conn.send(reponse.toString());
    }

    private void handleKickHttp(HttpExchange exchange) throws IOException {
        applyVisioCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, errorJson("Methode non autorisee.").toString());
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(body);
            JSONObject result = executerKickParticipant(
                    json.optString("kickToken", ""),
                    json.optString("roomName", "").trim(),
                    json.optString("targetIdentity", "").trim(),
                    null
            );
            int statusCode = "SUCCESS".equals(result.getString("status")) ? 200 : 403;
            sendJsonResponse(exchange, statusCode, result.toString());
        } catch (Exception ex) {
            logger.error("Erreur lors de l'expulsion visio via HTTP : ", ex);
            sendJsonResponse(exchange, 500, errorJson("Impossible d'expulser ce participant.").toString());
        }
    }

    private void handleEndRoomHttp(HttpExchange exchange) throws IOException {
        applyVisioCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, errorJson("Methode non autorisee.").toString());
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(body);
            String roomName = json.optString("roomName", "").trim();
            String endToken = json.optString("endToken", "").trim();

            if (roomName.isBlank() || endToken.isBlank()) {
                sendJsonResponse(exchange, 400, errorJson("Parametres end-room manquants.").toString());
                return;
            }

            verifierEndToken(endToken, roomName);
            planifierVerificationSalleVide(roomName);

            JSONObject result = new JSONObject();
            result.put("status", "SUCCESS");
            result.put("message", "Verification de fermeture planifiee.");
            sendJsonResponse(exchange, 202, result.toString());
        } catch (JWTVerificationException ex) {
            sendJsonResponse(exchange, 403, errorJson("Token end-room invalide.").toString());
        } catch (Exception ex) {
            logger.error("Erreur lors de la demande de fermeture visio via HTTP : ", ex);
            sendJsonResponse(exchange, 500, errorJson("Impossible de fermer le salon.").toString());
        }
    }

    private JSONObject executerKickParticipant(
            String kickToken,
            String roomName,
            String targetIdentity,
            String wsSenderId
    ) throws Exception {
        JSONObject reponse = new JSONObject();

        if (kickToken.isBlank() || roomName.isBlank() || targetIdentity.isBlank()) {
            reponse.put("status", "ERROR");
            reponse.put("message", "Parametres de kick manquants.");
            return reponse;
        }

        DecodedJWT jwt = verifierKickToken(kickToken, roomName);
        String creatorId = jwt.getSubject();
        String safeTargetIdentity = sanitizeLiveKitIdentity(targetIdentity);

        if (safeTargetIdentity.equals(creatorId)) {
            reponse.put("status", "ERROR");
            reponse.put("message", "Vous ne pouvez pas vous expulser vous-meme.");
            return reponse;
        }

        if (wsSenderId != null && !wsSenderId.equals(creatorId)) {
            reponse.put("status", "ERROR");
            reponse.put("message", "Seul le createur peut expulser un participant.");
            return reponse;
        }

        if (!estCreateurActif(roomName, Integer.parseInt(creatorId))) {
            reponse.put("status", "ERROR");
            reponse.put("message", "Seul le createur peut expulser un participant.");
            return reponse;
        }

        roomServiceClient.removeParticipant(roomName, safeTargetIdentity).execute();
        logger.info("Participant {} expulse de la room {} par createur {}", safeTargetIdentity, roomName, creatorId);

        reponse.put("status", "SUCCESS");
        reponse.put("message", "Participant expulse.");
        return reponse;
    }

    private DecodedJWT verifierKickToken(String kickToken, String roomName) throws JWTVerificationException {
        DecodedJWT jwt = jwtVerifier.verify(kickToken);
        if (!"visio_kick".equals(jwt.getClaim("purpose").asString())) {
            throw new JWTVerificationException("Token kick invalide.");
        }
        if (!roomName.equals(jwt.getClaim("room").asString())) {
            throw new JWTVerificationException("Salle invalide pour ce token kick.");
        }
        return jwt;
    }

    private void verifierEndToken(String endToken, String roomName) throws JWTVerificationException {
        DecodedJWT jwt = jwtVerifier.verify(endToken);
        if (!"visio_end".equals(jwt.getClaim("purpose").asString())) {
            throw new JWTVerificationException("Token end-room invalide.");
        }
        if (!roomName.equals(jwt.getClaim("room").asString())) {
            throw new JWTVerificationException("Salle invalide pour ce token end-room.");
        }
    }

    private void planifierVerificationSalleVide(String roomName) {
        scheduler.schedule(
                () -> verifierEtTerminerSalonSiVide(roomName),
                ROOM_EMPTY_CHECK_DELAY_SEC,
                TimeUnit.SECONDS
        );
    }

    private void verifierEtTerminerSalonSiVide(String roomName) {
        try {
            Response<List<LivekitModels.ParticipantInfo>> response = roomServiceClient.listParticipants(roomName).execute();

            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    if (terminerSalonEnBdd(roomName)) {
                        notifierChangementStatutVisio(roomName);
                    }
                } else {
                    logger.warn("Impossible de lister les participants pour {} : HTTP {}", roomName, response.code());
                }
                return;
            }

            List<LivekitModels.ParticipantInfo> participants = response.body();
            if (participants != null && !participants.isEmpty()) {
                logger.info("Salon {} toujours actif ({} participant(s)).", roomName, participants.size());
                return;
            }

            if (terminerSalonEnBdd(roomName)) {
                try {
                    roomServiceClient.deleteRoom(roomName).execute();
                } catch (Exception ex) {
                    logger.debug("deleteRoom LiveKit ignore pour {} : {}", roomName, ex.getMessage());
                }
                notifierChangementStatutVisio(roomName);
            }
        } catch (Exception ex) {
            logger.error("Erreur lors de la verification de salle vide pour {} : ", roomName, ex);
        }
    }

    private boolean terminerSalonEnBdd(String roomName) throws SQLException {
        try (Connection connDb = getDatabaseConnection();
             PreparedStatement stmt = connDb.prepareStatement(
                     "UPDATE VISIO SET statut = 'TERMINE', heure_fin = CURRENT_TIMESTAMP WHERE room_name = ? AND statut = 'EN_COURS'")) {
            stmt.setString(1, roomName);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.info("Salon {} passe en TERMINE (plus aucun participant).", roomName);
                return true;
            }
            return false;
        }
    }

    private void notifierChangementStatutVisio(String roomName) {
        JSONObject notification = new JSONObject();
        notification.put("type", "VISIO_STATUS_CHANGED");
        notification.put("roomName", roomName);
        notification.put("statut", "TERMINE");

        for (Set<WebSocket> sockets : connectedUsers.values()) {
            for (WebSocket socket : sockets) {
                if (socket.isOpen()) {
                    socket.send(notification.toString());
                }
            }
        }
    }

    private boolean estCreateurActif(String roomName, int creatorId) throws SQLException {
        try (Connection connDb = getDatabaseConnection();
             PreparedStatement stmt = connDb.prepareStatement(
                     "SELECT createur_id FROM VISIO WHERE room_name = ? AND statut != 'TERMINE' ORDER BY id DESC LIMIT 1")) {
            stmt.setString(1, roomName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("createur_id") == creatorId;
            }
        }
    }

    private String genererKickToken(int userId, String roomName, int createurId) {
        return JWT.create()
                .withIssuer("steevejobs-api")
                .withSubject(String.valueOf(userId))
                .withClaim("room", roomName)
                .withClaim("createurId", createurId)
                .withClaim("purpose", "visio_kick")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3_600_000L))
                .sign(jwtAlgorithm);
    }

    private String genererEndToken(int userId, String roomName) {
        return JWT.create()
                .withIssuer("steevejobs-api")
                .withSubject(String.valueOf(userId))
                .withClaim("room", roomName)
                .withClaim("purpose", "visio_end")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3_600_000L))
                .sign(jwtAlgorithm);
    }

    private JSONObject errorJson(String message) {
        JSONObject json = new JSONObject();
        json.put("status", "ERROR");
        json.put("message", message);
        return json;
    }

    private void applyVisioCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        applyVisioCorsHeaders(exchange);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleNotify(WebSocket conn, JSONObject json) {
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
                        logger.error("Erreur d'envoi a la socket UserID={}. Nettoyage.", targetId);
                        targetSockets.remove(targetConn);
                    }
                }
            }
        }

        logger.info("NOTIFY relaye par UserID={} vers {} cibles (EventID: {})", senderId, targets.length(), eventId);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex.getMessage() != null && !ex.getMessage().contains("Connection reset by peer")) {
            logger.error("Erreur serveur interne : ", ex);
        }
    }

    @Override
    public void onStart() {
        logger.info("Serveur WS demarre sur le port {}", getPort());
    }

    private String extraireUserIdDuToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            DecodedJWT jwt = jwtVerifier.verify(token);
            String userId = jwt.getSubject();
            if (userId == null && !jwt.getClaim("userId").isNull()) {
                userId = jwt.getClaim("userId").asString();
                if (userId == null) {
                    userId = String.valueOf(jwt.getClaim("userId").asInt());
                }
            }
            return userId;
        } catch (JWTVerificationException exception) {
            logger.warn("JWT refuse : {}", exception.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Erreur lecture JWT : ", e);
            return null;
        }
    }
}

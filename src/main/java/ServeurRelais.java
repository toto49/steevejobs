
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
import java.time.ZoneId;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ServeurRelais extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(ServeurRelais.class);
    private static final int MAX_MSG_SIZE_BYTES = 8192;
    private static final int MAX_CONN_PER_USER = 5;
    private static final int MAX_MSGS_PER_SEC = 20;
    private static final int ROOM_EMPTY_CHECK_DELAY_SEC = 3;
    private static final Pattern ROOM_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{1,100}$");

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
        if (secret == null || secret.isEmpty())
            throw new IllegalStateException("CRITIQUE : JWT_SECRET manquant dans le .env");

        this.livekitApiKey = firstEnv(dotenv, "API_KEY_VISIO");
        this.livekitApiSecret = firstEnv(dotenv, "API_SECRET_VISIO");
        if (this.livekitApiKey == null || this.livekitApiKey.isEmpty()
                || this.livekitApiSecret == null || this.livekitApiSecret.isEmpty())
            throw new IllegalStateException("CRITIQUE : API_KEY_VISIO ou API_SECRET_VISIO manquant dans le .env");

        this.dbUrl = trimEnv(dotenv.get("DB_URL"));
        this.dbUser = trimEnv(dotenv.get("DB_USER"));
        this.dbPassword = trimEnv(dotenv.get("DB_PASSWORD"));
        if (this.dbUrl == null || this.dbUser == null || this.dbPassword == null)
            throw new IllegalStateException("CRITIQUE : DB_URL, DB_USER ou DB_PASSWORD manquant dans le .env");

        this.jwtAlgorithm = Algorithm.HMAC256(secret);
        this.jwtVerifier = JWT.require(jwtAlgorithm)
                .withIssuer("steevejobs-api")
                .acceptLeeway(5)
                .build();

        String livekitWsUrl = firstEnv(dotenv, "LIVEKIT_URL", "LIVEKIT_SERVER_URL");
        String resolvedLivekitHttpUrl = trimEnv(dotenv.get("LIVEKIT_HTTP_URL"));
        if (resolvedLivekitHttpUrl == null || resolvedLivekitHttpUrl.isEmpty()) {
            resolvedLivekitHttpUrl = livekitWsUrl != null
                    ? livekitWsUrl.replace("wss://", "https://").replace("ws://", "http://")
                    : "https://livekit.atomgame.fr";
        }
        this.roomServiceClient = RoomServiceClient.createClient(
                resolvedLivekitHttpUrl, this.livekitApiKey, this.livekitApiSecret);

        scheduler.scheduleAtFixedRate(rateLimiter::clear, 1, 1, TimeUnit.SECONDS);
    }


    private static String trimEnv(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String firstEnv(Dotenv dotenv, String... keys) {
        for (String key : keys) {
            String value = trimEnv(dotenv.get(key));
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static String sanitizeLiveKitIdentity(String identity) {
        if (identity == null || identity.isBlank()) return "participant";
        return identity.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private static boolean isValidRoomName(String roomName) {
        return roomName != null && ROOM_NAME_PATTERN.matcher(roomName).matches();
    }

    public static void main(String[] args) {
        int port = 8887;
        if (args.length > 0) port = Integer.parseInt(args[0]);

        ServeurRelais server = new ServeurRelais(port);
        server.start();

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String httpPortValue = trimEnv(dotenv.get("KICK_HTTP_PORT"));
        int httpPort = (httpPortValue != null && !httpPortValue.isEmpty())
                ? Integer.parseInt(httpPortValue) : 8889;
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
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
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
            connectedUsers.computeIfPresent(userId, (key, sockets) -> {
                sockets.remove(conn);
                logger.info("Deconnexion : UserID={}, Appareils restants={}", userId, sockets.size());
                return sockets.isEmpty() ? null : sockets;
            });
        }
        rateLimiter.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.getBytes(StandardCharsets.UTF_8).length > MAX_MSG_SIZE_BYTES) {
            logger.warn("Payload trop lourd. IP={}", conn.getRemoteSocketAddress());
            conn.close(1009, "Message Too Big");
            return;
        }
        if (rateLimiter.merge(conn, 1, Integer::sum) > MAX_MSGS_PER_SEC) {
            logger.warn("Rate limit depasse pour IP={}", conn.getRemoteSocketAddress());
            conn.close(1008, "Rate Limit Exceeded");
            return;
        }

        try {
            JSONObject json = new JSONObject(message);
            switch (json.optString("type")) {
                case "REGISTER" -> handleRegister(conn, json);
                case "REQUEST_VISIO_TOKEN" -> handleRequestVisioToken(conn, json);
                case "PLANIFY_VISIO" -> handlePlanifyVisio(conn, json);
                case "GET_MY_VISIOS" -> handleGetMyVisios(conn);
                case "DELETE_VISIO" -> handleDeleteVisio(conn, json);
                case "KICK_VISIO_PARTICIPANT" -> handleKickVisioParticipant(conn, json);
                case "NOTIFY" -> handleNotify(conn, json);
                default -> logger.warn("Type de message inconnu : {}", json.optString("type"));
            }
        } catch (Exception e) {
            logger.error("Erreur de parsing JSON : {} | Msg: {}", e.getMessage(), message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex.getMessage() != null && !ex.getMessage().contains("Connection reset by peer"))
            logger.error("Erreur serveur interne : ", ex);
    }

    @Override
    public void onStart() {
        logger.info("Serveur WS demarre sur le port {}", getPort());
    }


    private void handleRegister(WebSocket conn, JSONObject json) {
        String userId = extraireUserIdDuToken(json.optString("token"));
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
        connectedUsers.compute(userId, (k, sockets) -> {
            if (sockets == null) sockets = ConcurrentHashMap.newKeySet();
            if (sockets.size() >= MAX_CONN_PER_USER) {
                logger.warn("Limite de {} appareils atteinte pour UserID={}", MAX_CONN_PER_USER, userId);
                conn.close(1008, "Too Many Devices");
                return sockets;
            }
            sockets.add(conn);
            return sockets;
        });
        logger.info("Auth reussie : UserID={} est en ligne.", userId);
    }


    private static LocalDateTime timestampVersLocal(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static Timestamp localVersTimestamp(LocalDateTime dateHeure) {
        return Timestamp.from(dateHeure.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static String messageErreurSql(Exception ex) {
        if (ex instanceof SQLException sqlEx && sqlEx.getMessage() != null) {
            String msg = sqlEx.getMessage();
            if (msg.contains("Duplicate") || msg.contains("duplicate")) {
                return "Un salon avec ce nom existe deja.";
            }
            return msg;
        }
        return ex.getMessage() != null ? ex.getMessage() : "Erreur base de donnees.";
    }

    private void handleDeleteVisio(WebSocket conn, JSONObject json) {
        String senderId = conn.getAttachment();
        if (senderId == null) {
            conn.close(1008, "Not Authenticated");
            return;
        }

        int intUserId = Integer.parseInt(senderId);
        String roomName = json.optString("roomName", "").trim();

        if (!isValidRoomName(roomName)) {
            sendError(conn, "DELETE_VISIO_RESPONSE", "Nom de salon invalide.");
            return;
        }

        JSONObject reponse = new JSONObject().put("type", "DELETE_VISIO_RESPONSE");
        try (Connection db = getDatabaseConnection()) {
            int visioId;
            int createurId;
            String typeReunion;
            Timestamp heureProgrammee;

            try (PreparedStatement stmt = db.prepareStatement(
                    "SELECT id, createur_id, type_reunion, heure_programmee FROM VISIO WHERE room_name = ?")) {
                stmt.setString(1, roomName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.send(reponse.put("status", "ERROR").put("message", "Salon introuvable.").toString());
                        return;
                    }
                    visioId    = rs.getInt("id");
                    createurId = rs.getInt("createur_id");
                    typeReunion = rs.getString("type_reunion");
                    heureProgrammee = rs.getTimestamp("heure_programmee");
                }
            }

            if (createurId != intUserId) {
                conn.send(reponse.put("status", "ERROR").put("message", "Seul le createur peut supprimer ce salon.").toString());
                return;
            }

            if (estSalonInstantane(typeReunion, heureProgrammee)) {
                supprimerSalonVisioParId(db, visioId);
                logger.info("Salon instantane {} supprime par le createur.", roomName);
            } else {
                try (PreparedStatement stmt = db.prepareStatement(
                        "UPDATE VISIO SET statut = 'TERMINE', heure_fin = CURRENT_TIMESTAMP WHERE id = ?")) {
                    stmt.setInt(1, visioId);
                    stmt.executeUpdate();
                }
            }

            notifierChangementStatutVisio(roomName);
            conn.send(reponse.put("status", "SUCCESS").put("message", "Salon supprime avec succes.").toString());
        } catch (Exception ex) {
            logger.error("Erreur lors de la suppression du salon {} : ", roomName, ex);
            conn.send(reponse.put("status", "ERROR").put("message", "Erreur lors de la suppression du salon.").toString());
        }
    }

    private void handleKickVisioParticipant(WebSocket conn, JSONObject json) {
        JSONObject reponse = new JSONObject().put("type", "KICK_VISIO_RESPONSE");
        try {
            JSONObject result = executerKickParticipant(
                    json.optString("kickToken", ""),
                    json.optString("roomName", "").trim(),
                    json.optString("targetIdentity", "").trim(),
                    conn.getAttachment());
            reponse.put("status", result.getString("status"))
                    .put("message", result.getString("message"));
        } catch (Exception ex) {
            logger.error("Erreur kick WS : ", ex);
            reponse.put("status", "ERROR").put("message", "Impossible d'expulser ce participant.");
        }
        conn.send(reponse.toString());
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
        if (eventId == null || eventId.trim().isEmpty()) eventId = UUID.randomUUID().toString();

        String msg = new JSONObject()
                .put("type", "UPDATE_TICKET")
                .put("eventId", eventId)
                .put("payload", payload)
                .toString();

        for (int i = 0; i < targets.length(); i++) {
            String targetId = String.valueOf(targets.getInt(i));
            Set<WebSocket> sockets = connectedUsers.get(targetId);
            if (sockets == null) continue;

            for (WebSocket s : new ArrayList<>(sockets)) {
                if (!s.isOpen()) {
                    sockets.remove(s);
                    continue;
                }
                try {
                    s.send(msg);
                } catch (Exception ex) {
                    logger.error("Erreur d'envoi a UserID={}. Nettoyage.", targetId);
                    sockets.remove(s);
                }
            }
        }
        logger.info("NOTIFY par UserID={} vers {} cibles (EventID={})", senderId, targets.length(), eventId);
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
            JSONObject json = new JSONObject(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            JSONObject result = executerKickParticipant(
                    json.optString("kickToken", ""),
                    json.optString("roomName", "").trim(),
                    json.optString("targetIdentity", "").trim(),
                    null);
            sendJsonResponse(exchange,
                    "SUCCESS".equals(result.getString("status")) ? 200 : 403,
                    result.toString());
        } catch (Exception ex) {
            logger.error("Erreur kick HTTP : ", ex);
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
            JSONObject json = new JSONObject(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String roomName = json.optString("roomName", "").trim();
            String endToken = json.optString("endToken", "").trim();

            if (roomName.isBlank() || endToken.isBlank()) {
                sendJsonResponse(exchange, 400, errorJson("Parametres end-room manquants.").toString());
                return;
            }
            verifierEndToken(endToken, roomName);
            planifierVerificationSalleVide(roomName);
            sendJsonResponse(exchange, 202,
                    new JSONObject().put("status", "SUCCESS")
                            .put("message", "Verification de fermeture planifiee.").toString());
        } catch (JWTVerificationException ex) {
            sendJsonResponse(exchange, 403, errorJson("Token end-room invalide.").toString());
        } catch (Exception ex) {
            logger.error("Erreur end-room HTTP : ", ex);
            sendJsonResponse(exchange, 500, errorJson("Impossible de fermer le salon.").toString());
        }
    }

    private JSONObject executerKickParticipant(
            String kickToken, String roomName, String targetIdentity, String wsSenderId) throws Exception {

        if (kickToken.isBlank() || roomName.isBlank() || targetIdentity.isBlank())
            return errorJson("Parametres de kick manquants.");

        DecodedJWT jwt = verifierKickToken(kickToken, roomName);
        String creatorId = jwt.getSubject();
        String safeTarget = sanitizeLiveKitIdentity(targetIdentity);

        if (safeTarget.equals(creatorId))
            return errorJson("Vous ne pouvez pas vous expulser vous-meme.");
        if (wsSenderId != null && !wsSenderId.equals(creatorId))
            return errorJson("Seul le createur peut expulser un participant.");
        if (!estCreateurActif(roomName, Integer.parseInt(creatorId)))
            return errorJson("Seul le createur peut expulser un participant.");

        roomServiceClient.removeParticipant(roomName, safeTarget).execute();
        logger.info("Participant {} expulse de {} par {}", safeTarget, roomName, creatorId);

        return new JSONObject().put("status", "SUCCESS").put("message", "Participant expulse.");
    }

    private DecodedJWT verifierKickToken(String kickToken, String roomName) throws JWTVerificationException {
        DecodedJWT jwt = jwtVerifier.verify(kickToken);
        if (!"visio_kick".equals(jwt.getClaim("purpose").asString()))
            throw new JWTVerificationException("Token kick invalide.");
        if (!roomName.equals(jwt.getClaim("room").asString()))
            throw new JWTVerificationException("Salle invalide pour ce token kick.");
        return jwt;
    }

    private void verifierEndToken(String endToken, String roomName) throws JWTVerificationException {
        DecodedJWT jwt = jwtVerifier.verify(endToken);
        if (!"visio_end".equals(jwt.getClaim("purpose").asString()))
            throw new JWTVerificationException("Token end-room invalide.");
        if (!roomName.equals(jwt.getClaim("room").asString()))
            throw new JWTVerificationException("Salle invalide pour ce token end-room.");
    }

    private void planifierVerificationSalleVide(String roomName) {
        scheduler.schedule(() -> verifierEtTerminerSalonSiVide(roomName),
                ROOM_EMPTY_CHECK_DELAY_SEC, TimeUnit.SECONDS);
    }

    private void verifierEtTerminerSalonSiVide(String roomName) {
        try {
            Response<List<LivekitModels.ParticipantInfo>> response =
                    roomServiceClient.listParticipants(roomName).execute();

            if (!response.isSuccessful()) {
                if (response.code() == 404 && cloturerSalonEnBdd(roomName))
                    notifierChangementStatutVisio(roomName);
                else
                    logger.warn("Impossible de lister les participants de {} : HTTP {}", roomName, response.code());
                return;
            }

            List<LivekitModels.ParticipantInfo> participants = response.body();
            if (participants != null && !participants.isEmpty()) {
                logger.info("Salon {} toujours actif ({} participant(s)).", roomName, participants.size());
                return;
            }

            if (cloturerSalonEnBdd(roomName)) {
                try { roomServiceClient.deleteRoom(roomName).execute();
                } catch (Exception ex) {
                    logger.debug("deleteRoom ignore pour {} : {}", roomName, ex.getMessage());
                }
                notifierChangementStatutVisio(roomName);
            }
        } catch (Exception ex) {
            logger.error("Erreur verification salle vide pour {} : ", roomName, ex);
        }
    }

    private boolean estSalonInstantane(String typeReunion, Timestamp heureProgrammee) {
        return "INSTANTANEE".equals(typeReunion)
                || (typeReunion == null && heureProgrammee == null);
    }

    private void handleRequestVisioToken(WebSocket conn, JSONObject json) {
        String senderId = conn.getAttachment();
        if (senderId == null) {
            conn.close(1008, "Not Authenticated");
            return;
        }

        int intUserId = Integer.parseInt(senderId);
        String roomName = json.optString("roomName", "").trim();
        String identity = json.optString("identity", senderId);
        String displayName = json.optString("displayName", "Employe_" + senderId).trim();
        if (displayName.isBlank()) displayName = "Employe_" + senderId;

        if (!isValidRoomName(roomName)) {
            sendError(conn, "VISIO_TOKEN_RESPONSE", "Nom de salon invalide.");
            return;
        }

        logger.info("Demande de token visio : UserID={}, Room={}", senderId, roomName);

        JSONObject reponse = new JSONObject().put("type", "VISIO_TOKEN_RESPONSE");
        try {
            String typeReunion = null;
            String statut = null;
            int createurId = intUserId;
            boolean estInvite = false;

            String sqlSelect =
                    "SELECT v.type_reunion, v.statut, v.createur_id, " +
                            "  (SELECT COUNT(*) FROM VISIO_INVITATIONS vi " +
                            "   WHERE vi.visio_id = v.id AND vi.employe_id = ?) AS est_invite " +
                            "FROM VISIO v " +
                            "WHERE v.room_name = ? AND v.statut != 'TERMINE' " +
                            "ORDER BY v.id DESC LIMIT 1";

            try (Connection db = getDatabaseConnection();
                 PreparedStatement stmt = db.prepareStatement(sqlSelect)) {
                stmt.setInt(1, intUserId);
                stmt.setString(2, roomName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        typeReunion = rs.getString("type_reunion");
                        statut = rs.getString("statut");
                        createurId = rs.getInt("createur_id");
                        estInvite = rs.getInt("est_invite") > 0;
                    }
                }
            }
            if (typeReunion == null) {
                logger.info("Creation automatique d'une session instantanee : {}", roomName);
                try (Connection db = getDatabaseConnection()) {
                    purgerReliquatsSalonInstantane(db, roomName);
                    try (PreparedStatement stmt = db.prepareStatement(
                            "INSERT INTO VISIO (room_name, createur_id, type_reunion, statut, heure_debut) " +
                                    "VALUES (?, ?, 'INSTANTANEE', 'EN_COURS', CURRENT_TIMESTAMP)")) {
                        stmt.setString(1, roomName);
                        stmt.setInt(2, intUserId);
                        stmt.executeUpdate();
                    }
                }
                typeReunion = "INSTANTANEE";
                statut = "EN_COURS";
                createurId = intUserId;
            }

            boolean accesAutorise = "INSTANTANEE".equals(typeReunion)
                    || intUserId == createurId
                    || estInvite;

            if (!accesAutorise) {
                reponse.put("status", "ERROR")
                        .put("message", "Acces refuse : vous ne figurez pas sur la liste des invites de cette reunion.");
                logger.warn("Acces refuse pour UserID={} sur la room {}", intUserId, roomName);
                conn.send(reponse.toString());
                return;
            }

            try (Connection db = getDatabaseConnection()) {
                activerReunionsPlanifieesEligibles(db);
                if ("PROGRAMMEE".equals(statut)) {
                    try (PreparedStatement stmt = db.prepareStatement(
                            "UPDATE VISIO SET statut = 'EN_COURS', heure_debut = CURRENT_TIMESTAMP "
                                    + "WHERE room_name = ? AND statut = 'PROGRAMMEE'")) {
                        stmt.setString(1, roomName);
                        stmt.executeUpdate();
                    }
                }
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

            reponse.put("status", "SUCCESS")
                    .put("token", tokenLiveKit.toJwt())
                    .put("roomName", roomName)
                    .put("createurId", createurId)
                    .put("typeReunion", typeReunion)
                    .put("endToken", genererEndToken(intUserId, roomName));

            if (intUserId == createurId) {
                reponse.put("kickToken", genererKickToken(intUserId, roomName, createurId));
            }
            logger.info("Token LiveKit genere pour {} dans la room {} (type={})", safeIdentity, roomName, typeReunion);

        } catch (Exception ex) {
            logger.error("Erreur lors de la generation du token LiveKit : ", ex);
            reponse.put("status", "ERROR")
                    .put("message", "Erreur serveur lors de la generation du token LiveKit : " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }

        conn.send(reponse.toString());
    }

    private void handlePlanifyVisio(WebSocket conn, JSONObject json) {
        String senderId = conn.getAttachment();
        if (senderId == null) {
            conn.close(1008, "Not Authenticated");
            return;
        }

        int intUserId = Integer.parseInt(senderId);
        String planifRoom = json.optString("roomName", "").trim();

        if (!isValidRoomName(planifRoom)) {
            sendError(conn, "PLANIF_RESPONSE", "Nom de salon invalide.");
            return;
        }

        String heureProgStr = json.optString("heureProgrammee", "").trim();
        if (heureProgStr.isEmpty()) {
            sendError(conn, "PLANIF_RESPONSE", "Date et heure obligatoires.");
            return;
        }

        LocalDateTime heureProg;
        try {
            heureProg = LocalDateTime.parse(heureProgStr);
        } catch (Exception ex) {
            sendError(conn, "PLANIF_RESPONSE", "Format de date/heure invalide.");
            return;
        }

        JSONArray invitesJson = json.optJSONArray("invites");
        JSONObject reponse = new JSONObject().put("type", "PLANIF_RESPONSE");

        try (Connection db = getDatabaseConnection()) {
            if (salonActifExiste(db, planifRoom)) {
                reponse.put("status", "ERROR")
                        .put("message", "Un salon actif porte deja ce nom. Choisissez un autre identifiant.");
                conn.send(reponse.toString());
                return;
            }

            db.setAutoCommit(false);
            try {
                int newVisioId = insererReunionPlanifiee(db, planifRoom, intUserId, heureProg);
                if (newVisioId <= 0) {
                    throw new SQLException("Impossible de creer la reunion en base.");
                }

                if (invitesJson != null && invitesJson.length() > 0) {
                    try (PreparedStatement stmtI = db.prepareStatement(
                            "INSERT INTO VISIO_INVITATIONS (visio_id, employe_id) VALUES (?, ?)")) {
                        for (int i = 0; i < invitesJson.length(); i++) {
                            stmtI.setInt(1, newVisioId);
                            stmtI.setInt(2, invitesJson.getInt(i));
                            stmtI.addBatch();
                        }
                        stmtI.executeBatch();
                    }
                }

                db.commit();
                activerReunionsPlanifieesEligibles(db);
                reponse.put("status", "SUCCESS");
                logger.info("Nouvelle reunion planifiee : ID={}, Salle={}, heure={}", newVisioId, planifRoom, heureProg);
            } catch (Exception ex) {
                db.rollback();
                logger.error("Echec de la transaction de planification : ", ex);
                reponse.put("status", "ERROR").put("message", messageErreurSql(ex));
            }
        } catch (Exception ex) {
            logger.error("Erreur de connexion BDD lors de la planification : ", ex);
            reponse.put("status", "ERROR").put("message", messageErreurSql(ex));
        }

        conn.send(reponse.toString());
    }

    private void handleGetMyVisios(WebSocket conn) {
        String senderId = conn.getAttachment();
        if (senderId == null) {
            conn.close(1008, "Not Authenticated");
            return;
        }

        int intUserId = Integer.parseInt(senderId);
        JSONArray jArray = new JSONArray();

        String sql =
                "SELECT DISTINCT v.id, v.room_name, v.createur_id, v.type_reunion, v.statut, v.heure_programmee " +
                        "FROM VISIO v LEFT JOIN VISIO_INVITATIONS vi ON v.id = vi.visio_id " +
                        "WHERE (v.createur_id = ? OR vi.employe_id = ?) AND v.statut != 'TERMINE' " +
                        "ORDER BY v.heure_programmee DESC";

        try (Connection db = getDatabaseConnection()) {
            activerReunionsPlanifieesEligibles(db);
            try (PreparedStatement stmt = db.prepareStatement(sql)) {
                stmt.setInt(1, intUserId);
                stmt.setInt(2, intUserId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("heure_programmee");
                        LocalDateTime hp = ts != null ? timestampVersLocal(ts) : null;
                        jArray.put(new JSONObject()
                                .put("id", rs.getInt("id"))
                                .put("roomName", rs.getString("room_name"))
                                .put("createurId", rs.getInt("createur_id"))
                                .put("typeReunion", rs.getString("type_reunion"))
                                .put("statut", rs.getString("statut"))
                                .put("heureProgrammee", hp != null ? hp.toString() : ""));
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Erreur lors de la recuperation des visios : ", ex);
        }

        conn.send(new JSONObject()
                .put("type", "MY_VISIOS_RESPONSE")
                .put("reunions", jArray)
                .toString());
    }

    private boolean salonActifExiste(Connection db, String roomName) throws SQLException {
        try (PreparedStatement stmt = db.prepareStatement(
                "SELECT COUNT(*) FROM VISIO WHERE room_name = ? AND statut != 'TERMINE'")) {
            stmt.setString(1, roomName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void activerReunionsPlanifieesEligibles(Connection db) throws SQLException {
        LocalDateTime maintenant = LocalDateTime.now(ZoneId.systemDefault());
        try (PreparedStatement stmt = db.prepareStatement(
                "UPDATE VISIO SET statut = 'EN_COURS', heure_debut = CURRENT_TIMESTAMP "
                        + "WHERE statut = 'PROGRAMMEE' AND heure_programmee IS NOT NULL "
                        + "AND heure_programmee <= ?")) {
            stmt.setTimestamp(1, localVersTimestamp(maintenant));
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.info("{} reunion(s) planifiee(s) passee(s) en EN_COURS.", updated);
            }
        }
    }

    private int insererReunionPlanifiee(Connection db, String roomName, int createurId, LocalDateTime heureProg)
            throws SQLException {
        String sqlAvecType = "INSERT INTO VISIO (room_name, createur_id, type_reunion, statut, heure_programmee) "
                + "VALUES (?, ?, 'PLANIFIEE', 'PROGRAMMEE', ?)";
        String sqlSansType = "INSERT INTO VISIO (room_name, createur_id, statut, heure_programmee) "
                + "VALUES (?, ?, 'PROGRAMMEE', ?)";
        try {
            return executerInsertVisio(db, sqlAvecType, roomName, createurId, heureProg);
        } catch (SQLException ex) {
            logger.warn("Insert avec type_reunion echoue, retry sans type : {}", ex.getMessage());
            return executerInsertVisio(db, sqlSansType, roomName, createurId, heureProg);
        }
    }

    private int executerInsertVisio(Connection db, String sql, String roomName, int createurId, LocalDateTime heureProg)
            throws SQLException {
        try (PreparedStatement stmt = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, roomName);
            stmt.setInt(2, createurId);
            stmt.setTimestamp(3, localVersTimestamp(heureProg));
            stmt.executeUpdate();
            try (ResultSet gk = stmt.getGeneratedKeys()) {
                if (gk.next()) {
                    return gk.getInt(1);
                }
            }
        }
        return 0;
    }

    private void supprimerSalonVisioParId(Connection db, int visioId) throws SQLException {
        try (PreparedStatement inv = db.prepareStatement(
                "DELETE FROM VISIO_INVITATIONS WHERE visio_id = ?")) {
            inv.setInt(1, visioId);
            inv.executeUpdate();
        }
        try (PreparedStatement del = db.prepareStatement("DELETE FROM VISIO WHERE id = ?")) {
            del.setInt(1, visioId);
            del.executeUpdate();
        }
    }

    private void purgerReliquatsSalonInstantane(Connection db, String roomName) throws SQLException {
        try (PreparedStatement sel = db.prepareStatement(
                "SELECT id FROM VISIO WHERE room_name = ? " +
                        "AND (type_reunion = 'INSTANTANEE' OR (type_reunion IS NULL AND heure_programmee IS NULL))")) {
            sel.setString(1, roomName);
            try (ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    supprimerSalonVisioParId(db, rs.getInt("id"));
                }
            }
        }
    }

    private boolean cloturerSalonEnBdd(String roomName) throws SQLException {
        try (Connection db = getDatabaseConnection()) {
            int visioId = -1;
            String typeReunion = null;
            Timestamp heureProgrammee = null;

            try (PreparedStatement sel = db.prepareStatement(
                    "SELECT id, type_reunion, heure_programmee FROM VISIO " +
                            "WHERE room_name = ? AND statut = 'EN_COURS' ORDER BY id DESC LIMIT 1")) {
                sel.setString(1, roomName);
                try (ResultSet rs = sel.executeQuery()) {
                    if (!rs.next()) return false;
                    visioId = rs.getInt("id");
                    typeReunion = rs.getString("type_reunion");
                    heureProgrammee = rs.getTimestamp("heure_programmee");
                }
            }

            if (estSalonInstantane(typeReunion, heureProgrammee)) {
                supprimerSalonVisioParId(db, visioId);
                logger.info("Salon instantane {} supprime de la BDD (plus aucun participant).", roomName);
                return true;
            }

            try (PreparedStatement stmt = db.prepareStatement(
                    "UPDATE VISIO SET statut = 'TERMINE', heure_fin = CURRENT_TIMESTAMP WHERE id = ?")) {
                stmt.setInt(1, visioId);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    logger.info("Salon planifie {} passe en TERMINE (plus aucun participant).", roomName);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean terminerSalonEnBdd(String roomName) throws SQLException {
        return cloturerSalonEnBdd(roomName);
    }

    private void notifierChangementStatutVisio(String roomName) {
        String msg = new JSONObject()
                .put("type", "VISIO_STATUS_CHANGED")
                .put("roomName", roomName)
                .put("statut", "TERMINE")
                .toString();

        connectedUsers.values().forEach(sockets ->
                sockets.stream().filter(WebSocket::isOpen).forEach(s -> s.send(msg)));
    }

    private boolean estCreateurActif(String roomName, int creatorId) throws SQLException {
        try (Connection db = getDatabaseConnection();
             PreparedStatement stmt = db.prepareStatement(
                     "SELECT createur_id FROM VISIO WHERE room_name = ? AND statut != 'TERMINE' " +
                             "ORDER BY id DESC LIMIT 1")) {
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
        return new JSONObject().put("status", "ERROR").put("message", message);
    }

    private void sendError(WebSocket conn, String type, String message) {
        conn.send(new JSONObject()
                .put("type", type)
                .put("status", "ERROR")
                .put("message", message)
                .toString());
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
        } catch (JWTVerificationException ex) {
            logger.warn("JWT refuse : {}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            logger.error("Erreur lecture JWT : ", ex);
            return null;
        }
    }
}

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class ServeurRelais extends WebSocketServer {
    private final ConcurrentHashMap<String, WebSocket> connectedUsers = new ConcurrentHashMap<>();

    public ServeurRelais(int port) {
        super(new InetSocketAddress(port));
        this.setConnectionLostTimeout(30);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    public static void main(String[] args) {
        ServeurRelais server = new ServeurRelais(8887);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Extinction du serveur...");
            try {
                server.stop(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String userId = conn.getAttachment();
        if (userId != null) {
            connectedUsers.remove(userId);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            if (message.startsWith("REGISTER:")) {
                String[] parts = message.split(":", 2);
                if (parts.length < 2) return;

                String userId = parts[1].trim();
                conn.setAttachment(userId);
                connectedUsers.put(userId, conn);

                System.out.println("✅ [ONLINE] Utilisateur ID: " + userId);
            } else if (message.startsWith("NOTIFY:")) {
                String[] parts = message.split(":");
                if (parts.length < 3) return;

                String[] targetUsers = parts[1].split(",");
                String payload = parts[2];


                String updateMessage = "UPDATE_TICKET:" + payload;

                for (String userId : targetUsers) {
                    WebSocket targetConn = connectedUsers.get(userId.trim());

                    if (targetConn != null && targetConn.isOpen()) {
                        targetConn.send(updateMessage);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur de parsing du message entrant.");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex.getMessage() != null && !ex.getMessage().contains("Connection reset by peer")) {
            System.err.println("🛑 Erreur serveur : " + ex.getMessage());
        }
    }

    @Override
    public void onStart() {
        System.out.println("🚀 Serveur de routage ultra-rapide démarré sur le port " + getPort());
    }
}
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class ServeurRelais extends WebSocketServer {
    private final ConcurrentHashMap<String, WebSocket> connectedUsers = new ConcurrentHashMap<>();

    public ServeurRelais(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connectedUsers.values().remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        if (message.startsWith("REGISTER:")) {
            String userId = message.split(":")[1];
            connectedUsers.put(userId, conn);
            System.out.println("✅ Utilisateur " + userId + " est en ligne.");
        }

        else if (message.startsWith("NOTIFY:")) {
            String[] parts = message.split(":");
            String[] targetUsers = parts[1].split(",");
            String ticketId = parts[2];

            for (String userId : targetUsers) {
                WebSocket targetConn = connectedUsers.get(userId);

                if (targetConn != null && targetConn.isOpen()) {
                    targetConn.send("UPDATE_TICKET:" + ticketId);
                }
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println(" Serveur de routage prêt sur le port " + getPort());
    }

    public static void main(String[] args) {
        new ServeurRelais(8887).start();
    }
}
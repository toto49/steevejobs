package service;

import com.eseo.steevejobs.service.WebSocketUiBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketUiBridgeTest {

    private WebSocketUiBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = WebSocketUiBridge.getInstance();
        bridge.clearTicketCallbacks();
        bridge.clearVisioCallbacks();
    }

    @Test
    void tryRefreshChatIfActive_avecCallback_retourneTrueEtAppelleLeCallback() {
        AtomicInteger ticketIdRecu = new AtomicInteger(-1);

        bridge.setTicketCallbacks(new WebSocketUiBridge.TicketCallbacks() {
            @Override
            public boolean tryRefreshChatIfActive(int ticketId) {
                ticketIdRecu.set(ticketId);
                return true;
            }

            @Override
            public void onRefreshTicketList() {
            }

            @Override
            public void onTicketNotification(String targetType, boolean pushEnabled) {
            }
        });

        assertTrue(bridge.tryRefreshChatIfActive(7));
        assertEquals(7, ticketIdRecu.get());
    }

    @Test
    void dispatchTicketNotification_appelleLeCallback() {
        AtomicReference<String> typeRecu = new AtomicReference<>();
        AtomicBoolean pushRecu = new AtomicBoolean(false);

        bridge.setTicketCallbacks(new WebSocketUiBridge.TicketCallbacks() {
            @Override
            public boolean tryRefreshChatIfActive(int ticketId) {
                return false;
            }

            @Override
            public void onRefreshTicketList() {
            }

            @Override
            public void onTicketNotification(String targetType, boolean pushEnabled) {
                typeRecu.set(targetType);
                pushRecu.set(pushEnabled);
            }
        });

        bridge.dispatchTicketNotification("TECH", true);

        assertEquals("TECH", typeRecu.get());
        assertTrue(pushRecu.get());
    }
}
